# production 実環境 IAM / EB / S3 棚卸し 作業メモ

## 実施日

- 2026-04-20

## 目的

- staging deploy 時に権限不足が段階的に発生した原因を整理するため、production の実環境設定を棚卸しする
- 対象は Elastic Beanstalk、IAM role / policy、S3 bucket policy

## Elastic Beanstalk

- Application
  - `taskflow-prd-eb-app`
- Environment
  - `Taskflow-prd-eb-app-env`
- Environment ID
  - `e-gvcyudrrcs`
- CNAME
  - `Taskflow-prd-eb-app-env.eba-8xzqpp6j.ap-northeast-1.elasticbeanstalk.com`
- Platform / instance
  - Corretto 17 on Amazon Linux 2023
  - `t3.micro`
- Environment type
  - `SingleInstance`
- Instance profile
  - `aws-elasticbeanstalk-ec2-role`
- Service role
  - `arn:aws:iam::359429618625:role/aws-elasticbeanstalk-service-role`
- Application environment variables
  - `SERVER_PORT=5000`
  - `SPRING_PROFILES_ACTIVE=prod`
  - Gradle / Maven default variablesあり
- Current deployed version
  - `taskflow-prd-backend-20260413-1349-7b7b99f`
- Current backend source bundle
  - `s3://taskflow-prd-eb-artifacts-359429618625/backend/taskflow-prd-backend-20260413-1349-7b7b99f.zip`

## GitHub Actions deploy role

- Role
  - `arn:aws:iam::359429618625:role/taskflow-prd-github-actions-deploy-role`
- Trust policy
  - Federated principal: `arn:aws:iam::359429618625:oidc-provider/token.actions.githubusercontent.com`
  - `sts:AssumeRoleWithWebIdentity`
  - `token.actions.githubusercontent.com:aud = sts.amazonaws.com`
  - `token.actions.githubusercontent.com:sub = repo:kitune-udon/task-manager-app:environment:production`
- Attached managed policies
  - なし
- Inline policy
  - `taskflow-prd-github-actions-deploy-policy`
- 主な許可
  - `elasticbeanstalk:CreateApplicationVersion`
  - `elasticbeanstalk:DescribeEnvironments`
  - `elasticbeanstalk:DescribeEvents`
  - `elasticbeanstalk:UpdateEnvironment`
  - `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject`, `s3:ListBucket`
    - `taskflow-prd-eb-artifacts-359429618625`
    - `taskflow-prd-frontend-site-359429618625`
  - `cloudformation:*`
    - `arn:aws:cloudformation:*:*:stack/awseb-*`
    - `arn:aws:cloudformation:*:*:stack/eb-*`
  - Elastic Beanstalk managed bucket 操作用の S3 権限
    - `elasticbeanstalk-ap-northeast-1-359429618625`
  - CloudFront distribution / function 更新権限
- IAM Simulator 確認
  - `elasticbeanstalk:CreateApplicationVersion`: `allowed`
  - `elasticbeanstalk:UpdateEnvironment`: `allowed`
  - `s3:GetObject` on production backend bundle: `allowed`
  - `ec2:DescribeSubnets`: `implicitDeny`
  - `ec2:DescribeImages`: `implicitDeny`
  - `s3:GetObject` on `elasticbeanstalk-env-resources-ap-northeast-1/eb_patching_resources/instance_patch_extension.linux`: `implicitDeny`

## Elastic Beanstalk service role

- Role
  - `arn:aws:iam::359429618625:role/aws-elasticbeanstalk-service-role`
- Trust policy
  - Service principal: `elasticbeanstalk.amazonaws.com`
  - `sts:AssumeRole`
- Attached managed policies
  - `AWSElasticBeanstalkEnhancedHealth`
  - `AWSElasticBeanstalkManagedUpdatesCustomerRolePolicy`
- Inline policies
  - なし
- IAM Simulator 確認
  - `s3:GetObject` on production backend bundle: `implicitDeny`

## Elastic Beanstalk EC2 instance profile role

- Role
  - `arn:aws:iam::359429618625:role/aws-elasticbeanstalk-ec2-role`
- Trust policy
  - Service principal: `ec2.amazonaws.com`
  - `sts:AssumeRole`
- Attached managed policies
  - `AWSElasticBeanstalkMulticontainerDocker`
  - `AWSElasticBeanstalkWebTier`
  - `AWSElasticBeanstalkWorkerTier`
  - `taskflow-prd-eb-ssm-read-policy`
- Inline policies
  - なし
- `taskflow-prd-eb-ssm-read-policy`
  - Default version: `v2`
  - `ssm:GetParameter` を許可
  - 許可対象:
    - `/taskflow/prd/backend/DB_URL`
    - `/taskflow/prd/backend/DB_USERNAME`
    - `/taskflow/prd/backend/DB_PASSWORD`
    - `/taskflow/prd/backend/JWT_SECRET`
    - `/taskflow/prd/backend/JWT_EXPIRATION_MILLIS`
    - `/taskflow/prd/backend/CORS_ALLOWED_ORIGINS`
- IAM Simulator 確認
  - `s3:GetObject` on production backend bundle: `implicitDeny`

## S3: backend artifact bucket

- Bucket
  - `taskflow-prd-eb-artifacts-359429618625`
- Bucket policy
  - なし
  - `NoSuchBucketPolicy`
- Public Access Block
  - `BlockPublicAcls=true`
  - `IgnorePublicAcls=true`
  - `BlockPublicPolicy=true`
  - `RestrictPublicBuckets=true`
- Object Ownership
  - `BucketOwnerEnforced`
- 補足
  - production deploy role は、この bucket の `backend/*.zip` に対する `s3:GetObject` が `allowed`
  - EB service role / EC2 instance profile role は、この bucket の `backend/*.zip` に対する `s3:GetObject` が `implicitDeny`

## S3: frontend bucket

- Bucket
  - `taskflow-prd-frontend-site-359429618625`
- Bucket policy
  - CloudFront service principal に `s3:GetObject` を許可
  - SourceArn:
    - `arn:aws:cloudfront::359429618625:distribution/E688SH91TX10P`
- Public Access Block
  - `BlockPublicAcls=true`
  - `IgnorePublicAcls=true`
  - `BlockPublicPolicy=true`
  - `RestrictPublicBuckets=true`

## S3: Elastic Beanstalk managed bucket

- Bucket
  - `elasticbeanstalk-ap-northeast-1-359429618625`
- Bucket policy
  - `aws-elasticbeanstalk-ec2-role` に log upload 用 `s3:PutObject` を許可
  - `aws-elasticbeanstalk-ec2-role` と `taskflow-stg-eb-ec2-role` に `resources/environments/*` の参照を許可
    - `s3:ListBucket`
    - `s3:ListBucketVersions`
    - `s3:GetObject`
    - `s3:GetObjectVersion`
  - bucket delete は deny
- Public Access Block
  - `BlockPublicAcls=false`
  - `IgnorePublicAcls=false`
  - `BlockPublicPolicy=false`
  - `RestrictPublicBuckets=false`

## 棚卸しで分かったこと

- production deploy role には staging deploy 中に不足した `ec2:DescribeSubnets` / `ec2:DescribeImages` も、EB 管理 resource bucket `elasticbeanstalk-env-resources-ap-northeast-1/*` の `s3:GetObject` も含まれていない
- production backend artifact bucket に bucket policy はない
- production backend bundle を明示的に読めるのは GitHub Actions deploy role
- EB service role / EC2 instance profile role は production backend artifact bucket の object を直接読む権限を持っていない
- したがって、production の実環境をそのまま「完全な正」として staging にコピーしても、今回 staging で出た deploy 時権限不足を事前に潰せるとは限らない
- staging では新規環境・新規 role・初回 application version 適用の組み合わせにより、production の既存運用時とは異なる EB / CloudFormation の権限チェック経路が表面化した可能性がある

## 次の検討ポイント

- 18章着手前チェックとして、production 実設定コピーではなく staging deploy 操作そのものを IAM Simulator で検証する
- staging deploy role に追加すべき権限は、production の現状差分だけでなく、実際に staging deploy で要求された API を基準に整理する
- backend artifact bucket を custom bucket にする場合、EB がどの role で source bundle を読むかを前提化し、role policy または bucket policy のどちらで許可するかを手順書に明記する
