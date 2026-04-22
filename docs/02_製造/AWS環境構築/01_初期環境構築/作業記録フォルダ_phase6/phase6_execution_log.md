# Phase6 Execution Log

> ログ設計反映版で実施

## 基本情報

- 作業日: 2026年4月10日
- 作業者: `未記入`
- AWS アカウント: `359429618625`
- リージョン: `ap-northeast-1`
- 対象 Phase: `Phase6`
- 参照手順書: `docs/02_製造/AWSデプロイ/作業手順_phase6.docx`
- 追補版手順書: `docs/02_製造/AWSデプロイ/作業手順_phase6_追補版.docx`

## Phase6 で固定して使う値

| 項目 | 内容 |
| --- | --- |
| AWS アカウント | `359429618625` |
| リージョン | `ap-northeast-1` |
| frontend 公開 URL | `https://d3jotedl3xn7u4.cloudfront.net` |
| CloudFront Distribution | `taskflow-prd-cf-web / E688SH91TX10P` |
| frontend バケット | `taskflow-prd-frontend-site-359429618625` |
| Elastic Beanstalk Environment | `Taskflow-prd-eb-app-env / e-gvcyudrrcs` |
| Elastic Beanstalk CNAME | `Taskflow-prd-eb-app-env.eba-8xzqpp6j.ap-northeast-1.elasticbeanstalk.com` |
| app SG | `taskflow-prd-sg-app / sg-0e79604022597200d` |
| db SG | `taskflow-prd-sg-db / sg-06bfddec58a3b9cec` |
| RDS | `taskflow-prd-rds / taskflow-prd-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com:5432` |
| Parameter Store prefix | `/taskflow/prd/backend/` |
| EB が参照する Parameter | `DB_URL / DB_USERNAME / DB_PASSWORD / JWT_SECRET / JWT_EXPIRATION_MILLIS / CORS_ALLOWED_ORIGINS` |
| CLI 実行 user | `taskflow-cli-operator` |

## 証跡フォルダ

- screenshots: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/screenshots`
- notes: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes`
- exports: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/exports`

## 参照資料

- `docs/01_設計/11_ログ設計書.xlsx`
- `docs/02_製造/AWSデプロイ/作業手順_phase6_追補版.docx`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/01_Phase6完了判定メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/cloudwatch_alarm_thresholds.md`
- `docs/02_製造/AWSデプロイ/作業手順_phase6.docx`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/01_Phase4完了判定メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/phase4_execution_log.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/exports/taskflow-prd-eb-ssm-read-policy-with-cors.json`

## 実行ログ

### 5.1 追補: ログ設計反映版の証跡ファイルを準備する

- 実施日時: `2026-04-11 JST`
- 結果: `完了`
- 対応内容:
  - `phase6_execution_log.md` の先頭へ `ログ設計反映版で実施` を追記
  - 参照資料へ `11_ログ設計書.xlsx`、`作業手順_phase6_追補版.docx`、`01_Phase6完了判定メモ.md` を追加
  - `notes/cloudwatch_log_design_alignment.md` を新規作成
- 補足:
  - AWS 側の設定変更は未着手
  - 以降は `作業手順_phase6_追補版.docx` の `5.1` から順に進める

### 5.2 追補: 最新ログ設計の要点を整理する

- 実施日時: `2026-04-11 JST`
- 結果: `完了`
- 対応内容:
  - `notes/cloudwatch_log_design_alignment.md` に CloudWatch 上でのログの見え方を整理
  - `application / security / audit` は論理ロガー名であり、CloudWatch Logs では EB の console 出力を `instance log streaming` で確認する前提へ補正
  - CloudWatch Logs 上の主確認キーを `eventId / requestId / errorCode / status / userId / taskId` に整理
  - `/actuator/health` と `/api/auth-test/**` の `2xx` では `LOG-REQ-001` が出ないことを注意点として明記
- 補足:
  - まだ AWS コンソールでの設定変更・確認は行っていない
  - 次項目 `5.3` から AWS Web コンソールでの確認作業に入る

### 5.3 追補: Elastic Beanstalk の instance log streaming を有効化または再確認する

- 実施日時: `2026-04-11 JST`
- 結果: `完了`
- 実施方法:
  - AWS Web コンソール
  - `Elastic Beanstalk > taskflow-prd-eb-app > Taskflow-prd-eb-app-env > 設定 > 更新、モニタリング、ログ記録`
- 確認結果:
  - `ログストリーミング`: `有効化`
  - `ログの保持`: `7`
  - `ロググループ`: `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env`
- 補足:
  - 構造化 JSON ログは専用ロググループ分離ではなく、Elastic Beanstalk の console 出力を CloudWatch Logs へ転送して確認する前提
  - 次項目で `environment health log streaming` も続けて確認する

### 5.4 追補: Elastic Beanstalk の environment health log streaming を有効化または再確認する

- 実施日時: `2026-04-11 JST`
- 結果: `完了`
- 実施方法:
  - AWS Web コンソール
  - `Elastic Beanstalk > taskflow-prd-eb-app > Taskflow-prd-eb-app-env > 設定 > 更新、モニタリング、ログ記録 > 編集`
- 確認結果:
  - `CloudWatch Logs へのヘルスイベントのストリーミング`: `有効化`
  - `保持`: `7`
  - `ロググループ`: `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/environment-health.log`
- 補足:
  - `システム=enhaned` の前提でヘルスイベントを CloudWatch Logs へ転送する構成になっている
  - これで instance logs と environment health logs の両方が CloudWatch Logs 側で追跡可能になった

### 5.5 追補: CloudWatch Logs の到達確認を eventId ベースで実施する

- 実施日時: `2026-04-11 JST`
- 結果: `完了`
- 実施方法:
  - AWS Web コンソール
  - `CloudWatch > Logs Insights`
  - 対象ロググループ:
    - `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/var/log/web.stdout.log`
    - `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/environment-health.log`
- 確認内容:
  - `web.stdout.log` で `eventId` を含む構造化 JSON ログが検索できることを確認
  - `CloudWatch Logs` 上では親ロググループではなく、`/var/log/web.stdout.log` 配下にアプリ本体ログが格納されることを確認
  - 代表 eventId として以下を確認:
    - `LOG-APP-001`
    - `LOG-APP-002`
    - `LOG-REQ-001`
    - `LOG-AUTH-001`
    - `LOG-AUTH-006`
    - `LOG-TASK-001`
- 補足:
  - `LOG-SYS-001` と `LOG-APP-004` は本番で安全に再現しないため、現時点では `未再現・設計確認のみ` とする
  - `LOG-REQ-001` の除外条件である `/actuator/health` と `/api/auth-test/**` の `2xx` は、後続手順で運用メモへ明記する

### 5.6 追補: Logs Insights の代表クエリを保存する

- 実施日時: `2026-04-11 JST`
- 結果: `完了`
- 対象ロググループ:
  - `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/var/log/web.stdout.log`
- 保存したクエリ:
  - `taskflow-prd-all-events`
  - `taskflow-prd-auth-failures`
  - `taskflow-prd-task-audit`
- 補足:
  - Elastic Beanstalk の `web.stdout.log` では JSON の前に syslog 風プレフィックスが付くため、保存クエリは `@message` から JSON 本文を `parse` で抽出する形へ補正した
  - 補正後クエリ本文は `notes/cloudwatch_log_design_alignment.md` に記録した

### 5.7 追補: LOG-REQ-001 の除外条件を明記したうえで access log を確認する

- 実施日時: `2026-04-11 JST`
- 結果: `完了`
- 確認内容:
  - 通常 API リクエストでは `LOG-REQ-001` を確認済み
  - `/api/auth-test/me` はブラウザ DevTools から `200` 応答を確認
  - `Logs Insights` 上で `/api/auth-test/me` に対する `LOG-REQ-001` を確認したところ、該当 1 件は `401` 応答であり、`2xx` 除外条件とは矛盾しない
- 判定:
  - `/api/auth-test/**` の `2xx` は `LOG-REQ-001` 除外対象として扱ってよい
  - `/actuator/health` の `2xx` も除外対象だが、現行構成では `CloudFront origin-facing only` 制限により外部ブラウザから直接再現しないため、今回は設計確認のみとする
- 補足:
  - health check 系 / auth-test 系で access log 欠損を見ても、直ちに障害扱いしない旨を運用メモへ反映する

### 5.8 追補: CloudWatch Alarm の構成は旧版方針を維持して確認する

- 実施日時: `2026-04-11 JST`
- 結果: `監査完了（アラーム未設定）`
- 実施方法:
  - AWS Web コンソール
  - `CloudWatch > アラーム`
  - `ap-northeast-1` と `us-east-1` の両リージョンで確認
- 確認結果:
  - `ap-northeast-1`: アラーム `0件`
  - `us-east-1`: アラーム `0件`
  - 旧版方針で想定していた `Elastic Beanstalk` / `RDS` / `CloudFront 5xx` の基本アラームは現時点で未設定
- 判定:
  - 「既存アラームを維持して確認する」という前提は未達
  - 監視基盤としては CloudWatch Logs 側の整備は進んだが、メトリクスアラームは今後の追加作業が必要
- 補足:
  - `SNS` 通知先の確認も、紐づくアラームが無いため今回は未確認
  - 本件は `残課題` として完了判定メモへ引き継ぐ

### 5.8 追補（2026-04-12 再実施）: CloudWatch Alarm を新規作成する

- 実施日時: `2026-04-12 JST`
- 結果: `完了`
- 実施方法:
  - AWS Web コンソール
  - `SNS` で通知先トピックを新規作成
  - `CloudWatch > アラーム` で `ap-northeast-1` / `us-east-1` に基本アラームを新規作成
- SNS 通知先:
  - `ap-northeast-1`: `taskflow-prd-alerts-apne1`
    - protocol: `Email`
    - endpoint: `whgd0765@gmail.com`
    - subscription status: `Confirmed`
  - `us-east-1`: `taskflow-prd-alerts-use1`
    - protocol: `Email`
    - endpoint: `whgd0765@gmail.com`
    - subscription status: `Confirmed`
- 作成したアラーム:
  - `ap-northeast-1`
    - `taskflow-prd-eb-environment-health`
      - metric: `AWS/ElasticBeanstalk / EnvironmentHealth`
      - condition: `EnvironmentHealth < 15`
      - action: `taskflow-prd-alerts-apne1`
    - `taskflow-prd-rds-cpu-high`
      - metric: `AWS/RDS / CPUUtilization`
      - condition: `CPUUtilization > 80`
      - action: `taskflow-prd-alerts-apne1`
    - `taskflow-prd-rds-storage-low`
      - metric: `AWS/RDS / FreeStorageSpace`
      - condition: `FreeStorageSpace < 5368709120`
      - action: `taskflow-prd-alerts-apne1`
    - `taskflow-prd-rds-memory-low`
      - metric: `AWS/RDS / FreeableMemory`
      - condition: `FreeableMemory < 134217728`
      - action: `taskflow-prd-alerts-apne1`
  - `us-east-1`
    - `taskflow-prd-cf-5xx-rate-high`
      - metric: `AWS/CloudFront / 5xxErrorRate`
      - distribution: `E688SH91TX10P`
      - condition: `5xxErrorRate > 1`
      - action: `taskflow-prd-alerts-use1`
- 判定:
  - 旧版方針で想定していた `Elastic Beanstalk` / `RDS` / `CloudFront 5xx` の基本アラーム整備を満たした
  - 2026-04-11 時点での `CloudWatch Alarm 未設定` は解消済み
- 補足:
  - しきい値一覧と通知先一覧は `notes/cloudwatch_alarm_thresholds.md` に整理した

### 5.9 追補: RDS PostgreSQL ログ出力と CloudTrail を確認する

- 実施日時: `2026-04-11 JST`
- 結果: `完了`
- RDS 側確認結果:
  - `RDS > taskflow-prd-rds > 変更 > ログのエクスポート` を確認
  - 初期状態では `iam-db-auth-error ログ`、`PostgreSQL ログ`、`アップグレードログ` のいずれも未チェック
  - `PostgreSQL ログ` を有効化して変更を適用
  - 反映後、`postgresql` が有効表示になり、RDS 用の CloudWatch Logs グループが作成されていることを確認
- 補足:
  - `iam-db-auth-error ログ` は IAM DB 認証が無効のため今回は未設定
  - `アップグレードログ` は常時監視目的では必須ではないため今回は未設定
  - 次に `CloudTrail` の multi-Region trail / S3 保存 / log file validation を確認する
- CloudTrail 側確認結果:
  - 初期状態では証跡 `0件`
  - 新規証跡 `taskflow-prd-trail` を作成
  - `マルチリージョンの証跡`: `はい`
  - `S3 バケット`: `aws-cloudtrail-logs-359429618625-0b416378`
  - `ログファイルの検証`: `有効`
  - `CloudWatch Logs` 連携: `未設定`
  - `ステータス`: `ログ記録`
- 判定:
  - 手順書で求める `RDS PostgreSQL logs exports` と `CloudTrail multi-Region trail + S3 保存 + log file validation` は満たした

### 5.10 追補: 初動メモと完了判定メモを更新する

- 実施日時: `2026-04-11 JST`
- 結果: `完了`
- 更新対象:
  - `notes/initial_response_memo.md`
  - `01_Phase6完了判定メモ.md`
- 反映内容:
  - `initial_response_memo.md` に `CloudWatch Logs / Logs Insights` を `eventId` 軸で先に確認する順序を追記
  - 保存済みクエリ `taskflow-prd-all-events`、`taskflow-prd-auth-failures`、`taskflow-prd-task-audit` の利用前提を追記
  - `/actuator/health` の `2xx` と `/api/auth-test/**` の `2xx` では `LOG-REQ-001` を出さないことを明記
  - `01_Phase6完了判定メモ.md` に `ログ設計反映済み` の追記、CloudWatch / CloudTrail / RDS logs export の結果を反映
  - その後 `2026-04-12` の追加対応で、同メモへ `CloudWatch Alarm + SNS` 整備済みの状態まで反映
- 判定:
  - 手順書 5.10 の要求である `運用メモ更新` と `完了判定メモ更新` を満たした

### 5.1 証跡フォルダと作業ログを作成する

- 実施日時: `2026-04-10 09:14 JST`
- 結果: `完了`
- 作成したフォルダ:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/screenshots`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/exports`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes`
- 作成したファイル:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/phase6_execution_log.md`
- メモ:
  - 手順書 3 章の固定値を転記
  - 参照資料として Phase4 完了判定メモ、Phase4 実行ログ、SSM policy JSON を列挙
  - 手順書の `db SG` は typo があるため、実値 `sg-06bfddec58a3b9cec` を採用して記録

### 5.2 作業前チェックを実施する

- 実施日時: `2026-04-10 09:15-09:18 JST`
- 結果: `完了`
- AWS CLI 実行主体:
  - `aws sts get-caller-identity`
  - `Arn=arn:aws:iam::359429618625:user/taskflow-cli-operator`
  - `Account=359429618625`
- リージョン:
  - `aws configure get region`
  - `ap-northeast-1`
- Elastic Beanstalk:
  - environment name: `Taskflow-prd-eb-app-env`
  - environment id: `e-gvcyudrrcs`
  - status: `Ready`
  - health: `Green`
  - health status: `Ok`
  - running version: `taskflow-prd-backend-20260409-1415-8d4d377`
- CloudFront:
  - distribution id: `E688SH91TX10P`
  - status: `Deployed`
  - domain: `d3jotedl3xn7u4.cloudfront.net`
  - default root object: `index.html`
- RDS:
  - instance id: `taskflow-prd-rds`
  - status: `available`
  - endpoint: `taskflow-prd-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com:5432`
  - backup retention period: `1`
  - latest restorable time: `2026-04-10T00:14:37+00:00`
- 判定:
  - CLI 実行主体、リージョン、EB、CloudFront、RDS の現在状態はすべて手順書の開始条件を満たす

### 5.3 Elastic Beanstalk の secret 参照設定を監査する

- 実施日時: `2026-04-10 09:19-09:20 JST`
- 結果: `完了`
- 実行コマンド:
  - `aws elasticbeanstalk describe-configuration-settings --application-name taskflow-prd-eb-app --environment-name Taskflow-prd-eb-app-env`
- 確認結果:
  - `DB_URL`
    - namespace: `aws:elasticbeanstalk:application:environmentsecrets`
    - value: `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_URL`
  - `DB_USERNAME`
    - namespace: `aws:elasticbeanstalk:application:environmentsecrets`
    - value: `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_USERNAME`
  - `DB_PASSWORD`
    - namespace: `aws:elasticbeanstalk:application:environmentsecrets`
    - value: `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_PASSWORD`
  - `JWT_SECRET`
    - namespace: `aws:elasticbeanstalk:application:environmentsecrets`
    - value: `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/JWT_SECRET`
  - `JWT_EXPIRATION_MILLIS`
    - namespace: `aws:elasticbeanstalk:application:environmentsecrets`
    - value: `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/JWT_EXPIRATION_MILLIS`
  - `CORS_ALLOWED_ORIGINS`
    - namespace: `aws:elasticbeanstalk:application:environmentsecrets`
    - value: `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/CORS_ALLOWED_ORIGINS`
- 補足:
  - 6 項目とも平文値ではなく ARN 参照で構成されていた
  - 今回は修正不要のため `アクション > アプリケーションサーバーを再起動` や `/actuator/health` 再確認は未実施

### 5.4 instance profile の Parameter Store 読み取り権限を監査する

- 実施日時: `2026-04-10 09:21-09:23 JST`
- 結果: `完了`
- 実行コマンド:
  - `aws iam list-policies --scope Local --query "Policies[?PolicyName=='taskflow-prd-eb-ssm-read-policy'].[Arn,DefaultVersionId]" --output text`
  - `aws iam get-policy-version --policy-arn arn:aws:iam::359429618625:policy/taskflow-prd-eb-ssm-read-policy --version-id v2`
  - `aws iam list-attached-role-policies --role-name aws-elasticbeanstalk-ec2-role`
- policy 基本情報:
  - policy arn: `arn:aws:iam::359429618625:policy/taskflow-prd-eb-ssm-read-policy`
  - default version: `v2`
- Document 確認:
  - statement action: `ssm:GetParameter` のみ
  - resource:
    - `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_URL`
    - `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_USERNAME`
    - `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_PASSWORD`
    - `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/JWT_SECRET`
    - `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/JWT_EXPIRATION_MILLIS`
    - `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/CORS_ALLOWED_ORIGINS`
- role attachment:
  - `aws-elasticbeanstalk-ec2-role` に `taskflow-prd-eb-ssm-read-policy` がアタッチ済み
- 補足:
  - 監査時点では 6 ARN 以外の Parameter Store resource は含まれていない
  - 今後 parameter を追加する場合は `Parameter Store でパラメータ作成 -> IAM policy に ARN 追加 -> EB 環境へ ARN 追加 -> アクション > アプリケーションサーバーを再起動` の順で実施する

### 5.5 S3 / CloudFront の公開設定を監査する

- 実施日時: `2026-04-10 09:24-09:26 JST`
- 結果: `完了`
- 実行コマンド:
  - `aws s3api get-public-access-block --bucket taskflow-prd-frontend-site-359429618625`
  - `aws s3api get-bucket-policy --bucket taskflow-prd-frontend-site-359429618625`
  - `aws s3api get-bucket-website --bucket taskflow-prd-frontend-site-359429618625`
  - `aws cloudfront get-distribution --id E688SH91TX10P`
- S3 bucket 監査結果:
  - bucket: `taskflow-prd-frontend-site-359429618625`
  - PublicAccessBlockConfiguration:
    - `BlockPublicAcls=true`
    - `IgnorePublicAcls=true`
    - `BlockPublicPolicy=true`
    - `RestrictPublicBuckets=true`
  - bucket policy:
    - principal: `cloudfront.amazonaws.com`
    - action: `s3:GetObject`
    - resource: `arn:aws:s3:::taskflow-prd-frontend-site-359429618625/*`
    - condition source arn: `arn:aws:cloudfront::359429618625:distribution/E688SH91TX10P`
  - website hosting:
    - `NoSuchWebsiteConfiguration`
    - 期待どおり static website hosting は無効
- CloudFront 監査結果:
  - distribution id: `E688SH91TX10P`
  - status: `Deployed`
  - domain: `d3jotedl3xn7u4.cloudfront.net`
  - default root object: `index.html`
  - origins:
    - S3 origin: `taskflow-prd-frontend-site-359429618625.s3.ap-northeast-1.amazonaws.com`
    - backend origin: `taskflow-prd-origin-backend -> taskflow-prd-eb-app-env.eba-8xzqpp6j.ap-northeast-1.elasticbeanstalk.com`
  - behaviors:
    - default behavior: S3 origin
    - `/api/*`: `taskflow-prd-origin-backend`
  - custom error responses:
    - `403 -> /index.html -> 200`
    - `404 -> /index.html -> 200`
- 未使用 OAC の扱い:
  - 監査時点では distribution が参照している S3 origin の OAC は `E34D1TMPWRPOLI` だった
  - その後、S3 origin の OAC を `taskflow-prd-oac-frontend / E26GNIW5QOQ2X5` へ付け替えた
  - 未使用になった自動生成 OAC `E34D1TMPWRPOLI` は削除済み

### 5.6 Network 公開設定を監査する

- 実施日時: `2026-04-10 09:27-09:29 JST`
- 結果: `監査完了（要対応事項あり）`
- 実行コマンド:
  - `aws ec2 describe-security-groups --group-ids sg-0e79604022597200d sg-06bfddec58a3b9cec`
  - `aws rds describe-db-instances --db-instance-identifier taskflow-prd-rds --query "DBInstances[0].[PubliclyAccessible,DBInstanceStatus,VpcSecurityGroups[0].VpcSecurityGroupId]" --output text`
- app SG:
  - sg: `taskflow-prd-sg-app / sg-0e79604022597200d`
  - inbound:
    - `tcp/80 -> prefix list pl-58a04531`
    - description: `cloudfront origin-facing only`
  - 判定:
    - `0.0.0.0/0` や一時検証用 IP の inbound は残っていない
- db SG:
  - sg: `taskflow-prd-sg-db / sg-06bfddec58a3b9cec`
  - inbound:
    - `tcp/5432 -> sg-0e79604022597200d`
    - `tcp/5432 -> sg-06db941d256dcaaa1`
  - 判定:
    - 手順書想定の `source=taskflow-prd-sg-app の 1 行のみ` ではなく、追加の security group source が残っている
- RDS:
  - instance: `taskflow-prd-rds`
  - status: `available`
  - publicly accessible: `false`
  - attached vpc sg: `sg-06bfddec58a3b9cec`
- 例外記録:
  - `sg-06db941d256dcaaa1` は将来の拡張機能追加対応で必要になる可能性を見込み、今回は削除しない判断とした
  - 現時点の判定としては、`db SG に source=app SG 以外の 5432 inbound がないこと` の厳密条件には未達
  - Phase6 完了判定では、この追加 SG を残した理由とリスクを明記したうえで `例外記録あり` として扱う

### 5.7 RDS バックアップと復旧導線を確認する

- 実施日時: `2026-04-10 09:37-09:39 JST`
- 結果: `完了`
- 実行コマンド:
  - `aws rds describe-db-instances --db-instance-identifier taskflow-prd-rds --query "DBInstances[0].[BackupRetentionPeriod,LatestRestorableTime,DBInstanceStatus]" --output text`
  - `aws rds describe-db-snapshots --db-instance-identifier taskflow-prd-rds --snapshot-type manual --query "DBSnapshots[].[DBSnapshotIdentifier,Status,SnapshotCreateTime]" --output text`
  - `aws rds create-db-snapshot --db-instance-identifier taskflow-prd-rds --db-snapshot-identifier taskflow-prd-rds-manual-20260410-phase6`
  - `aws rds wait db-snapshot-available --db-snapshot-identifier taskflow-prd-rds-manual-20260410-phase6`
  - `aws rds describe-db-snapshots --db-snapshot-identifier taskflow-prd-rds-manual-20260410-phase6 --query "DBSnapshots[0].[DBSnapshotIdentifier,Status,PercentProgress,SnapshotCreateTime]" --output text`
- バックアップ設定:
  - backup retention period: `1`
  - latest restorable time: `2026-04-10T00:29:37+00:00`
  - DB status: `available`
- manual snapshot:
  - snapshot id: `taskflow-prd-rds-manual-20260410-phase6`
  - final status: `available`
  - progress: `100`
  - snapshot create time: `2026-04-10T00:37:24.569000+00:00`
- 復旧メモ:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/rds_restore_memo.md` を作成
  - PITR は `RDS > データベース > taskflow-prd-rds > アクション > 特定時点への復元`
  - CLI は `aws rds restore-db-instance-to-point-in-time`
  - 復元時は新しい DB identifier を使い、既存本番 DB を直接上書きしない方針を明記
- 補足:
  - backup retention は `1` で 0 ではないため、手順書の最低条件は満たす
  - ただし推奨値 `7日以上` には未達のため、必要に応じて後続で見直し余地あり

### 5.8 「ヘルス」/「ログ」/ 初動メモを整備する

- 実施日時: `2026-04-10 09:49-09:50 JST`
- 結果: `完了`
- Web コンソール確認:
  - Elastic Beanstalk `Taskflow-prd-eb-app-env`
  - 「ヘルス」: `Green / Ok`
  - `ログをリクエスト`:
    - `最後の100行` を取得済み
    - type: `tail`
    - time: `2026-04-10 09:49:01.646 JST 頃`
    - instance: `i-04dc19e1314960747`
    - file name: `TailLogs-1775782141635.txt`
  - `ログ全体`:
    - type: `bundle`
    - time: `2026-04-10 09:50:11.531 JST 頃`
    - instance: `i-04dc19e1314960747`
- 初動メモ:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/initial_response_memo.md` を作成
  - 記載内容:
    - 症状確認
    - CloudFront 状態確認
    - EB の「ヘルス」/「イベント」/「ログ」確認
    - RDS status / LatestRestorableTime 確認
    - 復旧方式の分岐
- 補足:
  - 画像証跡から `bundle` / `tail` の取得時刻と instance id は確認できた
  - 共有された署名付き URL から `tail` ログの file name は `TailLogs-1775782141635.txt` と確認できた
  - 署名付き URL は一時的なアクセス情報を含むため、execution log には URL 全文を残さず file name のみ記録した
  - `bundle` 側の実ファイル名は未確認のため、種別・時刻・instance を記録値とした

### 5.9 DB_PASSWORD / JWT_SECRET のローテーション要否を判定し、必要時のみ実施する

- 実施日時: `2026-04-10 09:51-09:55 JST`
- 結果: `判定完了（要ローテーション・今回はスキップ）`
- 実施内容:
  - ローカル記録から `DB_PASSWORD` / `JWT_SECRET` の露出履歴を確認
  - Parameter Store の metadata を値を読まずに確認
- 確認コマンド:
  - `rg -n "DB_PASSWORD|JWT_SECRET" docs backend frontend -g '!**/node_modules/**'`
  - `aws ssm describe-parameters --parameter-filters Key=Path,Option=Recursive,Values=/taskflow/prd/backend --query "Parameters[?Name=='/taskflow/prd/backend/DB_PASSWORD' || Name=='/taskflow/prd/backend/JWT_SECRET'].[Name,Type,Version,LastModifiedDate]" --output table`
- 判定根拠:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase2/01_Phase2完了判定メモ.md` に
    - `作業途中で DB_PASSWORD と JWT_SECRET の復号値を画面表示しているため、現在の値をそのまま公開環境で使う場合はローテーションを推奨`
    と明記あり
  - Parameter metadata:
    - `/taskflow/prd/backend/DB_PASSWORD`
      - type: `SecureString`
      - version: `2`
      - last modified: `2026-04-09T10:30:13.626000+09:00`
    - `/taskflow/prd/backend/JWT_SECRET`
      - type: `SecureString`
      - version: `1`
      - last modified: `2026-04-09T10:19:00.870000+09:00`
  - 少なくとも `JWT_SECRET` にはローテーション済みの証跡が無い
- 判定:
  - 公開済み MVP 環境としては安全側に倒し、`DB_PASSWORD` と `JWT_SECRET` はローテーション要と判断する
- 今回の判断:
  - ローテーションは実施しない
- 未実施理由:
  - `DB_PASSWORD` 更新は本番 backend の再起動と接続確認が必要
  - `JWT_SECRET` 更新は既存セッション失効を伴う
  - 影響があるため、実施は別確認のうえで進める
  - Phase6 では監査と運用整理を優先し、ローテーションは後続タスクへ送る

### 5.10 taskflow-cli-operator の権限整理を実施する

- 実施日時: `2026-04-10 09:56-09:57 JST`
- 結果: `完了（パターンB: 例外記録あり）`
- 実行コマンド:
  - `aws iam list-attached-user-policies --user-name taskflow-cli-operator`
- 現在の attached policy:
  - `AdministratorAccess`
- 判断:
  - Phase6 では権限変更は実施しない
  - `AdministratorAccess` を一時的に維持し、例外記録を残す
- 理由:
  - 次フェーズでも AWS CLI 継続利用が見込まれる
  - 直ちに最小権限化すると作業停止や切り分けコスト増の可能性がある
- 例外記録:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/cli_user_exception_memo.md`
  - 期限: `2026-04-30`
  - 後続で customer managed policy への移行を検討する

### 6. Phase6 完了判定

- 実施日時: `2026-04-10 10:00-10:02 JST`
- 結果: `条件付きで Phase6 完了`
- 判定:
  - 公開設定監査、RDS backup / snapshot、EB health / logs、初動メモ整備までは完了
  - EB environment secrets は ARN 参照、instance profile 側 policy も 6 ARN に限定済み
  - app SG の `0.0.0.0/0` は残っておらず、frontend bucket も private 構成を維持
  - `taskflow-cli-operator` の権限整理は `例外記録あり` で締めた
- 条件付きとした理由:
  - `db SG` に `taskflow-prd-sg-app` 以外の `sg-06db941d256dcaaa1` が残っている
  - `DB_PASSWORD / JWT_SECRET` はローテーション要と判定したが、今回は未実施
  - `taskflow-cli-operator` は引き続き `AdministratorAccess` のまま
- 作成資料:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/01_Phase6完了判定メモ.md`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/rds_restore_memo.md`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/initial_response_memo.md`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/cli_user_exception_memo.md`
