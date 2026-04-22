# Phase7 完了判定メモ

## 結論

- 判定: `条件付き公開継続可`
- 判定日: `2026年4月13日`

Phase7 の主目的だった `公開 URL を使った最終総合テストと公開継続可否の確定` は概ね完了しています。`CloudFront` 公開 URL で `register / login / task CRUD / logout / session expired` を通し確認し、`未認証 / 認可 / バリデーション` の代表異常系も再確認できました。`CloudWatch Logs` では `requestId` と `eventId` を対応付けて追跡でき、`CloudWatch Alarm`、`initial_response_memo.md`、`rds_restore_memo.md` も参照可能です。

一方で、Phase6 からの継続課題である `db SG 例外 / secret ローテーション / CLI user 権限整理 / eventId 単位の監視未整備` は未解消のままです。加えて、Phase7 の spot check で `taskflow-prd-eb-environment-health` が `Elastic Beanstalk Green/Ok` と不整合な `ALARM` 状態であることを確認しました。そのため判定は `公開継続可` ではなく `条件付き公開継続可` とします。

## 実施情報

- 実施日: `2026年4月13日`
- 作業者: `未記入`
- 対象 ref:
  - `develop` を基準に確認
  - 公開 backend 反映版: `taskflow-prd-backend-20260413-0242-2bca460`
- backend version label:
  - `taskflow-prd-backend-20260413-0242-2bca460`
- frontend deploy 実施有無:
  - `実施あり`
  - Phase7 中に `deploy-frontend-prd` を再実行し、公開 bundle で再試験した
- frontend 公開 URL:
  - `https://d3jotedl3xn7u4.cloudfront.net`
- CloudFront Distribution:
  - `taskflow-prd-cf-web / E688SH91TX10P`
- Elastic Beanstalk Environment:
  - `Taskflow-prd-eb-app-env / e-gvcyudrrcs`

## Phase7 で完了した内容

- `5.1` 証跡フォルダと execution log を作成
- `5.2` 開始前チェックを実施し、Phase6 の残課題が悪化していないことを確認
- `5.3` MVP の検証対象と除外対象を固定
- `5.4` `GitHub Actions` の deploy 経路と production 承認前提を確認
- `5.5` 必要性を判定したうえで再デプロイ方針を整理
- `5.6` 公開 URL 正常系を実施し、途中で見つかった `frontend API base URL` と `backend port` の不整合を修正したうえで再試験完了
- `5.7` 異常系・認可境界を実施し、途中で見つかった `CloudFront custom error response` 問題を修正したうえで再試験完了
- `5.8` ネットワーク・API・CloudWatch Logs を確認
- `5.9` 監視・初動メモ・復旧導線の spot check を実施
- `5.10` 本完了判定メモを作成

## 検証結果

### deploy 経路

- `5.4` 時点で `ci` の最新成功を確認済み
- Phase7 中に `deploy-backend-prd` と `deploy-frontend-prd` を再実行し、公開 backend version label `taskflow-prd-backend-20260413-0242-2bca460` まで反映した
- `GitHub Actions -> Elastic Beanstalk / S3 / CloudFront` の本番反映導線は維持されている

### 正常系

- `register`: 成功
- `login`: 成功
- `task list / filter / detail`: 成功
- `task create / update / delete`: 成功
- `logout`: 成功
- `session expired`: 成功

### 異常系 / 認可

- 未認証 `GET /api/tasks`: `401 ERR-AUTH-001`
- 不正 token `GET /api/tasks`: `401 ERR-AUTH-003`
- 他人タスク詳細 `GET /api/tasks/{id}`: `403 ERR-AUTH-005`
- 他人タスク更新 `PUT /api/tasks/{id}`: `403 ERR-TASK-005`
- 他人タスク削除 `DELETE /api/tasks/{id}`: `403 ERR-TASK-006`
- 代表バリデーション:
  - 未入力で `タイトルを入力してください。`
  - `101` 文字で `タイトルは100文字以内で入力してください。`

### ログ

- `CloudWatch Logs` 対象:
  - `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/var/log/web.stdout.log`
  - `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/environment-health.log`
- `requestId` ベースで代表ログを確認済み:
  - `phase7-5-8-login-1776048485` -> `LOG-AUTH-001` / `LOG-REQ-001`
  - `phase7-5-8-invalid-1776048485` -> `LOG-AUTH-003` / `LOG-REQ-001`
  - `phase7-5-8-unauth-1776048485` -> `LOG-AUTH-006` / `LOG-REQ-001`
  - `phase7-5-8-tasks-1776048485` -> `LOG-REQ-001`
- `tail` と `bundle` のログ取得も可能であることを再確認した

### 監視 / 初動 / 復旧

- `CloudWatch Alarm` の基本構成は存在
  - `taskflow-prd-eb-environment-health`
  - `taskflow-prd-rds-cpu-high`
  - `taskflow-prd-rds-storage-low`
  - `taskflow-prd-rds-memory-low`
  - `taskflow-prd-cf-5xx-rate-high`
- `SNS` 通知先:
  - `ap-northeast-1`: `taskflow-prd-alerts-apne1 -> whgd0765@gmail.com`
  - `us-east-1`: `taskflow-prd-alerts-use1 -> whgd0765@gmail.com`
- `initial_response_memo.md` は `CloudFront -> EB -> Logs Insights -> EB logs -> RDS` の順を維持
- `rds_restore_memo.md` は `PITR / snapshot restore` の導線を参照可能
- `taskflow-prd-rds` は `available / PubliclyAccessible=false / LatestRestorableTime 確認可`
- 手動 snapshot `taskflow-prd-rds-manual-20260410-phase6` は `available`

### 除外対象

- `コメント / 添付ファイル / チーム管理 / 通知 / パスワードリセット / ヘルプ / 管理者画面` は Phase7 の MVP 合格条件に含めない
- 公開 UI 上でも上記機能の露出は確認していない

## Phase6 からの継続課題

- `db SG` 例外
  - `taskflow-prd-sg-db` に `sg-06db941d256dcaaa1` からの `5432` inbound が残っている
- secret ローテーション
  - `DB_PASSWORD / JWT_SECRET` はローテーション要判定のまま未実施
- CLI user 権限
  - `taskflow-cli-operator` は `AdministratorAccess` のまま
- eventId 単位の監視
  - `Metric Filter / Alarm` は未作成

### Phase7 で悪化していないこと

- 上記 4 点はいずれも Phase6 判定時点から新たな悪化は確認していない
- 公開 URL の正常系・異常系・ログ到達は Phase6 時点より具体的に確認範囲を広げられている

## Phase7 で新規に見つかった事項

### 解消済み

- `frontend` の公開環境 API base URL が `localhost` へ解決される不具合
- `deploy-backend-prd` の Procfile が `SERVER_PORT` と不整合だった不具合
- `CloudFront` の `403/404 -> /index.html` 変換により、`/api/*` の認可エラーが HTML に書き換わる不具合

### 未解消

- `taskflow-prd-eb-environment-health`
  - spot check 時点で `ALARM`
  - ただし `Elastic Beanstalk` 本体は `Ready / Green / Ok`
  - `AWS/ElasticBeanstalk EnvironmentHealth` の datapoint は直近でも `0.0` が多く、実状態と整合していない
  - 監視導線の所在確認は満たすが、Alarm のしきい値または参照メトリクスの見直しが必要

## 次フェーズへの引き継ぎ

- `taskflow-prd-eb-environment-health` のアラーム設計を見直す
- `DB_PASSWORD / JWT_SECRET` のローテーション要否判断を引き継ぎではなく実施判断まで進める
- `taskflow-cli-operator` の最小権限化方針を確定する
- `db SG` の追加 source `sg-06db941d256dcaaa1` の用途を確認し、不要なら削除する
- 将来監視として `eventId` 単位の `Metric Filter / Alarm` を追加検討する
- 本番障害時は `initial_response_memo.md` を起点に、`CloudFront / EB / Logs Insights / EB logs / RDS` の順で切り分ける
- DB 復旧や PITR が必要な場合は `rds_restore_memo.md` を参照し、既存本番 DB を直接上書きしない

## 参照資料

- `docs/02_製造/AWSデプロイ/作業手順_phase7.docx`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/phase7_execution_log.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes/phase7_5_6_public_flow_failure.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes/phase7_5_7_authz_boundary_issue.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes/phase7_5_8_network_api_cloudwatch.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes/phase7_5_9_monitoring_spot_check.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/01_Phase6完了判定メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/initial_response_memo.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/rds_restore_memo.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/cloudwatch_alarm_thresholds.md`
