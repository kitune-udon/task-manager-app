# 18_stagingへdeployして動作確認 作業メモ

## 更新日時

- 2026-04-20

## 対象

- 18. staging へ deploy して動作確認する

## 目的

- staging 環境へ backend / frontend を deploy できることを確認する
- staging 公開 URL で最低限の導線が動くことを確認する
- production に影響がないことを確認する

## 事前確認

- staging 用 AWS リソースは作成済み
- GitHub `staging` Environment Variables は作成済みリソース分を登録済み
- staging workflow は `master` / `develop` ともに反映済み
  - ユーザー操作により反映完了
  - `origin/master`: `fdff0e665653985a497ed8cb18d3fe79c7bc976d`
  - `origin/develop`: `dcfb94540bfd4933c03a1ec87c4b0ffa5b13b35d`
  - `.github/workflows/deploy-backend-stg.yml` の存在を両ブランチで確認
  - `.github/workflows/deploy-frontend-stg.yml` の存在を両ブランチで確認

## 今回の進め方

- 手順書上は GitHub Actions から deploy する
- workflow が `master` / `develop` に反映済みになったため、GitHub Actions から初回 deploy を実施する
- 手元環境には `gh` CLI / GitHub API token がないため、workflow dispatch は GitHub 画面から実行する
- 実行後の GitHub Actions 結果確認、AWS 側の EB / CloudFront / Logs 確認はこの作業で続けて実施する

## backend deploy 方針

- PostgreSQL test DB を用意する
- `./gradlew test --no-daemon`
- `./gradlew bootJar --no-daemon`
- JAR と `Procfile` を zip 化する
- `taskflow-stg-eb-artifacts-359429618625` へ upload する
- Elastic Beanstalk application version を作成する
- `Taskflow-stg-eb-app-env` へ version label を適用する

## frontend deploy 方針

- `npm ci`
- `npm run build`
- `dist/` を `taskflow-stg-frontend-site-359429618625` へ upload する
- SPA router Function / Distribution 設定を確認する
- CloudFront invalidation を作成する

## スモークテスト方針

- `/login` が開く
- 未認証 `/api/*` が HTML に書き換わらない
- register / login / task list / create / update / delete を確認する
- EB health / CloudWatch Logs / Flyway / DB 接続エラーを確認する

## 保留事項

- backend deploy
- frontend deploy
- smoke test

## backend deploy 初回失敗

- GitHub Actions `deploy-backend-stg` は `Wait until environment is ready` で失敗
- Elastic Beanstalk event
  - `Failed to deploy application.`
  - `Service:AmazonEC2, Message:You do not have permission to perform the 'ec2:DescribeSubnets' action.`
- EB environment 状態
  - Status: `Ready`
  - Health: `Red`
  - HealthStatus: `Degraded`
  - VersionLabel: 未設定
- 原因
  - `taskflow-stg-github-actions-deploy-role` の inline policy に `ec2:DescribeSubnets` が不足
- 対応方針
  - deploy role policy に EC2 network read-only 権限を追加する
  - staging 実 IAM inline policy を更新後、`deploy-backend-stg` を再実行する
- 対応状況
  - `.github/workflows/github_actions_deploy_role_policy_stg.json` は修正済み
  - `.github/workflows/github_actions_deploy_role_policy.json` も production 側の将来 deploy に備えて同様に修正済み
  - staging 実 AWS IAM inline policy は反映済み
  - IAM simulator で以下が `allowed` になったことを確認
    - `ec2:DescribeSubnets`
    - `ec2:DescribeSecurityGroups`
    - `ec2:DescribeVpcs`
    - `ec2:DescribeInstances`

## production / staging 権限比較

- GitHub Actions deploy role の inline policy は production / staging で action セットが同一
  - 差分は prd/stg の S3 bucket ARN など環境別 resource のみ
- staging だけに不足している action は確認されなかった
- CloudTrail で実際に拒否された action
  - `ec2:DescribeSubnets`
  - Principal: `assumed-role/taskflow-stg-github-actions-deploy-role/GitHubActions`
- IAM simulator で production / staging ともに `implicitDeny`
  - `ec2:DescribeSubnets`
  - `ec2:DescribeSecurityGroups`
  - `ec2:DescribeVpcs`
  - `ec2:DescribeInstances`
- 対応
  - production / staging の policy JSON に `ElasticBeanstalkEC2NetworkReadOnly` を追加
  - read-only action として `DescribeSubnets`, `DescribeSecurityGroups`, `DescribeVpcs`, `DescribeInstances` を許可する方針

## backend deploy 2回目失敗

- `ec2:DescribeSubnets` 追加後、GitHub Actions `deploy-backend-stg` を再実行
- Elastic Beanstalk event
  - `Failed to deploy application.`
  - `Service:Amazon S3, Message:Access Denied: S3Bucket=elasticbeanstalk-env-resources-ap-northeast-1, S3Key=eb_patching_resources/instance_patch_extension.linux`
- EB environment 状態
  - Status: `Ready`
  - Health: `Red`
  - HealthStatus: `Degraded`
  - VersionLabel: 未設定
- 原因
  - `taskflow-stg-github-actions-deploy-role` が EB の AWS 管理 resource bucket から patch extension を読む `s3:GetObject` を持っていない
- 対応方針
  - deploy role policy に `ElasticBeanstalkServiceResourcesReadOnly` を追加する
  - `arn:aws:s3:::elasticbeanstalk-env-resources-ap-northeast-1/*` への `s3:GetObject` を許可する
- 対応状況
  - `.github/workflows/github_actions_deploy_role_policy_stg.json` は修正済み
  - `.github/workflows/github_actions_deploy_role_policy.json` も production 側の将来 deploy に備えて同様に修正済み
  - staging 実 AWS IAM inline policy は反映済み
  - IAM simulator で以下が `allowed` になったことを確認
    - `s3:GetObject` on `arn:aws:s3:::elasticbeanstalk-env-resources-ap-northeast-1/eb_patching_resources/instance_patch_extension.linux`
    - `ec2:DescribeSubnets`
    - `elasticbeanstalk:UpdateEnvironment`
    - `elasticbeanstalk:CreateApplicationVersion`

## backend deploy 3回目失敗

- EB patch resource bucket の `s3:GetObject` 追加後、GitHub Actions `deploy-backend-stg` を再実行
- Elastic Beanstalk event
  - `Failed to deploy application.`
  - `Service:AmazonEC2, Message:You do not have permission to perform the 'ec2:DescribeImages' action.`
- 原因
  - `taskflow-stg-github-actions-deploy-role` が EB deploy 中に AMI 情報を参照する `ec2:DescribeImages` を持っていない
- 対応方針
  - `ElasticBeanstalkEC2NetworkReadOnly` に `ec2:DescribeImages` を追加する
  - staging 実 IAM inline policy に反映後、`deploy-backend-stg` を再実行する
- 対応結果
  - `.github/workflows/github_actions_deploy_role_policy_stg.json` に `ec2:DescribeImages` を追加
  - `.github/workflows/github_actions_deploy_role_policy.json` にも同内容を反映
  - `taskflow-stg-github-actions-deploy-role` の inline policy `taskflow-stg-github-actions-deploy-policy` へ反映済み
  - IAM Simulator で以下が `allowed` であることを確認
    - `ec2:DescribeImages`
    - `ec2:DescribeSubnets`
    - `ec2:DescribeSecurityGroups`
    - `ec2:DescribeVpcs`
    - `ec2:DescribeInstances`
    - `elasticbeanstalk:UpdateEnvironment`
    - `elasticbeanstalk:CreateApplicationVersion`
    - `s3:GetObject` on `arn:aws:s3:::elasticbeanstalk-env-resources-ap-northeast-1/eb_patching_resources/instance_patch_extension.linux`

## backend deploy 4回目失敗

- `ec2:DescribeImages` 追加後、GitHub Actions `deploy-backend-stg` を再実行
- Elastic Beanstalk event
  - `Failed to deploy application.`
  - `Service:AmazonCloudFormation, Message:S3 error: Access Denied`
- 確認結果
  - 最新 application version は `UNPROCESSED`
  - source bundle は `s3://taskflow-stg-eb-artifacts-359429618625/backend/taskflow-stg-backend-20260420-0403-dcfb945.zip`
  - artifact zip は bucket 上に存在する
  - `taskflow-stg-eb-ec2-role` は source bundle への `s3:GetObject` が `implicitDeny`
  - `aws-elasticbeanstalk-service-role` も source bundle への `s3:GetObject` が `implicitDeny`
- 対応方針
  - staging artifact bucket に bucket policy を設定し、以下 role に `backend/*` の `s3:GetObject` を許可する
    - `arn:aws:iam::359429618625:role/aws-elasticbeanstalk-service-role`
    - `arn:aws:iam::359429618625:role/taskflow-stg-eb-ec2-role`
- 対応結果
  - `.github/workflows/s3_eb_artifact_bucket_policy_stg.json` を追加
  - `taskflow-stg-eb-artifacts-359429618625` に bucket policy を反映済み
  - 読み戻しで `AllowElasticBeanstalkRolesReadBackendBundles` が設定済みであることを確認

## 18章着手前状態への rollback

- rollback 理由
  - production EB が Warning になっているため、まず 18章の staging deploy 対応で入れた変更を戻す
  - production Warning の直接原因は instance health 上 `90 % of memory is in use.` であり、staging deploy 権限変更とは別件と判断
- rollback 対象
  - `taskflow-stg-github-actions-deploy-role` の inline policy から 18章中に追加した以下 Sid を削除
    - `ElasticBeanstalkServiceResourcesReadOnly`
    - `ElasticBeanstalkEC2NetworkReadOnly`
  - `taskflow-stg-eb-artifacts-359429618625` の bucket policy を削除
  - ローカル JSON から同じ差分を削除
  - `.github/workflows/s3_eb_artifact_bucket_policy_stg.json` を削除
- rollback 結果
  - staging 実 IAM inline policy を 18章着手前相当の `.github/workflows/github_actions_deploy_role_policy_stg.json` で上書き済み
  - 読み戻しで `ElasticBeanstalkServiceResourcesReadOnly` / `ElasticBeanstalkEC2NetworkReadOnly` が存在しないことを確認
  - `taskflow-stg-eb-artifacts-359429618625` の bucket policy を削除済み
  - 読み戻しで `NoSuchBucketPolicy` になっていることを確認
  - ローカルの deploy role policy JSON 差分は解消済み
