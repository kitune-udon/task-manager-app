# 5.9 監視・初動メモ・復旧導線スポット確認メモ

## 実施日時

- `2026-04-13 JST`

## 結論

- `CloudWatch Alarm` は `EB / RDS / CloudFront` の基本構成が存在する
- `SNS` 通知先 topic と email endpoint は両リージョンで参照できる
- `initial_response_memo.md` は `CloudFront -> EB -> Logs Insights -> EB logs -> RDS` の順を維持している
- `rds_restore_memo.md` は `PITR / snapshot restore` の導線を参照できる
- ただし `taskflow-prd-eb-environment-health` は spot check 時点で `ALARM` で、`EB Green/Ok` と不整合がある

## 1. CloudWatch Alarm

### ap-northeast-1

- `taskflow-prd-eb-environment-health`
  - state: `ALARM`
  - action: `arn:aws:sns:ap-northeast-1:359429618625:taskflow-prd-alerts-apne1`
- `taskflow-prd-rds-cpu-high`
  - state: `OK`
  - action: `arn:aws:sns:ap-northeast-1:359429618625:taskflow-prd-alerts-apne1`
- `taskflow-prd-rds-storage-low`
  - state: `OK`
  - action: `arn:aws:sns:ap-northeast-1:359429618625:taskflow-prd-alerts-apne1`
- `taskflow-prd-rds-memory-low`
  - state: `OK`
  - action: `arn:aws:sns:ap-northeast-1:359429618625:taskflow-prd-alerts-apne1`

### us-east-1

- `taskflow-prd-cf-5xx-rate-high`
  - state: `OK`
  - action: `arn:aws:sns:us-east-1:359429618625:taskflow-prd-alerts-use1`

## 2. SNS 通知先

- `ap-northeast-1`
  - topic: `taskflow-prd-alerts-apne1`
  - protocol: `email`
  - endpoint: `whgd0765@gmail.com`
- `us-east-1`
  - topic: `taskflow-prd-alerts-use1`
  - protocol: `email`
  - endpoint: `whgd0765@gmail.com`

## 3. 初動メモ確認

対象:

- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/initial_response_memo.md`

確認できたこと:

- 手順 2 で `CloudFront`
- 手順 3 で `Elastic Beanstalk`
- 手順 4 で `CloudWatch Logs / Logs Insights`
- 手順 5 で `Elastic Beanstalk logs`
- 手順 6 で `RDS`

上記の順で確認導線が維持されている。

## 4. RDS 復旧導線確認

対象:

- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/rds_restore_memo.md`

確認できたこと:

- `PITR`
  - console: `RDS > データベース > taskflow-prd-rds > アクション > 特定時点への復元`
  - CLI: `aws rds restore-db-instance-to-point-in-time`
- `snapshot restore`
  - console: `RDS > スナップショット > 対象 snapshot > スナップショットを復元`
  - CLI: `aws rds restore-db-instance-from-db-snapshot`
- 注意事項:
  - 本番 DB を直接上書きしない
  - 新しい `DB instance identifier` を使う
  - 復元後に接続確認、migration 状態確認、接続先切替要否を確認する

## 5. 復旧判断に必要な現況

- DB instance: `taskflow-prd-rds`
  - status: `available`
  - PubliclyAccessible: `false`
  - LatestRestorableTime: `2026-04-13T03:04:34+00:00`
- manual snapshot:
  - `taskflow-prd-rds-manual-20260410-phase6`
  - status: `available`
  - create time: `2026-04-10T00:37:24.569000+00:00`

## 6. 要補足事項

- `Elastic Beanstalk` 本体は
  - Status: `Ready`
  - Health: `Green`
  - HealthStatus: `Ok`
  だった
- しかし `taskflow-prd-eb-environment-health` は `ALARM`
- `AWS/ElasticBeanstalk EnvironmentHealth` を `EnvironmentName=Taskflow-prd-eb-app-env` で spot check すると、直近 20 分の datapoint がほぼ `0.0` で推移していた
- そのため、Alarm の存在確認自体は満たすが、`EB health alarm` はしきい値または参照メトリクスの整合を別途見直した方がよい
