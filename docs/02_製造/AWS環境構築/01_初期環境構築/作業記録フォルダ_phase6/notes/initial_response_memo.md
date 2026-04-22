# Initial Response Memo

## 目的

公開中の `taskflow-prd` 環境で障害や不審挙動が起きたときに、最初の確認順を固定する。2026年4月11日のログ設計反映以降は、`Elastic Beanstalk` の画面ログだけでなく `CloudWatch Logs` 上の構造化 JSON を `eventId` 軸で先に確認する。

## 初動の確認順

1. 症状を固定する
- 発生時刻
- 画面 URL
- 影響範囲
- ブラウザエラーの有無
- 直前に実施した変更

2. CloudFront 側の状態を確認する
- Distribution: `taskflow-prd-cf-web / E688SH91TX10P`
- Domain: `https://d3jotedl3xn7u4.cloudfront.net`
- 管理画面上のステータスが `Deployed` か
- 直近で invalidation や設定変更が入っていないか

CLI:
```bash
aws cloudfront get-distribution --id E688SH91TX10P
aws cloudfront list-invalidations --distribution-id E688SH91TX10P
curl -I https://d3jotedl3xn7u4.cloudfront.net/login
```

3. Elastic Beanstalk の「ヘルス」を確認する
- Environment: `Taskflow-prd-eb-app-env / e-gvcyudrrcs`
- 「ヘルス」が `Green`
- `ヘルスステータス` が `Ok`
- 「イベント」に直近の `ERROR` や rollback が無いか

CLI:
```bash
aws elasticbeanstalk describe-environments --environment-names Taskflow-prd-eb-app-env
aws elasticbeanstalk describe-events --environment-name Taskflow-prd-eb-app-env --max-records 20
```

4. CloudWatch Logs / Logs Insights を `eventId` 軸で確認する
- まず `CloudWatch > Logs Insights` を開く
- 対象ロググループ:
  - `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/var/log/web.stdout.log`
  - `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/environment-health.log`
- まず保存済みクエリ `taskflow-prd-all-events` で直近イベント全体を確認する
- 認証系は `taskflow-prd-auth-failures`、タスク監査系は `taskflow-prd-task-audit` を使う
- 主確認キー:
  - `eventId`
  - `requestId`
  - `errorCode`
  - `status`
  - `userId`
  - `taskId`
- 優先して見る代表 eventId:
  - 起動系: `LOG-APP-001` / `LOG-APP-002`
  - access 系: `LOG-REQ-001`
  - 認証系: `LOG-AUTH-001` / `LOG-AUTH-002` / `LOG-AUTH-003` / `LOG-AUTH-005` / `LOG-AUTH-006`
  - タスク監査系: `LOG-TASK-001` / `LOG-TASK-002` / `LOG-TASK-003`
- 注意:
  - `/actuator/health` の `200`
  - `/api/auth-test/**` の `2xx`
  - では `LOG-REQ-001` を出さない
  - そのため、これら path の access log 欠損だけでは直ちに障害扱いしない

5. Elastic Beanstalk の「ログ」/「イベント」を確認する
- まず `ログをリクエスト > 最後の100行` を取得する
- 必要なら `ログをリクエスト > ログ全体` を取得する
- 取得対象インスタンス: `i-04dc19e1314960747`
- 2026年4月10日時点の取得実績:
  - `tail` 取得: `2026-04-10 09:49 JST 頃`
  - `bundle` 取得: `2026-04-10 09:50 JST 頃`
- `eb-engine.log`、application log、access log に異常がないか確認する

CLI:
```bash
aws elasticbeanstalk request-environment-info --environment-name Taskflow-prd-eb-app-env --info-type tail
aws elasticbeanstalk retrieve-environment-info --environment-name Taskflow-prd-eb-app-env --info-type tail
aws elasticbeanstalk request-environment-info --environment-name Taskflow-prd-eb-app-env --info-type bundle
aws elasticbeanstalk retrieve-environment-info --environment-name Taskflow-prd-eb-app-env --info-type bundle
```

6. RDS の状態を確認する
- DB instance: `taskflow-prd-rds`
- `DBInstanceStatus=available` か
- `PubliclyAccessible=false` か
- `LatestRestorableTime` が確認できるか
- 2026年4月10日時点の確認値:
  - `LatestRestorableTime=2026-04-10T00:29:37+00:00`

CLI:
```bash
aws rds describe-db-instances --db-instance-identifier taskflow-prd-rds
aws rds describe-db-snapshots --db-instance-identifier taskflow-prd-rds --snapshot-type manual
```

7. 復旧方式を決める
- アプリ設定や secret 参照の問題:
  - Parameter Store / IAM policy / EB の環境プロパティを確認
- アプリ配備の問題:
  - EB の現在 version と直近 deploy を確認
- データ破損や DB 障害:
  - `rds_restore_memo.md` を参照し、PITR または snapshot restore を検討

CLI:
```bash
aws elasticbeanstalk describe-configuration-settings --application-name taskflow-prd-eb-app --environment-name Taskflow-prd-eb-app-env
aws iam get-policy-version --policy-arn arn:aws:iam::359429618625:policy/taskflow-prd-eb-ssm-read-policy --version-id v2
aws ssm get-parameter --name /taskflow/prd/backend/CORS_ALLOWED_ORIGINS
```

## 注意事項

- `CloudWatch Logs` 上では `eventId / requestId / errorCode / status` を主キーとして追跡する
- `web.stdout.log` では JSON の前に `Apr ... web[pid]:` のプレフィックスが付くため、保存済み `Logs Insights` クエリを使う前提でよい
- `/actuator/health` の `2xx` と `/api/auth-test/**` の `2xx` では `LOG-REQ-001` を出さない
- 現行の公開構成では `Elastic Beanstalk` 直 URL は `CloudFront origin-facing only` 制限の影響を受けるため、`/actuator/health` の直接ブラウザ再現は常にできるとは限らない
- 既存本番 DB `taskflow-prd-rds` を直接上書きしない
- DB 復元時は新しい DB identifier を使う
- `db SG` には `taskflow-prd-sg-app` 以外に `sg-06db941d256dcaaa1` も残っているため、一時接続経路の影響有無を切り分け時に意識する
- `taskflow-cli-operator` は現時点で広い権限を持つため、操作ミスに注意する
