# Phase4 完了判定メモ

## 結論

- 判定: `条件付きで Phase4 完了`
- 判定日: `2026年4月9日`

Phase4 の主目的だった `frontend を S3 + CloudFront で HTTPS 公開し、同一 CloudFront ドメイン配下の /api/* から backend API へ到達できる状態` は達成しています。`/login` 表示、認証、タスク CRUD、`CORS_ALLOWED_ORIGINS` 反映、`sg-app` の CloudFront prefix list 制限まで完了しており、Phase5 へ進める状態です。

一方で、CloudFront の S3 origin は手動作成した OAC ではなく自動生成 OAC を参照しており、`taskflow-prd-oac-frontend` は未使用のままです。また、手順書 18 章で想定していたスクリーンショット証跡の一部は `phase4` フォルダへ未集約です。動作上の blocker ではありませんが、運用整理と証跡整理の宿題として残します。

## 完了した内容

- `6` Phase4 用の証跡フォルダと実行ログを作成
- `7` private S3 bucket `taskflow-prd-frontend-site-359429618625` を作成
- `8` OAC `taskflow-prd-oac-frontend` を作成
- `9` CloudFront distribution `taskflow-prd-cf-web` を作成し、S3 origin / backend origin / `/api/*` behavior / `index.html` / SPA deep link 設定を反映
- `10` CloudFront 標準ドメインを frontend 公開 URL として確定し、`CORS_ALLOWED_ORIGINS` を登録
- `11` `frontend/.env.production` を CloudFront ドメインへ更新して本番 build を作成
- `12` build 成果物を S3 へ同期
- `13` CloudFront invalidation を実行
- `14` CloudFront ドメイン経由で `/login`、認証、タスク CRUD を確認し、CORS 設定不備を修正
- `15` `taskflow-prd-sg-app` の `HTTP/80` を CloudFront origin-facing managed prefix list に制限
- `16` `taskflow-cli-operator` の access key は Phase5 以降も使う前提で保持し、後で最小権限化する方針を記録

## 主要な作成済みリソース

### CloudFront / S3

- S3 bucket: `taskflow-prd-frontend-site-359429618625`
- CloudFront distribution name: `taskflow-prd-cf-web`
- Distribution ID: `E688SH91TX10P`
- Domain name: `d3jotedl3xn7u4.cloudfront.net`
- frontend 公開 URL: `https://d3jotedl3xn7u4.cloudfront.net`
- default origin: `taskflow-prd-frontend-site-359429618625.s3.ap-northeast-1.amazonaws.com`
- backend origin name: `taskflow-prd-origin-backend`
- backend origin domain: `Taskflow-prd-eb-app-env.eba-8xzqpp6j.ap-northeast-1.elasticbeanstalk.com`

### CloudFront 設定

- default root object: `index.html`
- custom error responses:
  - `403 -> /index.html -> 200`
  - `404 -> /index.html -> 200`
- `/api/*` behavior:
  - target origin: `taskflow-prd-origin-backend`
  - viewer protocol policy: `Redirect HTTP to HTTPS`
  - allowed methods: `GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE`
  - cache policy: `CachingDisabled`
  - origin request policy: `AllViewerExceptHostHeader`

### backend / CORS

- Parameter Store:
  - `/taskflow/prd/backend/CORS_ALLOWED_ORIGINS`
  - value: `https://d3jotedl3xn7u4.cloudfront.net`
- Elastic Beanstalk runtime env:
  - `CORS_ALLOWED_ORIGINS` を SSM 参照で追加済み
- IAM:
  - `taskflow-prd-eb-ssm-read-policy` に `CORS_ALLOWED_ORIGINS` の ARN を追加済み

### Security Group

- `taskflow-prd-sg-app / sg-0e79604022597200d`
  - `HTTP/80` source: `com.amazonaws.global.cloudfront.origin-facing / pl-58a04531`
  - `0.0.0.0/0` の暫定公開は削除済み

## 検証結果

- `GET https://d3jotedl3xn7u4.cloudfront.net/login` は `200`
- `/login` の直アクセスとリロードで SPA 表示を確認
- `POST /api/auth/register` は `201`
- `POST /api/auth/login` は `200`
- `POST /api/tasks` は `201`
- `PUT /api/tasks/{id}` は `200`
- `GET /api/tasks/{id}` は `200`
- `DELETE /api/tasks/{id}` は `204`
- browser-origin 相当の login request で
  - `Access-Control-Allow-Origin: https://d3jotedl3xn7u4.cloudfront.net`
  - `Access-Control-Allow-Credentials: true`
  を確認
- 実ブラウザでも `testUser2@example.com` のログイン成功を確認

## Phase4 の残課題

- CloudFront の S3 origin で使用中の OAC
  - 実際に使われているのは自動生成 OAC `E34D1TMPWRPOLI`
  - 手動作成した `taskflow-prd-oac-frontend / E26GNIW5QOQ2X5` は未使用
  - 動作影響はないが、後で統一する
- 証跡整理
  - 手順書 18 章で想定した CloudFront / SG / browser Network のスクリーンショットは `phase4/screenshots` に未集約
  - 実行ログと CLI 出力で代替できているが、必要なら後で補完する
- CLI user 権限
  - `taskflow-cli-operator` の access key は保持
  - Phase5 以降で必要操作を洗い出し、`AdministratorAccess` から最小権限へ縮小する

## Phase5 への引き継ぎ

- frontend と backend の初回公開経路は `https://d3jotedl3xn7u4.cloudfront.net` に統一済み
- API の公開入口は Elastic Beanstalk 直URLではなく `CloudFront /api/*` を前提にする
- `CORS_ALLOWED_ORIGINS` は登録済みだが、新しい parameter を増やすときは IAM policy 側の ARN 追加漏れに注意する
- backend 単体確認のために Elastic Beanstalk 直URLへ一時アクセスしたい場合は、`sg-app` が CloudFront prefix list 制限になっている点を意識する
- OAC の整理と CLI user の最小権限化は後続タスクとして扱う

## 参照資料

- `docs/02_製造/AWSデプロイ/作業手順_phase4.docx`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/phase4_execution_log.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/exports/taskflow-prd-eb-ssm-read-policy-with-cors.json`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/01_Phase3完了判定メモ.md`
