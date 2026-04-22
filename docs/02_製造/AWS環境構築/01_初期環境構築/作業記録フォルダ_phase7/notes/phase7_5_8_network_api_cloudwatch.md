# 5.8 ネットワーク・API・CloudWatch Logs 確認メモ

## 実施日時

- `2026-04-13 JST`

## 結論

- `CloudFront` の公開配信は正常
- `API` は公開 URL 配下で `401 / 200` を期待どおり返却
- `CloudWatch Logs` で `requestId` ベースの到達確認ができる
- `Elastic Beanstalk` の `tail / bundle` ログ取得も可能

## 1. ネットワーク確認

- `GET https://d3jotedl3xn7u4.cloudfront.net/login`
  - `HTTP/2 200`
  - `content-type: text/html`
  - `cache-control: no-cache,no-store,must-revalidate`
  - `server: AmazonS3`
  - `x-cache: Miss from cloudfront`

証跡:

- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_00_login_headers.txt`

## 2. API 応答確認

### 未認証

- requestId: `phase7-5-8-unauth-1776048485`
- request:
  - `GET /api/tasks`
- response:
  - `HTTP/2 401`
  - `errorCode: ERR-AUTH-001`
  - `message: 認証が必要です`

証跡:

- `phase7_5_8_01_unauth_headers.txt`
- `phase7_5_8_01_unauth_body.json`

### 不正 token

- requestId: `phase7-5-8-invalid-1776048485`
- request:
  - `GET /api/tasks`
  - `Authorization: Bearer invalid-token`
- response:
  - `HTTP/2 401`
  - `errorCode: ERR-AUTH-003`
  - `message: トークンが不正です`

証跡:

- `phase7_5_8_02_invalid_token_headers.txt`
- `phase7_5_8_02_invalid_token_body.json`

### 認証成功

- requestId: `phase7-5-8-login-1776048485`
- request:
  - `POST /api/auth/login`
- response:
  - `HTTP/2 200`
  - `user.id: 26`

証跡:

- `phase7_5_8_03_login_headers.txt`
- `phase7_5_8_03_login_body.json`

### 認証済み一覧取得

- requestId: `phase7-5-8-tasks-1776048485`
- request:
  - `GET /api/tasks`
  - user: `phase7-a-1776047545162-qzt31a@example.com`
- response:
  - `HTTP/2 200`
  - body: `[]`

証跡:

- `phase7_5_8_04_tasks_ok_headers.txt`
- `phase7_5_8_04_tasks_ok_body.json`

## 3. Elastic Beanstalk 状態

- Environment: `Taskflow-prd-eb-app-env`
- Status: `Ready`
- Health: `Green`
- HealthStatus: `Ok`
- VersionLabel: `taskflow-prd-backend-20260413-0242-2bca460`

### 直近イベント

- `2026-04-13T02:43:18Z`
  - `Environment update completed successfully.`
- `2026-04-13T02:59:44Z`
  - `Pulled logs for environment instances.`
- `2026-04-13T03:00:24Z`
  - `Requested environment info from each instance.`

## 4. CloudWatch Logs 到達確認

対象ロググループ:

- `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/var/log/web.stdout.log`
- `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/environment-health.log`

### web.stdout.log

- `phase7-5-8-login-1776048485`
  - `LOG-AUTH-001`
  - `LOG-REQ-001`
- `phase7-5-8-invalid-1776048485`
  - `LOG-AUTH-003`
  - `LOG-REQ-001`
- `phase7-5-8-unauth-1776048485`
  - `LOG-AUTH-006`
  - `LOG-REQ-001`
- `phase7-5-8-tasks-1776048485`
  - `LOG-REQ-001`

補足:

- `401` 応答は API body の `requestId` と CloudWatch 上の `requestId` が一致
- 認証成功側でも `LOG-AUTH-001` と `LOG-REQ-001` を確認できた

### environment-health.log

- `2026-04-13T02:49:55Z`
  - `status=Ok`
  - `causes=100.0 % of the requests are erroring with HTTP 4xx. Insufficient request rate (6.0 requests/min) ...`
- `2026-04-13T02:50:05Z`
  - `status=Ok`
  - `causes=[]`

判断:

- 直前に `401` 系の意図的な検証リクエストを低頻度で実施したため、一時的に health cause へ反映された
- 10 秒後には `Ok / causes=[]` へ戻っており、継続障害ではない

## 5. Elastic Beanstalk ログ取得

### tail

- instance: `i-04dc19e1314960747`
- file name: `TailLogs-1776049179705.txt`
- 保存先:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_TailLogs-1776049179705.txt`

確認できたこと:

- `web.stdout.log` に `phase7-5-8-login / invalid / unauth / tasks` の requestId 行が含まれる
- `eb-engine.log` に直近 deploy 後の process 起動と nginx 起動が含まれる

### bundle

- instance: `i-04dc19e1314960747`
- file name: `BundleLogs-1776049220000.zip`
- 保存先:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_BundleLogs-1776049220000.zip`

zip 内の代表ファイル:

- `var/log/web.stdout.log`
- `var/log/eb-engine.log`
- `var/log/nginx/access.log`
- `var/log/nginx/error.log`
- `var/log/healthd/daemon.log`

## 注意

- `tail / bundle` の署名付き URL は一時アクセス情報を含むため、このメモには残さない
