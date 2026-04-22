# 5.6 公開 URL 正常系確認の失敗メモ

## 実施日時

- `2026-04-13 JST`

## 実施内容

- `Playwright + Chrome(headless)` で `https://d3jotedl3xn7u4.cloudfront.net/login` へ接続
- 公開 URL を入口に、`新規登録 -> ログイン -> タスク操作` の正常系確認を開始

## 確認できたこと

- ログイン画面自体は表示される
- `新規登録` 画面への遷移も可能

## 失敗したこと

- 新規登録フォーム送信後、画面は `/signup` に留まり `Network Error` を表示
- そのため `ログイン -> タスク一覧 -> 作成 -> 編集 -> 削除 -> ログアウト` の正常系は続行不可

## 追加で切り分けた結果

### 1. 公開 frontend の build 済み JS

- `https://d3jotedl3xn7u4.cloudfront.net/assets/index-CYVYHfzj.js` を確認
- bundle 内に `http://localhost:8080` が埋め込まれている
- つまり公開 frontend は API 接続先として `localhost` を参照している

### 2. CloudFront 経由の API 疎通

- `POST https://d3jotedl3xn7u4.cloudfront.net/api/auth/register`
- 応答: `HTTP/2 502 Bad Gateway`
- `x-cache: Error from cloudfront`

### 3. backend 再デプロイ後の追加確認

- frontend の配信自体は更新され、`index.html` は `index-ClH6bZjU.js` を返している
- ただし API は再デプロイ後も `HTTP/2 502 Bad Gateway` のまま
- `Elastic Beanstalk` の現行 `VersionLabel` は `taskflow-prd-backend-20260413-0142-cdd8bc6`
- `web.stdout.log` では再デプロイ後も `Tomcat started on port 8080 (http)` を出力している
- `nginx/error.log` では `127.0.0.1:5000` への `connect() failed (111: Connection refused)` が継続している
- `describe-configuration-settings` で確認できた環境変数は `SERVER_PORT=5000`
- つまり root cause は `backend workflow が Procfile に -Dserver.port=$PORT を書いており、EB 実環境の SERVER_PORT=5000 と不整合` である

### 4. 次の修正方針

- `.github/workflows/deploy-backend-prd.yml` の `Procfile` 生成行を `-Dserver.port=$SERVER_PORT` に修正する
- 参照用テンプレート `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6_5/workflow_templates/deploy-backend-prd.yml` も同じ内容にそろえる
- 修正後に `deploy-backend-prd -> deploy-frontend-prd` を再実行して `5.6` を再試験する

## 影響

- 公開 URL を入口にした MVP 正常系確認はこの状態では合格不可
- frontend 側の API 接続先設定と、CloudFront `/api/*` 経路の両方を見直す必要がある

## 証跡

- ログイン画面: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_01_login.png`
- 失敗画面: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_6_failure.png`
