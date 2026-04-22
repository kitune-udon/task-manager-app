# 18章 staging deploy preflight チェック定義

## 目的

- `deploy-backend-stg` 実行前に、Elastic Beanstalk deploy で使う IAM / S3 / EB 設定の不足を検知する
- production 実環境の単純コピーではなく、staging deploy 操作で実際に通る経路を基準に確認する
- preflight で `implicitDeny` や設定欠落が出た場合は、18章の deploy 実行前に停止する

## 実行タイミング

- 18章「staging へ deploy して動作確認する」に着手する直前
- staging deploy role、EB instance profile、artifact bucket policy を変更した直後
- `deploy-backend-stg` を再実行する前

## 前提値

```sh
export AWS_REGION=ap-northeast-1
export ACCOUNT_ID=359429618625
export REPOSITORY=kitune-udon/task-manager-app
export GITHUB_ENVIRONMENT=staging

export EB_APPLICATION_NAME=taskflow-stg-eb-app
export EB_ENVIRONMENT_NAME=Taskflow-stg-eb-app-env
export EB_S3_BUCKET=taskflow-stg-eb-artifacts-359429618625
export EB_MANAGED_BUCKET=elasticbeanstalk-ap-northeast-1-359429618625
export EB_RESOURCE_BUCKET=elasticbeanstalk-env-resources-ap-northeast-1

export DEPLOY_ROLE_NAME=taskflow-stg-github-actions-deploy-role
export DEPLOY_ROLE_ARN=arn:aws:iam::359429618625:role/taskflow-stg-github-actions-deploy-role
export EB_SERVICE_ROLE_NAME=aws-elasticbeanstalk-service-role
export EB_SERVICE_ROLE_ARN=arn:aws:iam::359429618625:role/aws-elasticbeanstalk-service-role
export EB_EC2_ROLE_NAME=taskflow-stg-eb-ec2-role
export EB_EC2_ROLE_ARN=arn:aws:iam::359429618625:role/taskflow-stg-eb-ec2-role

export TEST_VERSION_LABEL=preflight-stg-backend-dummy
export TEST_BUNDLE_KEY=backend/${TEST_VERSION_LABEL}.zip
```

## 判定サマリ

- deploy role の OIDC trust が `repo:kitune-udon/task-manager-app:environment:staging` に限定されていること
- EB 環境が staging 用 application / environment / role / instance profile を参照していること
- deploy role が EB deploy API、artifact bucket、EB managed bucket、CloudFormation、Auto Scaling、EC2 describe、CloudWatch Logs を許可していること
- deploy role が EB resource bucket の patch extension を読めること
- EC2 instance profile role が staging の SSM Parameter を読めること
- custom artifact bucket の `backend/*` を EB deploy 経路で読めるようにしていること
- どれかが `implicitDeny` / `explicitDeny` / 設定なしの場合は deploy しないこと

## 1. 呼び出し元確認

```sh
aws sts get-caller-identity --output table
```

期待結果:

- `Account` が `359429618625`
- 想定している作業者または CI 実行 role であること

## 2. EB 環境設定確認

```sh
aws elasticbeanstalk describe-environments \
  --region "${AWS_REGION}" \
  --application-name "${EB_APPLICATION_NAME}" \
  --environment-names "${EB_ENVIRONMENT_NAME}" \
  --query 'Environments[0].{ApplicationName:ApplicationName,EnvironmentName:EnvironmentName,EnvironmentId:EnvironmentId,Status:Status,Health:Health,HealthStatus:HealthStatus,VersionLabel:VersionLabel,AbortableOperationInProgress:AbortableOperationInProgress}' \
  --output table
```

期待結果:

- `ApplicationName = taskflow-stg-eb-app`
- `EnvironmentName = Taskflow-stg-eb-app-env`
- `Status = Ready`
- `AbortableOperationInProgress = False`
- deploy 前時点で `HealthStatus = Ok` が望ましい

```sh
aws elasticbeanstalk describe-configuration-settings \
  --region "${AWS_REGION}" \
  --application-name "${EB_APPLICATION_NAME}" \
  --environment-name "${EB_ENVIRONMENT_NAME}" \
  --query 'ConfigurationSettings[0].OptionSettings[?Namespace==`aws:elasticbeanstalk:environment` || Namespace==`aws:autoscaling:launchconfiguration`].[Namespace,OptionName,Value]' \
  --output table
```

期待結果:

- `ServiceRole = arn:aws:iam::359429618625:role/aws-elasticbeanstalk-service-role`
- `IamInstanceProfile = taskflow-stg-eb-ec2-role`
- production の `aws-elasticbeanstalk-ec2-role` を参照していないこと

## 3. GitHub Actions deploy role trust 確認

```sh
aws iam get-role \
  --role-name "${DEPLOY_ROLE_NAME}" \
  --query 'Role.AssumeRolePolicyDocument' \
  --output json
```

期待結果:

- `Principal.Federated = arn:aws:iam::359429618625:oidc-provider/token.actions.githubusercontent.com`
- `Action = sts:AssumeRoleWithWebIdentity`
- `token.actions.githubusercontent.com:aud = sts.amazonaws.com`
- `token.actions.githubusercontent.com:sub = repo:kitune-udon/task-manager-app:environment:staging`
- `production` environment を指していないこと

## 4. deploy role: EB / EC2 / CloudFormation / Auto Scaling / Logs

```sh
aws iam simulate-principal-policy \
  --policy-source-arn "${DEPLOY_ROLE_ARN}" \
  --action-names \
    elasticbeanstalk:CreateApplicationVersion \
    elasticbeanstalk:DescribeEnvironments \
    elasticbeanstalk:DescribeEvents \
    elasticbeanstalk:UpdateEnvironment \
    cloudformation:DescribeStacks \
    cloudformation:GetTemplate \
    cloudformation:UpdateStack \
    autoscaling:DescribeAccountLimits \
    autoscaling:DescribeAutoScalingGroups \
    autoscaling:DescribeAutoScalingInstances \
    autoscaling:DescribeLaunchConfigurations \
    autoscaling:DescribeLoadBalancers \
    autoscaling:DescribeNotificationConfigurations \
    autoscaling:DescribeScalingActivities \
    autoscaling:DescribeScheduledActions \
    autoscaling:ResumeProcesses \
    autoscaling:SuspendProcesses \
    ec2:DescribeLaunchTemplates \
    ec2:DescribeLaunchTemplateVersions \
    ec2:DescribeSubnets \
    ec2:DescribeSecurityGroups \
    ec2:DescribeVpcs \
    ec2:DescribeInstances \
    ec2:DescribeImages \
    logs:DescribeLogGroups \
  --query 'EvaluationResults[].{Action:EvalActionName,Decision:EvalDecision}' \
  --output table
```

期待結果:

- すべて `allowed`

補足:

- staging deploy では `ec2:DescribeSubnets` と `ec2:DescribeImages` の不足が実際に発生したため、production 実環境で `implicitDeny` でも staging preflight では必須チェックにする

## 5. deploy role: artifact bucket

```sh
aws iam simulate-principal-policy \
  --policy-source-arn "${DEPLOY_ROLE_ARN}" \
  --action-names s3:ListBucket \
  --resource-arns "arn:aws:s3:::${EB_S3_BUCKET}" \
  --query 'EvaluationResults[].{Action:EvalActionName,Resource:EvalResourceName,Decision:EvalDecision}' \
  --output table
```

```sh
aws iam simulate-principal-policy \
  --policy-source-arn "${DEPLOY_ROLE_ARN}" \
  --action-names s3:PutObject s3:GetObject s3:DeleteObject \
  --resource-arns "arn:aws:s3:::${EB_S3_BUCKET}/${TEST_BUNDLE_KEY}" \
  --query 'EvaluationResults[].{Action:EvalActionName,Resource:EvalResourceName,Decision:EvalDecision}' \
  --output table
```

期待結果:

- すべて `allowed`

## 6. deploy role: EB managed bucket / EB resource bucket

```sh
aws iam simulate-principal-policy \
  --policy-source-arn "${DEPLOY_ROLE_ARN}" \
  --action-names \
    s3:GetBucketLocation \
    s3:GetBucketPolicy \
    s3:ListBucket \
    s3:PutBucketPolicy \
  --resource-arns "arn:aws:s3:::${EB_MANAGED_BUCKET}" \
  --query 'EvaluationResults[].{Action:EvalActionName,Resource:EvalResourceName,Decision:EvalDecision}' \
  --output table
```

```sh
aws iam simulate-principal-policy \
  --policy-source-arn "${DEPLOY_ROLE_ARN}" \
  --action-names s3:GetObject s3:PutObject s3:DeleteObject \
  --resource-arns "arn:aws:s3:::${EB_MANAGED_BUCKET}/resources/environments/preflight-dummy" \
  --query 'EvaluationResults[].{Action:EvalActionName,Resource:EvalResourceName,Decision:EvalDecision}' \
  --output table
```

```sh
aws iam simulate-principal-policy \
  --policy-source-arn "${DEPLOY_ROLE_ARN}" \
  --action-names s3:GetObject \
  --resource-arns "arn:aws:s3:::${EB_RESOURCE_BUCKET}/eb_patching_resources/instance_patch_extension.linux" \
  --query 'EvaluationResults[].{Action:EvalActionName,Resource:EvalResourceName,Decision:EvalDecision}' \
  --output table
```

期待結果:

- すべて `allowed`

補足:

- staging deploy では `elasticbeanstalk-env-resources-ap-northeast-1/eb_patching_resources/instance_patch_extension.linux` の `s3:GetObject` 不足が実際に発生したため、preflight 必須チェックにする

## 7. EC2 instance profile role: SSM Parameter

```sh
aws iam simulate-principal-policy \
  --policy-source-arn "${EB_EC2_ROLE_ARN}" \
  --action-names ssm:GetParameter \
  --resource-arns \
    "arn:aws:ssm:${AWS_REGION}:${ACCOUNT_ID}:parameter/taskflow/stg/backend/DB_URL" \
    "arn:aws:ssm:${AWS_REGION}:${ACCOUNT_ID}:parameter/taskflow/stg/backend/DB_USERNAME" \
    "arn:aws:ssm:${AWS_REGION}:${ACCOUNT_ID}:parameter/taskflow/stg/backend/DB_PASSWORD" \
    "arn:aws:ssm:${AWS_REGION}:${ACCOUNT_ID}:parameter/taskflow/stg/backend/JWT_SECRET" \
    "arn:aws:ssm:${AWS_REGION}:${ACCOUNT_ID}:parameter/taskflow/stg/backend/JWT_EXPIRATION_MILLIS" \
    "arn:aws:ssm:${AWS_REGION}:${ACCOUNT_ID}:parameter/taskflow/stg/backend/CORS_ALLOWED_ORIGINS" \
  --query 'EvaluationResults[].{Action:EvalActionName,Resource:EvalResourceName,Decision:EvalDecision}' \
  --output table
```

期待結果:

- すべて `allowed`

補足:

- production の SSM Parameter を参照できないことも別途確認したい場合は、production parameter ARN で `implicitDeny` になることを見る

## 8. artifact bucket policy

custom artifact bucket を使う場合、EB が source bundle を読む経路を明示する。

確認:

```sh
aws s3api get-bucket-policy \
  --bucket "${EB_S3_BUCKET}" \
  --query Policy \
  --output text
```

期待結果:

- 方針Aまたは方針Bのどちらかを満たす

方針A: bucket policy で EB role に `backend/*` の `s3:GetObject` を許可する

```json
{
  "Sid": "AllowElasticBeanstalkRolesReadBackendBundles",
  "Effect": "Allow",
  "Principal": {
    "AWS": [
      "arn:aws:iam::359429618625:role/aws-elasticbeanstalk-service-role",
      "arn:aws:iam::359429618625:role/taskflow-stg-eb-ec2-role"
    ]
  },
  "Action": "s3:GetObject",
  "Resource": "arn:aws:s3:::taskflow-stg-eb-artifacts-359429618625/backend/*"
}
```

方針B: role policy で EB role に `backend/*` の `s3:GetObject` を許可する

- `aws-elasticbeanstalk-service-role` は production / staging で共有しているため、staging bucket に限定する
- `taskflow-stg-eb-ec2-role` は staging 専用 role のため、staging bucket に限定する

推奨:

- staging では方針Aを推奨する
- 理由は、共有 service role 側に staging 固有権限を増やさず、staging artifact bucket 内だけで許可範囲を閉じられるため

## 9. EB managed bucket policy

```sh
aws s3api get-bucket-policy \
  --bucket "${EB_MANAGED_BUCKET}" \
  --query Policy \
  --output text
```

期待結果:

- `taskflow-stg-eb-ec2-role` が `resources/environments/*` を参照できる
- 少なくとも以下が含まれること
  - `s3:ListBucket`
  - `s3:ListBucketVersions`
  - `s3:GetObject`
  - `s3:GetObjectVersion`

## 10. S3 bucket basic settings

```sh
aws s3api get-public-access-block \
  --bucket "${EB_S3_BUCKET}" \
  --output json
```

```sh
aws s3api get-bucket-ownership-controls \
  --bucket "${EB_S3_BUCKET}" \
  --output json
```

期待結果:

- backend artifact bucket は public access block が全て `true`
- `ObjectOwnership = BucketOwnerEnforced`

## 11. preflight 結果記録テンプレート

```md
## staging deploy preflight 結果

- 実施日:
- 実施者:
- 対象 ref:
- EB environment:
- deploy role:
- EB service role:
- EB EC2 role:
- artifact bucket:

### 結果

- EB 環境設定:
- OIDC trust:
- deploy role EB / EC2 / CloudFormation:
- deploy role artifact bucket:
- deploy role EB managed/resource bucket:
- EC2 role SSM Parameter:
- artifact bucket policy:
- EB managed bucket policy:
- S3 basic settings:

### deploy 可否

- deploy 実行可否:
- 保留事項:
```

## 12. NG 時の対応ルール

- `implicitDeny` が1つでも出た場合は deploy しない
- production role の現状を根拠に `implicitDeny` を許容しない
- 追加権限は、まず staging 用 JSON / bucket policy JSON に反映してから AWS 実環境へ適用する
- AWS 実環境へ適用した差分は、必ず作業メモとリポジトリ管理ファイルへ反映する
- production 共有 role に staging 固有権限を入れる場合は、staging bucket / staging parameter ARN に限定する
