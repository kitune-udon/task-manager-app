# Phase6 完了判定メモ

## 結論

- 判定: `条件付きで Phase6 完了`
- 判定日: `2026年4月10日`
- 追補反映日: `2026年4月12日`

Phase6 の主目的だった `公開済み MVP 環境に対する最低限のセキュリティ・運用整備` は概ね完了しています。`S3 / CloudFront / Security Group / RDS / Elastic Beanstalk` の公開設定監査、`RDS` の自動バックアップと手動スナップショット確認、`EB の「ヘルス」/「ログ」` の取得手順整理、`初動メモ` と `復旧メモ` の整備までは完了しており、公開環境の運用基盤として最低限の土台はできています。

加えて、2026年4月12日までの追補で `ログ設計反映済み` の状態まで進めました。`Elastic Beanstalk` の `instance log streaming` と `environment health log streaming` を有効化し、`CloudWatch Logs` 上で構造化 JSON ログを `eventId` ベースで検索できること、代表 `eventId` の到達、`Logs Insights` 保存クエリ、`RDS PostgreSQL logs export`、`CloudTrail` の `multi-Region trail + S3 保存 + log file validation`、`Elastic Beanstalk / RDS / CloudFront` の基本 `CloudWatch Alarm` を確認・整備しています。

一方で、`db SG` には `taskflow-prd-sg-app` 以外の `5432` inbound source が 1 件残っており、`DB_PASSWORD / JWT_SECRET` はローテーション要と判定したものの今回は未実施です。また、`taskflow-cli-operator` も `AdministratorAccess` のままで、期限付きの例外記録対応にとどめています。そのため、判定は `完了` ではなく `条件付き完了` とします。

## 完了した内容

- `5.1` Phase6 用の証跡フォルダと実行ログを作成
- `5.2` CLI identity / region / EB / CloudFront / RDS の現在状態を固定
- `5.3` Elastic Beanstalk の `environmentsecrets` を監査し、6 項目が SSM ARN 参照であることを確認
- `5.4` `taskflow-prd-eb-ssm-read-policy` を監査し、`ssm:GetParameter` + 6 ARN のみに絞られていることを確認
- `5.5` S3 / CloudFront の公開設定を監査し、private bucket + CloudFront 構成が維持されていることを確認
- `5.6` app SG / db SG / RDS の公開設定を監査し、例外 SG の残存を確認
- `5.7` RDS の自動バックアップと `LatestRestorableTime` を確認し、manual snapshot `taskflow-prd-rds-manual-20260410-phase6` を作成
- `5.8` Elastic Beanstalk の「ヘルス」が `Green / Ok` であること、`tail` と `bundle` ログ取得、初動メモ整備を実施
- `5.9` `DB_PASSWORD / JWT_SECRET` はローテーション要と判定
- `5.10` `taskflow-cli-operator` は権限変更せず、期限付きの例外記録を作成
- `追補 5.3` Elastic Beanstalk の `instance log streaming` を有効化し、CloudWatch Logs への転送を確認
- `追補 5.4` `environment health log streaming` を有効化し、health 用ロググループを確認
- `追補 5.5` `CloudWatch Logs` で構造化 JSON ログを `eventId` ベースで検索し、代表 `eventId` の到達を確認
- `追補 5.6` `Logs Insights` 保存クエリ `taskflow-prd-all-events`、`taskflow-prd-auth-failures`、`taskflow-prd-task-audit` を整備
- `追補 5.7` `/actuator/health` と `/api/auth-test/**` の `2xx` では `LOG-REQ-001` が出ないことを運用メモへ反映
- `追補 5.8` `CloudWatch Alarm` を監査し、`ap-northeast-1` / `us-east-1` ともアラーム未設定であることを確認
- `追補 5.8（再実施）` `SNS` 通知先を整備し、`Elastic Beanstalk / RDS / CloudFront` の基本 `CloudWatch Alarm` を新規作成
- `追補 5.9` `RDS PostgreSQL logs export` を有効化し、`CloudTrail` 証跡 `taskflow-prd-trail` を新規作成

## 主要な確認済みリソース

### frontend / 公開導線

- frontend URL: `https://d3jotedl3xn7u4.cloudfront.net`
- CloudFront Distribution: `taskflow-prd-cf-web / E688SH91TX10P`
- default root object: `index.html`
- `/api/*` behavior: `taskflow-prd-origin-backend`

### Elastic Beanstalk

- Environment: `Taskflow-prd-eb-app-env / e-gvcyudrrcs`
- Status: `Ready`
- ヘルス: `Green`
- HealthStatus: `Ok`
- environment secrets:
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `JWT_SECRET`
  - `JWT_EXPIRATION_MILLIS`
  - `CORS_ALLOWED_ORIGINS`

### CloudWatch / CloudTrail

- app log group: `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/var/log/web.stdout.log`
- health log group: `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/environment-health.log`
- saved queries:
  - `taskflow-prd-all-events`
  - `taskflow-prd-auth-failures`
  - `taskflow-prd-task-audit`
- CloudTrail trail: `taskflow-prd-trail`
  - multi-region trail: `yes`
  - S3 bucket: `aws-cloudtrail-logs-359429618625-0b416378`
  - log file validation: `enabled`
  - CloudWatch Logs integration: `not configured`
- SNS topics:
  - `ap-northeast-1`: `taskflow-prd-alerts-apne1`
  - `us-east-1`: `taskflow-prd-alerts-use1`
- basic alarms:
  - `taskflow-prd-eb-environment-health`
  - `taskflow-prd-rds-cpu-high`
  - `taskflow-prd-rds-storage-low`
  - `taskflow-prd-rds-memory-low`
  - `taskflow-prd-cf-5xx-rate-high`

### IAM / SSM

- policy: `taskflow-prd-eb-ssm-read-policy`
- default version: `v2`
- action: `ssm:GetParameter` のみ
- resource: backend 用 6 ARN のみ
- role attachment: `aws-elasticbeanstalk-ec2-role`

### Network / DB

- app SG: `taskflow-prd-sg-app / sg-0e79604022597200d`
  - `HTTP/80 -> pl-58a04531 (CloudFront origin-facing only)`
- db SG: `taskflow-prd-sg-db / sg-06bfddec58a3b9cec`
  - `5432 -> sg-0e79604022597200d`
  - `5432 -> sg-06db941d256dcaaa1`
- RDS: `taskflow-prd-rds`
  - status: `available`
  - publicly accessible: `false`
  - backup retention period: `1`
  - latest restorable time: `2026-04-10T00:29:37+00:00`
  - CloudWatch Logs exports: `PostgreSQL log`
- manual snapshot:
  - `taskflow-prd-rds-manual-20260410-phase6`
  - status: `available`

## 検証結果

- app SG に `0.0.0.0/0` の inbound は残っていない
- frontend bucket の `Block Public Access` 4 項目はすべて有効
- S3 website hosting は無効
- bucket policy は CloudFront distribution `E688SH91TX10P` からの `s3:GetObject` のみ許可
- EB の environment secrets は 6 項目すべて ARN 参照
- instance profile 側の SSM 読み取り権限は 6 ARN のみに制限
- RDS の自動バックアップは有効で `LatestRestorableTime` を確認済み
- manual snapshot 1 件を作成済み
- EB の `最後の100行` と `ログ全体(bundle)` の取得手順を確認済み
- `CloudWatch Logs` で構造化 JSON ログを `eventId` ベースで検索できる
- 代表 `eventId` として `LOG-APP-001`、`LOG-APP-002`、`LOG-REQ-001`、`LOG-AUTH-001`、`LOG-AUTH-006`、`LOG-TASK-001` を確認済み
- `/actuator/health` と `/api/auth-test/**` の `2xx` では `LOG-REQ-001` を出さないことを運用メモへ反映済み
- `RDS PostgreSQL logs export` を有効化済み
- `CloudTrail` は multi-Region trail + S3 保存 + log file validation を満たす
- `CloudWatch Alarm` は `ap-northeast-1` / `us-east-1` の両リージョンに基本アラームを整備済み
- `taskflow-cli-operator` の権限整理は `例外記録あり` で締めている

## Phase6 の残課題

- db SG 例外
  - `taskflow-prd-sg-db` に `sg-06db941d256dcaaa1` からの `5432` inbound が残っている
  - 将来拡張の可能性を見込んで保持したが、厳密な最小公開には未達
- secret ローテーション
  - `DB_PASSWORD / JWT_SECRET` はローテーション要と判定したが今回は未実施
  - 特に `JWT_SECRET` は Parameter Store metadata 上 `Version=1` のままで、更新証跡がない
- CLI user 権限
  - `taskflow-cli-operator` は `AdministratorAccess` のまま
  - `2026-04-30` までに最小権限化方針を確定する必要がある
- eventId 単位の監視
  - `Metric Filter / Alarm` は未作成
  - 今回は `Logs Insights` 保存クエリまでを整備範囲とした
## 次フェーズへの引き継ぎ

- DB_PASSWORD / JWT_SECRET のローテーションを実施する場合は、既存 snapshot `taskflow-prd-rds-manual-20260410-phase6` を前提に進める
- DB 復元や PITR が必要になった場合は `rds_restore_memo.md` を参照する
- 障害初動は `initial_response_memo.md` の確認順に従う
- 障害時は `CloudWatch Logs` の保存済みクエリ `taskflow-prd-all-events` を起点に `eventId / requestId / errorCode / status` を先に確認する
- `/actuator/health` の `2xx` と `/api/auth-test/**` の `2xx` で `LOG-REQ-001` が出ないのは仕様どおりと扱う
- メトリクス監視は `CloudWatch Alarm + SNS` を前提に運用し、将来は `eventId` 単位の `Metric Filter / Alarm` を追加検討する
- `taskflow-cli-operator` の使用コマンドを棚卸しし、customer managed policy への置き換えを検討する
- `db SG` の追加 source `sg-06db941d256dcaaa1` は、実際の用途が固まった時点で見直す
- CloudFront の OAC 整理は完了済みのため、後続では追加対応不要

## 参照資料

- `docs/02_製造/AWSデプロイ/作業手順_phase6.docx`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/phase6_execution_log.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/rds_restore_memo.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/initial_response_memo.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/cloudwatch_log_design_alignment.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/cloudwatch_alarm_thresholds.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/cli_user_exception_memo.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/01_Phase4完了判定メモ.md`
