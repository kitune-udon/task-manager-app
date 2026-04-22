# 17_staging用CloudWatch_SNS整備 作業メモ

## 更新日時

- 2026-04-20

## 対象

- 17. staging 用 CloudWatch / SNS を整備する

## 目的

- staging でも最低限の logs / alarm / notification 導線を持たせる
- production と識別できる `taskflow-stg-*` 命名で作成する
- production の EB health alarm は不整合があるため、機械的に複製しない

## 事前確認

- staging alarm は未作成
- production alarm は以下 4 件
  - `taskflow-prd-eb-environment-health`
  - `taskflow-prd-rds-cpu-high`
  - `taskflow-prd-rds-storage-low`
  - `taskflow-prd-rds-memory-low`
- production SNS topic
  - `arn:aws:sns:ap-northeast-1:359429618625:taskflow-prd-alerts-apne1`
  - email subscription あり

## EB log streaming 確認結果

- `StreamLogs`: `true`
- `RetentionInDays`: `7`
- `HealthStreamingEnabled`: `true`
- health log retention: `7`

## staging log group 確認結果

- `/aws/elasticbeanstalk/Taskflow-stg-eb-app-env/environment-health.log`
- `/aws/elasticbeanstalk/Taskflow-stg-eb-app-env/var/log/eb-engine.log`
- `/aws/elasticbeanstalk/Taskflow-stg-eb-app-env/var/log/eb-hooks.log`
- `/aws/elasticbeanstalk/Taskflow-stg-eb-app-env/var/log/nginx/access.log`
- `/aws/elasticbeanstalk/Taskflow-stg-eb-app-env/var/log/nginx/error.log`
- `/aws/elasticbeanstalk/Taskflow-stg-eb-app-env/var/log/web.stdout.log`
- retention はすべて `7` 日

## 作成する SNS topic

- `taskflow-stg-alerts-apne1`
  - Region: `ap-northeast-1`
  - 用途: EB / RDS alarm
- `taskflow-stg-alerts-use1`
  - Region: `us-east-1`
  - 用途: CloudFront alarm

## 作成する CloudWatch alarm

- `taskflow-stg-eb-environment-health`
  - Namespace: `AWS/ElasticBeanstalk`
  - Metric: `EnvironmentHealth`
  - Dimension: `EnvironmentName=Taskflow-stg-eb-app-env`
  - 条件: `>= 15`
  - 補足: production の `LessThanThreshold 15` は踏襲しない
- `taskflow-stg-rds-cpu-high`
  - Namespace: `AWS/RDS`
  - Metric: `CPUUtilization`
  - Dimension: `DBInstanceIdentifier=taskflow-stg-rds`
  - 条件: `> 80`
- `taskflow-stg-rds-storage-low`
  - Namespace: `AWS/RDS`
  - Metric: `FreeStorageSpace`
  - Dimension: `DBInstanceIdentifier=taskflow-stg-rds`
  - 条件: `< 5368709120`
- `taskflow-stg-rds-memory-low`
  - Namespace: `AWS/RDS`
  - Metric: `FreeableMemory`
  - Dimension: `DBInstanceIdentifier=taskflow-stg-rds`
  - 条件: `< 134217728`
- `taskflow-stg-cf-5xx-rate-high`
  - Region: `us-east-1`
  - Namespace: `AWS/CloudFront`
  - Metric: `5xxErrorRate`
  - Dimension: `DistributionId=EY2A56GLZXQQ4`, `Region=Global`
  - 条件: `> 5`

## 保留事項

- SNS email subscription の設定

## 実施結果

- SNS topic を作成した
  - `arn:aws:sns:ap-northeast-1:359429618625:taskflow-stg-alerts-apne1`
  - `arn:aws:sns:us-east-1:359429618625:taskflow-stg-alerts-use1`
- CloudWatch alarm を作成した
  - `taskflow-stg-eb-environment-health`: `OK`
  - `taskflow-stg-rds-cpu-high`: `OK`
  - `taskflow-stg-rds-storage-low`: `OK`
  - `taskflow-stg-rds-memory-low`: `OK`
  - `taskflow-stg-cf-5xx-rate-high`: `OK`
- alarm action
  - EB / RDS: `taskflow-stg-alerts-apne1`
  - CloudFront: `taskflow-stg-alerts-use1`

## 残対応

- staging SNS topic の email subscription は作成済み
- production と同じ通知先を使用
- 2 topic とも confirmation 済み

## SNS subscription 作成結果

- `taskflow-stg-alerts-apne1`
  - Protocol: `email`
  - Status: `Confirmed`
  - SubscriptionArn: `arn:aws:sns:ap-northeast-1:359429618625:taskflow-stg-alerts-apne1:f5db7638-95fd-4806-9fff-4ec6c11ed47c`
- `taskflow-stg-alerts-use1`
  - Protocol: `email`
  - Status: `Confirmed`
  - SubscriptionArn: `arn:aws:sns:us-east-1:359429618625:taskflow-stg-alerts-use1:0b205066-b229-4845-a3f5-fee2570b6134`

## 完了判定

- EB log streaming は有効
- staging の EB log group は作成済み
- RDS alarm 3 件は作成済みで `OK`
- EB health alarm は作成済みで `OK`
- CloudFront 5xx alarm は作成済みで `OK`
- SNS topic 2 件は作成済み
- SNS email subscription は 2 件とも confirmation 済み
