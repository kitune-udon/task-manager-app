# Phase4 Execution Log

## 基本情報

- 作業日: 2026年4月9日
- 作業者: `未記入`
- AWS アカウント: `359429618625`
- リージョン: `ap-northeast-1`
- 対象 Phase: `Phase4`
- 参照手順書: `docs/02_製造/AWSデプロイ/作業手順_phase4.docx`

## Phase4 で固定して使う値

| 項目 | 内容 |
| --- | --- |
| AWS アカウント | `359429618625` |
| リージョン | `ap-northeast-1` |
| VPC | `taskflow-prd-vpc / vpc-0c923d9f3616e4f65` |
| public subnet | `subnet-0620ef37ddb586208 / subnet-02cc8b52e22d96aec` |
| private subnet | `subnet-0142da2df4d49ac04 / subnet-0e7ac4e833ccd98e9` |
| app SG | `taskflow-prd-sg-app / sg-0e79604022597200d` |
| db SG | `taskflow-prd-sg-db / sg-06bfddec58a3b9cec` |
| RDS | `taskflow-prd-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com:5432 / DB=taskflow` |
| Elastic Beanstalk Environment | `Taskflow-prd-eb-app-env / e-gvcyudrrcs` |
| Elastic Beanstalk CNAME | `Taskflow-prd-eb-app-env.eba-8xzqpp6j.ap-northeast-1.elasticbeanstalk.com` |
| Parameter Store prefix | `/taskflow/prd/backend/` |
| 追加済み parameter | `CORS_ALLOWED_ORIGINS` |
| frontend S3 bucket 候補名 | `taskflow-prd-frontend-site-359429618625` |
| OAC 名称候補 | `taskflow-prd-oac-frontend` |
| 公開方針 | `CloudFront 1本で frontend と /api/* を同一オリジン化` |
| 添付機能 | `初回公開では停止（UI 非表示を維持）` |

## 証跡フォルダ

- screenshots: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/screenshots`
- notes: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/notes`
- exports: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/exports`

## 実行ログ

### 5. 作業前チェック

- 実施日時: `2026-04-09 16:37-16:42 JST`
- 結果: `完了`
- git 状態:
  - branch: `develop`
  - commit: `8d4d377`
  - tracking: `origin/develop`
  - worktree は dirty
  - 補足: 既存の docs / deploy 系差分があり、Phase4 の blocker ではない
- Phase3 前提:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/01_Phase3完了判定メモ.md` で `条件付きで Phase3 完了` を確認
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/phase3_execution_log.md` で `/actuator/health`、`register`、`login`、`/api/users` の成功を確認
- frontend 設定:
  - `frontend/.env.production` は placeholder のまま
  - `VITE_API_BASE_URL=https://replace-me-with-backend-url.example.com`
- AWS CLI:
  - `aws sts get-caller-identity` 成功
  - `Arn=arn:aws:iam::359429618625:user/taskflow-cli-operator`
  - `aws configure get region` は `ap-northeast-1`
- 判定:
  - Phase4 開始前チェックは通過

### 6. 作業証跡フォルダを作成する

- 実施日時: `2026-04-09 16:42 JST`
- 結果: `完了`
- 作成したフォルダ:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/screenshots`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/exports`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/notes`
- 作成したファイル:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/phase4_execution_log.md`
- メモ:
  - 手順書 3 章の固定値を転記
  - 5 章の事前チェック結果も同ファイルに記録開始

### 7. frontend 用 S3 バケットを作成する

- 実施日時: `2026-04-09 16:44-16:47 JST`
- 結果: `完了`
- バケット名:
  - `taskflow-prd-frontend-site-359429618625`
- リージョン:
  - `ap-northeast-1`
- Block Public Access:
  - 4 項目すべて `有効`
- タグ:
  - `Project=taskflow`
  - `System=task-manager-app`
  - `Environment=prd`
  - `Scope=mvp`
  - `ManagedBy=manual`
- メモ:
  - 初回は private bucket のまま作成
  - S3 Website hosting は未設定のまま進行

### 8. CloudFront 用の OAC を作成する

- 実施日時: `2026-04-09 16:48 JST`
- 結果: `完了`
- OAC 名:
  - `taskflow-prd-oac-frontend`
- OAC ID:
  - `E26GNIW5QOQ2X5`
- メモ:
  - 手順書どおり OAC を事前作成

### 9. CloudFront ディストリビューションを作成する

- 実施日時: `2026-04-09 16:49-17:20 JST`
- 結果: `完了`
- Distribution 基本情報:
  - name: `taskflow-prd-cf-web`
  - distribution id: `E688SH91TX10P`
  - domain name: `d3jotedl3xn7u4.cloudfront.net`
  - status: `InProgress`
  - default root object: `index.html`
- Origin:
  - default origin:
    - `taskflow-prd-frontend-site-359429618625.s3.ap-northeast-1.amazonaws.com`
  - backend origin:
    - name: `taskflow-prd-origin-backend`
    - domain: `taskflow-prd-eb-app-env.eba-8xzqpp6j.ap-northeast-1.elasticbeanstalk.com`
    - protocol: `HTTP only`
- Behavior:
  - path pattern: `/api/*`
  - target origin: `taskflow-prd-origin-backend`
  - viewer protocol policy: `Redirect HTTP to HTTPS`
  - allowed methods: `GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE`
  - cache policy: `CachingDisabled`
  - origin request policy: `AllViewerExceptHostHeader`
- Custom error responses:
  - `403 -> /index.html -> 200`
  - `404 -> /index.html -> 200`
- 補足:
  - 実際に distribution が使用している S3 origin の OAC は `E34D1TMPWRPOLI`
  - 手動で作成した `taskflow-prd-oac-frontend / E26GNIW5QOQ2X5` は現時点では未使用
  - OAC の統一は後回しとし、まずは Phase4 の疎通完了を優先する

### 10. frontend の最終公開 URL を確定し、backend 側 CORS 設定を更新する

- 実施日時: `2026-04-09 18:04-18:06 JST`
- 結果: `完了`
- frontend 最終公開 URL:
  - `https://d3jotedl3xn7u4.cloudfront.net`
- Parameter Store:
  - name: `/taskflow/prd/backend/CORS_ALLOWED_ORIGINS`
  - type: `String`
  - value: `https://d3jotedl3xn7u4.cloudfront.net`
- Elastic Beanstalk 再読込:
  - 対象 environment: `Taskflow-prd-eb-app-env`
  - 実施内容: `アクション > アプリケーションサーバーを再起動`
  - environment status: `Ready`
  - health: `Green`
  - health status: `Ok`
- 主要 events:
  - `restartAppServer is starting.`
  - `Instance deployment completed successfully.`
  - `Restarted application server on all ec2 instances.`
  - `Environment health has transitioned from Info to Ok. Application restart completed 79 seconds ago and took 7 seconds.`
- メモ:
  - `CORS_ALLOWED_ORIGINS` の登録後、`アクション > アプリケーションサーバーを再起動` により再読込を実施
  - CloudFront distribution 側は引き続き `InProgress` の可能性があるが、CORS 設定更新作業自体は完了
  - 後続で Elastic Beanstalk runtime environment variables に `CORS_ALLOWED_ORIGINS` を SSM 参照として追加する必要があることが判明
  - 初回適用時は `taskflow-prd-eb-ssm-read-policy` に対象 ARN が含まれておらずロールバック
  - IAM policy へ `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/CORS_ALLOWED_ORIGINS` を追加後、再適用で解消

### 11. frontend の本番 build を作成する

- 実施日時: `2026-04-09 18:10-18:13 JST`
- 結果: `完了`
- `.env.production` 更新:
  - `VITE_API_BASE_URL=https://d3jotedl3xn7u4.cloudfront.net`
- 実行環境:
  - `node v22.14.0`
  - `npm 10.9.2`
- 実行コマンド:
  - `npm ci`
  - `npm run build`
- build 結果:
  - `vite v8.0.3 building client environment for production...`
  - `91 modules transformed`
  - `✓ built in 1.07s`
- 生成物:
  - `dist/index.html`
  - `dist/assets/index-DWgu-DFq.css`
  - `dist/assets/index-zUsEUVUe.js`
  - `dist/favicon.svg`
  - `dist/icons.svg`
- CloudFront ドメイン埋め込み確認:
  - `dist/assets/index-zUsEUVUe.js` 内に `d3jotedl3xn7u4.cloudfront.net` を確認
- メモ:
  - frontend build の API 接続先は CloudFront ドメインへ切替済み

### 12. build 成果物を S3 へ配置する

- 実施日時: `2026-04-09 18:15 JST`
- 結果: `完了`
- 実行コマンド:
  - `aws s3 sync frontend/dist s3://taskflow-prd-frontend-site-359429618625/ --delete`
- 同期結果:
  - `upload: frontend/dist/favicon.svg -> s3://taskflow-prd-frontend-site-359429618625/favicon.svg`
  - `upload: frontend/dist/icons.svg -> s3://taskflow-prd-frontend-site-359429618625/icons.svg`
  - `upload: frontend/dist/index.html -> s3://taskflow-prd-frontend-site-359429618625/index.html`
  - `upload: frontend/dist/assets/index-DWgu-DFq.css -> s3://taskflow-prd-frontend-site-359429618625/assets/index-DWgu-DFq.css`
  - `upload: frontend/dist/assets/index-zUsEUVUe.js -> s3://taskflow-prd-frontend-site-359429618625/assets/index-zUsEUVUe.js`
- S3 配置確認:
  - `index.html` がバケット直下に存在
  - `assets/index-DWgu-DFq.css` が存在
  - `assets/index-zUsEUVUe.js` が存在
  - `favicon.svg` / `icons.svg` も直下に存在
- Website hosting:
  - `NoSuchWebsiteConfiguration`
  - 期待どおり S3 Website hosting は未設定
- バケットポリシー:
  - CloudFront distribution `E688SH91TX10P` に対する `s3:GetObject` 許可を確認
  - resource: `arn:aws:s3:::taskflow-prd-frontend-site-359429618625/*`
- メモ:
  - private bucket + CloudFront OAC 前提で配置完了

### 13. CloudFront キャッシュを更新する

- 実施日時: `2026-04-09 18:17 JST`
- 結果: `完了`
- 実行コマンド:
  - `aws cloudfront create-invalidation --distribution-id E688SH91TX10P --paths '/*'`
- Invalidation:
  - id: `I257AMZBVIW262XZLQY0CRUHZI`
  - paths:
    - `/*`
  - final status: `Completed`
  - create time: `2026-04-09T09:17:24.070000+00:00`
- メモ:
  - frontend 配置後のキャッシュ更新を実施

### 14. frontend / API の公開確認を行う

- 実施日時: `2026-04-09 18:21-21:19 JST`
- 結果: `完了`
- CloudFront 公開 URL:
  - `https://d3jotedl3xn7u4.cloudfront.net`
- 初期表示確認:
  - `GET /login` は `HTTP 200`
  - `index.html` の配信と root 要素を確認
  - リロード時も `403/404` にはならず、SPA ルーティング設定が有効
- API 疎通確認:
  - `POST /api/auth/register` -> `201`
  - `POST /api/auth/login` -> `200`
  - `POST /api/tasks` -> `201`
  - `PUT /api/tasks/2` -> `200`
  - `GET /api/tasks/2` -> `200`
  - `DELETE /api/tasks/2` -> `204`
- ブラウザ確認:
  - シークレットウィンドウで `/login` を表示
  - `testUser2@example.com` でログイン成功
  - 初回は browser-origin 付き auth request が `403 Invalid CORS request` となりログイン失敗
  - Elastic Beanstalk runtime environment variables に `CORS_ALLOWED_ORIGINS` を追加し、IAM policy を更新して再適用後に解消
- CORS / Network 確認:
  - browser 相当ヘッダー付き `POST /api/auth/login` は `HTTP 200`
  - `Access-Control-Allow-Origin: https://d3jotedl3xn7u4.cloudfront.net`
  - `Access-Control-Allow-Credentials: true`
  - response body に `token` と `user` を確認
- 補足:
  - 初回の更新確認で `POST /api/tasks/1` を誤送信し、一時的に `500 / SYS-999` を記録
  - Elastic Beanstalk access log を確認した結果、実際の update endpoint 不具合ではなく HTTP method 指定ミスと判明
  - 正しい `PUT` で再試験したところ更新成功を確認
  - ブラウザ側で `register success` 後に user 未作成 / `ログイン時 token 取得失敗` の症状を確認
  - frontend の auth API レスポンス検証を追加し、期待 JSON 以外を成功扱いしないよう hotfix を反映
  - hotfix build で配信 asset が `index-Ch9uA5zZ.js` へ更新されたことを確認
  - CloudFront invalidation `I9R995SMCAEMM1786MIPVQH7B0` は `Completed`
  - 開発者ツールの直接確認は未実施だが、backend access log / browser-origin 付き curl / 実ブラウザログイン成功で同等の疎通確認を代替

### 15. sg-app の暫定公開を見直す

- 実施日時: `2026-04-09 22:04 JST`
- 結果: `完了`
- 対象 SG:
  - `taskflow-prd-sg-app / sg-0e79604022597200d`
- 変更前:
  - `HTTP/80 -> 0.0.0.0/0`
  - description: `temporary for phase3 backend verification`
- 変更後:
  - `HTTP/80 -> com.amazonaws.global.cloudfront.origin-facing`
  - prefix list id: `pl-58a04531`
  - description: `cloudfront origin-facing only`
- 確認結果:
  - `0.0.0.0/0` ルールは削除済み
  - CloudFront `/login` は引き続き表示可能
- メモ:
  - Phase3 の暫定公開を解消し、backend への 80/tcp は CloudFront origin-facing 経路に限定

### 16. Phase4 で不要になった認証情報を見直す

- 実施日時: `2026-04-09 22:15 JST`
- 結果: `完了`
- 対象:
  - `taskflow-cli-operator` の access key
- 判断:
  - `削除しない`
- 理由:
  - Phase5 以降も AWS CLI を継続利用する前提のため
  - ただし current permission は広く、恒久運用には不向き
- 後続対応:
  - Phase5 以降で必要な操作を洗い出したうえで最小権限へ縮小する
  - `AdministratorAccess` のまま固定しない

### 17. Phase4 完了判定

- 実施日時: `2026-04-09 22:20-22:23 JST`
- 結果: `条件付きで Phase4 完了`
- 判定:
  - CloudFront 標準ドメイン `https://d3jotedl3xn7u4.cloudfront.net` を frontend 公開 URL として確定
  - frontend build / S3 配置 / invalidation / `/api/*` behavior / SPA deep link 対応まで完了
  - CloudFront ドメイン経由で login / task CRUD / browser-origin CORS 応答を確認
  - `CORS_ALLOWED_ORIGINS` 登録、EB runtime env 反映、IAM policy 更新、app server 再読込まで完了
  - `sg-app` の `HTTP/80` は CloudFront origin-facing managed prefix list `pl-58a04531` に制限済み
  - `taskflow-cli-operator` access key は Phase5 以降も使う前提で保持し、後で最小権限化する方針を記録済み
- 残課題:
  - CloudFront の S3 origin は自動生成 OAC `E34D1TMPWRPOLI` を使用中で、手動作成した `taskflow-prd-oac-frontend / E26GNIW5QOQ2X5` は未使用
  - 手順書 18 章で想定したスクリーンショット証跡は `phase4/screenshots` に未集約
- 作成資料:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase4/01_Phase4完了判定メモ.md`
