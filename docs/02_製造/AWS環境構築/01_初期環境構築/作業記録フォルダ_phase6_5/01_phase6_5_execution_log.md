# Phase6.5 実行ログ

## 概要
- 目的:
  - GitHub Actions
  - GitHub Environment 承認
  - AWS OIDC
  を使って本番 deploy を自動化する。
- 結果:
  - `ci` 成功
  - `deploy-backend-prd` 成功
  - `deploy-frontend-prd` 成功
- 検証日: `2026-04-12`

## GitHub 側
- `production` Environment を作成
- Required reviewer を設定
- deploy 対象ブランチを `develop` に制限
- 同一作業者による初回検証のため、`Prevent self-review` を一時的に無効化

## AWS 側
- `token.actions.githubusercontent.com` 向け OIDC Provider を作成
- deploy role `taskflow-prd-github-actions-deploy-role` を作成
- backend 用 artifact バケット `taskflow-prd-eb-artifacts-359429618625` を作成
- backend deploy 先:
  - Application: `taskflow-prd-eb-app`
  - Environment: `Taskflow-prd-eb-app-env`
- frontend deploy 先:
  - S3 bucket: `taskflow-prd-frontend-site-359429618625`
  - CloudFront distribution: `E688SH91TX10P`

## 検証中に判明した deploy role policy の追加調整
初回の Elastic Beanstalk 環境 bootstrap では、最初のドラフト権限だけでは不足した。切り分けの中で、以下の追加権限が必要と分かった。

- Elastic Beanstalk 管理 S3 バケットの bootstrap
  - `s3:CreateBucket`
  - `s3:PutBucketOwnershipControls`
- Elastic Beanstalk 管理 S3 バケットのオブジェクト操作
  - `s3:DeleteObject`
  - `s3:Get*`
  - `s3:List*`
  - `s3:PutObject`
  - `s3:PutObjectAcl`
  - `s3:PutObjectVersionAcl`
  - `s3:GetBucketLocation`
  - `s3:GetBucketPolicy`
  - `s3:PutBucketPolicy`
- Elastic Beanstalk 管理 CloudFormation stack 参照
  - `cloudformation:*` on `awseb-*` / `eb-*`
- Auto Scaling 参照権限
  - `autoscaling:DescribeAccountLimits`
  - `autoscaling:DescribeAutoScalingGroups`
  - `autoscaling:DescribeAutoScalingInstances`
  - `autoscaling:DescribeLaunchConfigurations`
  - `autoscaling:DescribeLoadBalancers`
  - `autoscaling:DescribeNotificationConfigurations`
  - `autoscaling:DescribeScalingActivities`
  - `autoscaling:DescribeScheduledActions`
- Auto Scaling プロセス制御
  - `autoscaling:ResumeProcesses`
  - `autoscaling:SuspendProcesses`
- EC2 Launch Template 参照
  - `ec2:DescribeLaunchTemplates`
  - `ec2:DescribeLaunchTemplateVersions`
- Elastic Beanstalk 向け CloudWatch Logs 権限
  - `logs:DescribeLogGroups`
  - `logs:CreateLogGroup`
  - `logs:DeleteLogGroup`
  - `logs:PutRetentionPolicy`

## 検証中に加えた workflow の改善
- `deploy-backend-prd.yml` の待機ロジックを強化し、deploy 成功条件を次の 4 点にした。
  - `Status = Ready`
  - `Health = Green`
  - `HealthStatus = Ok`
  - `VersionLabel = expected version label`
- Elastic Beanstalk が次のような failure event を出したら、workflow を即失敗させるようにした。
  - `Failed to deploy application`
  - `Application update failed`
  - `Incorrect application version found`

## 成功確認結果
- backend の実行中バージョン:
  - `taskflow-prd-backend-20260412-0947-412b8c3`
- Elastic Beanstalk イベントで確認できたこと:
  - `New application version was deployed to running EC2 instances.`
  - `Environment update completed successfully.`
- frontend workflow で確認できたこと:
  - S3 upload 成功
  - CloudFront invalidation 成功

## フォローアップ
- 二重承認運用へ戻す場合は、初回検証後に `production` の `Prevent self-review` を再度有効化する。
- 今後権限を追加する場合は、deploy role の実際の inline policy とテンプレートを常に同じ状態に保つ。
