# Phase7 Execution Log

## 基本情報

- 作業日: `2026年4月13日`
- 作業者: `未記入`
- AWS アカウント: `359429618625`
- リージョン: `ap-northeast-1`
- 対象 Phase: `Phase7`
- 参照手順書: `docs/02_製造/AWSデプロイ/作業手順_phase7.docx`

## Phase7 で固定して使う値

| 項目 | 内容 |
| --- | --- |
| frontend 公開 URL | `https://d3jotedl3xn7u4.cloudfront.net` |
| CloudFront Distribution | `taskflow-prd-cf-web / E688SH91TX10P` |
| Elastic Beanstalk Application | `taskflow-prd-eb-app` |
| Elastic Beanstalk Environment | `Taskflow-prd-eb-app-env` |
| backend artifact バケット | `taskflow-prd-eb-artifacts-359429618625` |
| frontend 配備先バケット | `taskflow-prd-frontend-site-359429618625` |
| GitHub Environment | `production` |
| production 条件 | `Required reviewer 有効 / deploy branch は develop のみ` |
| deploy role | `taskflow-prd-github-actions-deploy-role` |

## 証跡フォルダ

- screenshots: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots`
- exports: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports`
- notes: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes`

## 参照資料

- `docs/02_製造/AWSデプロイ/作業手順_phase7.docx`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/phase6_execution_log.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/01_Phase6完了判定メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/initial_response_memo.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/rds_restore_memo.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6_5/01_phase6_5_execution_log.md`

## 実行ログ

### 5.1 証跡フォルダと作業ログを作成する

- 実施日時: `2026-04-13 JST`
- 結果: `完了`
- 対応内容:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/` を作成
  - `screenshots`、`exports`、`notes` の 3 サブフォルダを作成
  - `phase7_execution_log.md` を新規作成
  - 手順書 2 章の固定値を転記
- 補足:
  - 作業者名は未確定のため `未記入` とした
  - 後続の各項目結果はこの execution log へ追記していく

### 5.2 開始前チェックを実施する

- 実施日時: `2026-04-13 JST`
- 結果: `完了`
- 実施方法:
  - `aws sts get-caller-identity`
  - `aws cloudfront get-distribution --id E688SH91TX10P`
  - `aws elasticbeanstalk describe-environments --application-name taskflow-prd-eb-app --environment-names Taskflow-prd-eb-app-env`
  - `aws rds describe-db-instances --db-instance-identifier taskflow-prd-rds`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/01_Phase6完了判定メモ.md` を確認
- 確認結果:
  - AWS CLI identity:
    - Account: `359429618625`
    - Arn: `arn:aws:iam::359429618625:user/taskflow-cli-operator`
  - CloudFront Distribution `E688SH91TX10P`:
    - Status: `Deployed`
    - DomainName: `d3jotedl3xn7u4.cloudfront.net`
  - Elastic Beanstalk Environment `Taskflow-prd-eb-app-env`:
    - Status: `Ready`
    - Health: `Green`
    - HealthStatus: `Ok`
    - VersionLabel: `taskflow-prd-backend-20260412-0947-412b8c3`
  - RDS `taskflow-prd-rds`:
    - DBInstanceStatus: `available`
    - PubliclyAccessible: `false`
    - LatestRestorableTime: `2026-04-13T00:59:31+00:00`
  - Phase6 完了判定メモ:
    - 判定: `条件付きで Phase6 完了`
    - 追補反映日: `2026年4月12日`
    - 継続残課題: `db SG 例外 / secret ローテーション / CLI user 権限整理 / eventId 単位の監視`
- 補足:
  - 手順書どおり、Phase6 の残課題は存在しているが `悪化していないこと` を開始条件として扱う
  - 開始前チェック時点では Phase7 を進める阻害要因は見当たらない

### 5.3 MVP の検証対象と除外対象を固定する

- 実施日時: `2026-04-13 JST`
- 結果: `完了`
- 検証対象:
  - `新規登録`
  - `ログイン`
  - `ログアウト`
  - `セッション切れ導線`
  - `タスク一覧`
  - `フィルタ`
  - `詳細`
  - `作成`
  - `編集`
  - `削除`
  - `担当者候補取得`
  - `入力バリデーション`
  - `認可境界`
  - `監視確認`
- 除外対象:
  - `コメント`
  - `添付ファイル`
  - `チーム管理`
  - `通知`
  - `パスワードリセット`
  - `ヘルプ`
  - `管理者画面`
- ブラウザ確認時の追加観点:
  - 除外対象 UI が不用意に公開されていないことを確認対象に含める
  - `コメント / 添付 / チーム管理` はもともとの設計書記載があっても、今回の Phase7 合格条件には含めない
- 補足:
  - 以後の正常系・異常系試験はこの対象範囲を基準に記録する
  - 将来機能に関する差分を見つけても、Phase7 では `除外対象の露出有無` として扱う

### 5.4 GitHub Actions の最新結果と production 承認前提を確認する

- 実施日時: `2026-04-13 JST`
- 結果: `完了`
- 確認方法:
  - ローカル workflow 定義:
    - `.github/workflows/ci.yml`
    - `.github/workflows/deploy-backend-prd.yml`
    - `.github/workflows/deploy-frontend-prd.yml`
    - `.github/workflows/github_environment_variables_and_secrets.md`
  - GitHub API:
    - `GET /repos/kitune-udon/task-manager-app/environments/production`
    - `GET /repos/kitune-udon/task-manager-app/environments/production/deployment-branch-policies`
    - `GET /repos/kitune-udon/task-manager-app/actions/workflows/{workflow_id}/runs?branch=develop&per_page=5`
  - 参照資料:
    - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6_5/01_phase6_5_execution_log.md`
- production Environment 確認結果:
  - Environment 名: `production`
  - Required reviewer: `有効`
  - reviewer: `kitune-udon`
  - deployment branch policy: `custom_branch_policies=true`
  - 許可 branch: `develop`
  - `prevent_self_review`: `false`
- workflow 定義確認結果:
  - `ci` は `pull_request(develop/main)` と `push(develop)` で起動する
  - `deploy-backend-prd` と `deploy-frontend-prd` は `workflow_dispatch` で起動する
  - deploy 2 本はいずれも `environment: production` を参照する
- 最新 workflow 実行結果:
  - `ci`
    - run id: `24305395134`
    - run number: `5`
    - branch: `develop`
    - head sha: `1aa7f50d186a56fb13f516dc7e2e710a0c4cd4f8`
    - event: `push`
    - conclusion: `success`
    - created_at: `2026-04-12T11:14:17Z`
    - updated_at: `2026-04-12T11:15:27Z`
  - `deploy-backend-prd`
    - run id: `24303823548`
    - run number: `4`
    - branch: `develop`
    - head sha: `412b8c31cd04997cb085796621d2dfa1d20d0343`
    - event: `workflow_dispatch`
    - conclusion: `success`
    - created_at: `2026-04-12T09:46:34Z`
    - updated_at: `2026-04-12T09:49:00Z`
  - `deploy-frontend-prd`
    - run id: `24303967406`
    - run number: `1`
    - branch: `develop`
    - head sha: `412b8c31cd04997cb085796621d2dfa1d20d0343`
    - event: `workflow_dispatch`
    - conclusion: `success`
    - created_at: `2026-04-12T09:55:03Z`
    - updated_at: `2026-04-12T09:55:40Z`
- Phase6.5 参照結果:
  - 最終結果: `ci / deploy-backend-prd / deploy-frontend-prd 成功`
  - backend version label: `taskflow-prd-backend-20260412-0947-412b8c3`
  - frontend workflow では `S3 upload 成功 / CloudFront invalidation 成功` を確認済み
- 補足:
  - Phase7 の起点となる deploy 経路は `GitHub Actions` に一本化されている
  - `Required reviewer` と `develop` 制限は維持されている
  - `prevent_self_review` は現時点でも `false` のため、単独運用から戻す場合は次フェーズで再有効化要否を確認する

### 5.5 必要な場合のみ、GitHub Actions から再デプロイする

- 実施日時: `2026-04-13 JST`
- 結果: `スキップ（理由記録あり）`
- 判断材料:
  - 現在の `HEAD`: `1aa7f50d186a56fb13f516dc7e2e710a0c4cd4f8`
  - 本番 backend の現行 `VersionLabel`: `taskflow-prd-backend-20260412-0947-412b8c3`
  - `412b8c3..1aa7f50` の差分ファイル:
    - `.github/workflows/github_actions_deploy_role_policy.json`
    - `.github/workflows/github_actions_oidc_trust_policy.json`
    - `.github/workflows/github_environment_variables_and_secrets.md`
    - `.github/workflows/notes.md`
- スキップ理由:
  - 差分は `GitHub Actions` 周辺の補足ファイルのみで、`backend / frontend` の配備物やランタイム設定に影響しない
  - `deploy-backend-prd` / `deploy-frontend-prd` の最新成功 run がすでに存在し、Phase7 の確認対象バージョンは特定できている
  - この時点で再デプロイしても、公開アプリの挙動確認という Phase7 本来の目的に対する追加価値が小さい
- Phase7 試験対象として固定する版:
  - backend deploy run: `24303823548`
  - backend version label: `taskflow-prd-backend-20260412-0947-412b8c3`
  - frontend deploy run: `24303967406`
  - deploy 対象 commit: `412b8c31cd04997cb085796621d2dfa1d20d0343`
- 補足:
  - 以後に `backend / frontend / runtime 設定` へ影響する変更が入った場合は、`backend -> frontend` の順で再デプロイしてから Phase7 のブラウザ試験をやり直す

### 5.6 CloudFront 公開 URL で正常系ブラウザ確認を行う

- 実施日時: `2026-04-13 JST`
- 結果: `要修正（公開導線で正常系未完了）`
- 実施方法:
  - `Playwright + Chrome(headless)` で `https://d3jotedl3xn7u4.cloudfront.net/login` を起点に操作
  - 証跡スクリーンショット:
    - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_01_login.png`
    - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_failure.png`
- 実施結果:
  - `ログイン画面表示`: 確認済み
  - `新規登録画面遷移`: 確認済み
  - `新規登録送信`: `失敗`
    - `/signup` に留まり `Network Error` を表示
  - `ログイン / タスク一覧 / フィルタ / 作成 / 詳細 / 編集 / 削除 / ログアウト`: `未実施`
- 追加切り分け:
  - 公開 frontend の build 済み JS `index-CYVYHfzj.js` に `http://localhost:8080` が埋め込まれていることを確認
  - `POST https://d3jotedl3xn7u4.cloudfront.net/api/auth/register` は `HTTP/2 502 Bad Gateway`
- 判定:
  - 公開 URL を入口にした MVP 正常系はこの状態では成立しない
  - `frontend の API 接続先設定` と `CloudFront /api/* 経路` の両方を見直す必要がある
- 参照メモ:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes/phase7_5_6_public_flow_failure.md`

### 5.6 不具合修正対応メモ

- 実施日時: `2026-04-13 JST`
- 状況: `ローカル修正完了 / 再デプロイ待ち`
- 原因整理:
  - frontend:
    - `frontend/src/lib/apiClient.ts` が `VITE_API_BASE_URL` 未設定時に常に `http://localhost:8080` を採用していた
    - そのため公開 CloudFront 配下でも API 接続先が localhost に解決されうる状態だった
  - backend:
    - Elastic Beanstalk の nginx upstream は `127.0.0.1:5000` を参照していた
    - 再デプロイ後の `describe-configuration-settings` で `SERVER_PORT=5000` を確認した
    - 一方、`deploy-backend-prd.yml` の Procfile 生成は `-Dserver.port=$PORT` になっており、EB 実環境の変数名と不整合だった
    - `web.stdout.log` では再デプロイ後も `Tomcat started on port 8080 (http)` を出力していた
    - その結果、`/api/auth/register` が `502 Bad Gateway` になっていた
- 修正内容:
  - `frontend/src/lib/apiClient.ts`
    - `VITE_API_BASE_URL` 未設定時は、`localhost / 127.0.0.1` 以外の配信環境で `window.location.origin` を API base URL として採用するよう変更
    - 末尾スラッシュ除去処理を共通関数化
  - `backend/src/main/resources/application-dev.yml`
  - `backend/src/main/resources/application-local.yml`
  - `backend/src/main/resources/application-prod.yml`
    - `server.port` を `${PORT:${SERVER_PORT:8080}}` に変更し、ローカルとデプロイ環境の両方で待受ポート解決が崩れにくい形へ補強
  - `.github/workflows/deploy-backend-prd.yml`
    - Procfile 生成行を `-Dserver.port=$SERVER_PORT` に修正
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6_5/workflow_templates/deploy-backend-prd.yml`
    - 実運用 workflow と同じ内容に修正
- ローカル確認結果:
  - `backend`: `./gradlew test` 成功
  - `frontend`: `npm run build` 成功
- 再反映方針:
  - ユーザーが `develop` へ push を実施する
  - その後 `GitHub Actions` で `deploy-backend-prd` を `ref=develop` で実行
  - backend 完了後に `deploy-frontend-prd` を `ref=develop` で実行
  - 2 本の deploy 完了後、`5.6 CloudFront 公開 URL で正常系ブラウザ確認` を再実施する
- 補足:
  - workflow 定義上、`deploy-backend-prd` / `deploy-frontend-prd` はどちらも `workflow_dispatch` 起動であり、push のみでは本番反映されない

### 5.6 再実施結果

- 実施日時: `2026-04-13 JST`
- 結果: `完了`
- 再実施前の公開状態確認:
  - `POST https://d3jotedl3xn7u4.cloudfront.net/api/auth/register` が `HTTP/2 201` を返すことを確認
  - frontend `index.html` は `index-ClH6bZjU.js` を返していることを確認
  - Elastic Beanstalk Environment `Taskflow-prd-eb-app-env`:
    - Status: `Ready`
    - Health: `Green`
    - HealthStatus: `Ok`
    - VersionLabel: `taskflow-prd-backend-20260413-0155-2f4ddc6`
- 実施方法:
  - `Playwright + Chrome(headless)` で `https://d3jotedl3xn7u4.cloudfront.net/login` を起点に再試験
  - 一時ユーザーを新規登録し、`新規登録 -> ログイン -> タスク一覧 -> フィルタ -> 作成 -> 詳細 -> 編集 -> フィルタ再確認 -> 削除 -> ログアウト -> セッション切れ` を通し確認
- 確認結果:
  - `ログイン画面表示`: `確認済み`
  - `新規登録`: `成功`
  - `ログイン`: `成功`
  - `タスク一覧表示`: `成功`
  - `フィルタ(TODO / MEDIUM)`: `成功`
  - `タスク詳細表示`: `成功`
  - `タスク作成`: `成功`
  - `タスク編集(DOING / HIGH へ変更)`: `成功`
  - `フィルタ(DOING / HIGH)`: `成功`
  - `タスク削除`: `成功`
  - `ログアウト`: `成功`
  - `セッション切れ導線`: `成功`
  - 除外対象 UI 表示:
    - `コメント`: `非表示`
    - `添付ファイル`: `非表示`
    - `チーム管理`: `非表示`
    - `通知`: `非表示`
    - `パスワードリセット`: `非表示`
    - `ヘルプ`: `非表示`
    - `管理者画面`: `非表示`
- 証跡スクリーンショット:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_02_login_retest.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_03_signup_success.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_04_tasks_list.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_05_task_created.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_06_filter_todo_medium.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_07_task_detail.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_08_task_updated.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_09_filter_doing_high.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_10_task_deleted.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_11_logout.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_12_session_expired.png`
- 補足:
  - ブラウザ試験で作成した一時タスクは削除済み
  - 一時ユーザーは `phase7-1776045617450-nhokp5@example.com` で作成された

### 5.7 未認証・セッション切れ・認可境界を確認する

- 実施日時: `2026-04-13 JST`
- 結果: `要修正（認可境界の公開挙動に不整合）`
- 確認結果:
  - `未認証 direct URL`:
    - 未ログイン状態で `https://d3jotedl3xn7u4.cloudfront.net/tasks` を直接開くと `/login` へ誘導される
    - `GET /api/tasks` は `401 ERR-AUTH-001`
  - `session expired`:
    - token を `invalid-token` に差し替えて再読込すると `/login` へ戻る
    - その際の `GET /api/tasks` は `401 ERR-AUTH-003`
    - 画面には `認証期限が切れたため、再度ログインしてください。` を表示
  - `代表バリデーション`:
    - タスク作成画面の未入力送信で `入力内容を確認してください。` / `タイトルを入力してください。`
    - タイトル `101` 文字で `タイトルは100文字以内で入力してください。`
  - `認可境界（ユーザー B で他人タスク操作）`:
    - `GET /api/tasks/{id}`:
      - 想定: `403 ERR-AUTH-005`
      - 実際: `200` で `index.html` を返し、画面は権限エラーではなく空の詳細表示になる
    - `PUT /api/tasks/{id}`:
      - 想定: `403 ERR-TASK-005`
      - 実際: `200` で `index.html` を返し、画面は `タスクを更新しました。` と誤表示する
      - ただし creator 側で実データ確認するとタイトル / ステータス / 優先度は未変更
    - `DELETE /api/tasks/{id}`:
      - 想定: `403 ERR-TASK-006`
      - 実際: `200` で `index.html` を返し、画面は `タスクを削除しました。` と誤表示する
      - ただし creator 側で実データ確認すると対象タスクは残存
- 原因確認:
  - CloudFront distribution `E688SH91TX10P` は `/api/*` behavior を backend origin へ向けている
  - 一方で distribution 全体の `CustomErrorResponses` が
    - `403 -> /index.html -> 200`
    - `404 -> /index.html -> 200`
    になっており、backend の `403` が API でも `index.html` へ書き換えられる
- 参照メモ:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes/phase7_5_7_authz_boundary_issue.md`
- 証跡スクリーンショット:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_01_unauth_redirect.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_04_authz_detail_blank.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_05_authz_update_result.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_06_authz_delete_result.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_07_validation_required.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_08_validation_length.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_09_session_expired.png`
- 後処理:
  - 認可境界確認用の代表タスク `taskId=13` は削除済み
  - 途中切り分けで残った既知の一時タスク `8 / 9 / 10 / 11` も削除済み

### 5.7 不具合修正対応メモ

- 実施日時: `2026-04-13 JST`
- 状況: `修正反映完了 / 再試験完了`
- 原因整理:
  - CloudFront distribution `E688SH91TX10P` の `CustomErrorResponses` に
    - `403 -> /index.html -> 200`
    - `404 -> /index.html -> 200`
    が設定されていた
  - そのため `/api/*` behavior で backend origin が返した `403` / `404` も SPA 用 HTML へ書き換えられ、認可エラーが画面配信用 `index.html` として返却されていた
  - 結果として、他人タスク詳細は空表示、更新と削除は誤って成功メッセージを出す状態になっていた
- 恒久対策:
  - `frontend/cloudfront/spa-viewer-request.js`
    - `viewer-request` の CloudFront Function を追加し、`/api/*` と静的 asset を除く SPA 画面ルートのみ `/index.html` へ rewrite するようにした
  - `frontend/scripts/ensure-cloudfront-spa-routing.mjs`
    - CloudFront Function の作成または更新、`LIVE` publish、distribution への関連付け、既存 `403/404 -> /index.html` custom error response の除去を自動化した
  - `.github/workflows/deploy-frontend-prd.yml`
    - frontend deploy 時に上記 script を実行し、今後の再デプロイでも SPA ルーティング設定が維持されるようにした
  - `.github/workflows/github_actions_deploy_role_policy.json`
    - deploy role に `cloudfront:GetDistributionConfig` / `UpdateDistribution` / `CreateFunction` / `DescribeFunction` / `UpdateFunction` / `PublishFunction` を追加し、workflow から CloudFront 設定を更新できるようにした
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6_5/workflow_templates/*`
    - 実運用 workflow / policy と同じ内容に更新した
- 実環境反映:
  - AWS 上の inline policy `taskflow-prd-github-actions-deploy-policy` を最新化した
  - `CLOUDFRONT_DISTRIBUTION_ID=E688SH91TX10P` で `frontend/scripts/ensure-cloudfront-spa-routing.mjs` を実行し、production distribution に恒久対策を適用した
  - 適用結果:
    - CloudFront Function: `taskflow-prd-spa-router`
    - `removedCustomErrorResponses`: `403 / 404`
    - `viewerRequestRewriteEnabled`: `true`
- ローカル確認結果:
  - `node --check frontend/cloudfront/spa-viewer-request.js`: 成功
  - `node --check frontend/scripts/ensure-cloudfront-spa-routing.mjs`: 成功

### 5.7 再実施結果

- 実施日時: `2026-04-13 JST`
- 結果: `完了`
- 実施方法:
  - `Playwright + Chrome(headless)` で公開 URL を再試験
  - `ユーザー A` が作成したタスクに対して `ユーザー B` で詳細 / 更新 / 削除を試行し、API 応答と画面表示を突合
- 確認結果:
  - `未認証 direct URL`:
    - `/tasks` 直接アクセスで `/login` に遷移
    - `GET /api/tasks` は `401 ERR-AUTH-001`
  - `session expired`:
    - `GET /api/tasks` は `401 ERR-AUTH-003`
    - 最終画面は `/login`
  - `代表バリデーション`:
    - 未入力送信で `タイトルを入力してください。`
    - タイトル `101` 文字で `タイトルは100文字以内で入力してください。`
  - `認可境界（ユーザー B で他人タスク操作）`:
    - `GET /api/tasks/{id}` は `403 ERR-AUTH-005`
    - `PUT /api/tasks/{id}` は `403 ERR-TASK-005`
    - `DELETE /api/tasks/{id}` は `403 ERR-TASK-006`
    - creator 側で再確認し、対象タスクの `title / status / priority` は未変更であることを確認
- 証跡スクリーンショット:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_10_unauth_redirect_retest.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_11_user_a_tasks_retest.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_12_forbidden_detail_retest.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_13_forbidden_update_retest.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_14_forbidden_delete_retest.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_15_validation_required_retest.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_16_validation_length_retest.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_17_session_expired_retest.png`
- 後処理:
  - 再試験用の代表タスク `taskId=15` は試験中に削除済み
  - 念のため creator 権限で再削除を試行し、`404` で既に存在しないことを確認した

### 5.8 ネットワーク・API・CloudWatch Logs を確認する

- 実施日時: `2026-04-13 JST`
- 結果: `完了`
- ネットワーク確認:
  - `GET https://d3jotedl3xn7u4.cloudfront.net/login`
    - `HTTP/2 200`
    - `content-type: text/html`
    - `cache-control: no-cache,no-store,must-revalidate`
    - `server: AmazonS3`
    - `x-cache: Miss from cloudfront`
- API 応答確認:
  - `GET /api/tasks` 未認証
    - requestId: `phase7-5-8-unauth-1776048485`
    - `HTTP/2 401`
    - `errorCode: ERR-AUTH-001`
  - `GET /api/tasks` 不正 token
    - requestId: `phase7-5-8-invalid-1776048485`
    - `HTTP/2 401`
    - `errorCode: ERR-AUTH-003`
  - `POST /api/auth/login`
    - requestId: `phase7-5-8-login-1776048485`
    - `HTTP/2 200`
  - `GET /api/tasks` 認証済み
    - requestId: `phase7-5-8-tasks-1776048485`
    - `HTTP/2 200`
    - response body: `[]`
- Elastic Beanstalk 状態:
  - Environment: `Taskflow-prd-eb-app-env`
  - Status: `Ready`
  - Health: `Green`
  - HealthStatus: `Ok`
  - VersionLabel: `taskflow-prd-backend-20260413-0242-2bca460`
- CloudWatch Logs 確認:
  - 対象ロググループ:
    - `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/var/log/web.stdout.log`
    - `/aws/elasticbeanstalk/Taskflow-prd-eb-app-env/environment-health.log`
  - `web.stdout.log` で requestId 到達を確認:
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
  - `environment-health.log` では、意図的な `401` 検証直後の低リクエスト帯で
    - `2026-04-13T02:49:55Z`
    - `status=Ok`
    - `causes="100.0 % of the requests are erroring with HTTP 4xx. Insufficient request rate (6.0 requests/min) ..."`
    を確認
  - 同ログで
    - `2026-04-13T02:50:05Z`
    - `status=Ok`
    - `causes=[]`
    へ戻っており、一時的な確認負荷による表示と判断
- Elastic Beanstalk ログ取得:
  - `tail`:
    - instance: `i-04dc19e1314960747`
    - file name: `TailLogs-1776049179705.txt`
    - `exports/phase7_5_8_TailLogs-1776049179705.txt` に保存
  - `bundle`:
    - instance: `i-04dc19e1314960747`
    - file name: `BundleLogs-1776049220000.zip`
    - `exports/phase7_5_8_BundleLogs-1776049220000.zip` に保存
    - zip 内に `var/log/web.stdout.log` / `var/log/eb-engine.log` / `var/log/nginx/access.log` / `var/log/healthd/daemon.log` を確認
- 参照メモ:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes/phase7_5_8_network_api_cloudwatch.md`
- 証跡:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_00_login_headers.txt`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_01_unauth_headers.txt`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_01_unauth_body.json`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_02_invalid_token_headers.txt`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_02_invalid_token_body.json`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_03_login_headers.txt`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_03_login_body.json`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_04_tasks_ok_headers.txt`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_04_tasks_ok_body.json`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_TailLogs-1776049179705.txt`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/exports/phase7_5_8_BundleLogs-1776049220000.zip`
- 補足:
  - `tail / bundle` の署名付き URL は一時アクセス情報を含むため、execution log には全文を記録しない

### 5.9 監視・初動メモ・復旧導線をスポット確認する

- 実施日時: `2026-04-13 JST`
- 結果: `完了（要補足あり）`
- CloudWatch Alarm 確認:
  - `ap-northeast-1`
    - `taskflow-prd-eb-environment-health`
      - action: `taskflow-prd-alerts-apne1`
      - state: `ALARM`
    - `taskflow-prd-rds-cpu-high`
      - action: `taskflow-prd-alerts-apne1`
      - state: `OK`
    - `taskflow-prd-rds-storage-low`
      - action: `taskflow-prd-alerts-apne1`
      - state: `OK`
    - `taskflow-prd-rds-memory-low`
      - action: `taskflow-prd-alerts-apne1`
      - state: `OK`
  - `us-east-1`
    - `taskflow-prd-cf-5xx-rate-high`
      - action: `taskflow-prd-alerts-use1`
      - state: `OK`
- SNS 通知先確認:
  - `taskflow-prd-alerts-apne1`
    - protocol: `email`
    - endpoint: `whgd0765@gmail.com`
  - `taskflow-prd-alerts-use1`
    - protocol: `email`
    - endpoint: `whgd0765@gmail.com`
- 初動メモ確認:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/initial_response_memo.md`
  - 確認順が
    - `CloudFront`
    - `Elastic Beanstalk`
    - `CloudWatch Logs / Logs Insights`
    - `Elastic Beanstalk logs`
    - `RDS`
    の順で維持されていることを確認
- 復旧導線確認:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/notes/rds_restore_memo.md`
  - `PITR`
    - console: `RDS > データベース > taskflow-prd-rds > アクション > 特定時点への復元`
    - CLI: `aws rds restore-db-instance-to-point-in-time`
  - `snapshot restore`
    - console: `RDS > スナップショット > 対象 snapshot > スナップショットを復元`
    - CLI: `aws rds restore-db-instance-from-db-snapshot`
  - 注意事項として
    - 本番 DB を直接上書きしない
    - 新しい DB instance identifier を使う
    が明記されていることを確認
- 復旧判断に必要な現況:
  - `taskflow-prd-rds`
    - status: `available`
    - PubliclyAccessible: `false`
    - LatestRestorableTime: `2026-04-13T03:04:34+00:00`
  - manual snapshot:
    - `taskflow-prd-rds-manual-20260410-phase6`
    - status: `available`
    - created: `2026-04-10T00:37:24.569000+00:00`
- 要補足:
  - `taskflow-prd-eb-environment-health` は存在するが、spot check 時点で `ALARM`
  - 一方で `Elastic Beanstalk` 本体は
    - Status: `Ready`
    - Health: `Green`
    - HealthStatus: `Ok`
    だった
  - さらに `AWS/ElasticBeanstalk EnvironmentHealth` を `EnvironmentName=Taskflow-prd-eb-app-env` で確認すると、直近 20 分の datapoint がほぼ `0.0` で推移していた
  - 監視導線の所在確認は満たすが、`EB health alarm` は実状態との整合を別途見直す必要がある
- 参照メモ:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes/phase7_5_9_monitoring_spot_check.md`

## 6. 次フェーズへの引き継ぎ

- 実施日時: `2026-04-13 JST`
- 結果: `完了`
- 引き継ぎ整理:
  - `UI 不整合`
    - `public frontend が localhost へ API 接続しうる`
    - `backend Procfile の port 解決不整合`
    - いずれも `5.6` で修正済み
  - `認可不整合`
    - `CloudFront 403/404 -> /index.html` により `/api/*` の `403` が HTML 化される問題
    - `5.7` で CloudFront Function 方式へ切替済み
  - `ログ欠落`
    - Phase7 最終状態では新規の欠落は確認していない
    - `requestId / eventId` ベースの追跡、`tail / bundle` 取得も可能
  - `未解消の監視課題`
    - `taskflow-prd-eb-environment-health` が `EB Green/Ok` と不整合な `ALARM`
- 次フェーズへ渡す運用注意:
  - `GitHub Actions deploy role policy` を更新した場合は
    - `.github/workflows/github_actions_deploy_role_policy.json`
    - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6_5/workflow_templates/github_actions_deploy_role_policy.json`
    - AWS inline policy `taskflow-prd-github-actions-deploy-policy`
    を常に同じ内容へ揃える
  - production Environment の `prevent_self_review` は現時点で `false`
    - 初回検証後に再有効化要否を次フェーズで判断する
  - Phase6 残課題:
    - `db SG 例外`
    - `secret ローテーション`
    - `CLI user 権限整理`
    - `eventId 単位の監視`
    は継続管理する
  - 今後本番へ再反映が必要になった場合も、ローカル手動 deploy ではなく
    - `deploy-backend-prd`
    - `deploy-frontend-prd`
    の再実行後に再試験する
  - Phase7 で追加した恒久対策
    - `frontend/cloudfront/spa-viewer-request.js`
    - `frontend/scripts/ensure-cloudfront-spa-routing.mjs`
    - `.github/workflows/deploy-frontend-prd.yml`
    は次回 deploy でも維持する前提で扱う
- 引き継ぎメモ:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/02_Phase8引き継ぎメモ.md`
