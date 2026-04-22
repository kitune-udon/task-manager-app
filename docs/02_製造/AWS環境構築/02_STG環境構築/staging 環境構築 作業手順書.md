# staging 環境構築 作業手順書（改訂版）

## 1. 文書の目的

本手順書は、現在の AWS 環境を `production / master` 用環境として扱い、別途 `develop` 用の `staging` 環境を新規作成するための作業手順をまとめたものです。

この手順書では、以下をゴールとします。

- 既存の `taskflow-prd-*` を production 用として整理する
- `taskflow-stg-*` の staging 用 AWS リソースを新規作成する
- GitHub Actions から `develop` → staging、`master` → production に deploy できるようにする
- production と staging で、少なくとも DB / secret / 配信先 / deploy 先を分離する
- 非有識者でも、順番どおりに進めれば想定どおり作業できる状態にする
- staging 環境構築用の作業は `master` ブランチから切った専用作業ブランチで進め、完了後に `master` へマージできる状態にする

---

## 2. この手順書での前提

### 2.1 環境の役割

- `production` : `master` ブランチ用
- `staging` : `develop` ブランチ用

### 2.2 命名ルール

- production: `taskflow-prd-*`
- staging: `taskflow-stg-*`

### 2.3 共通利用するもの

以下は原則共通利用で進めます。

- VPC
- public subnet
- private subnet
- Internet Gateway
- Route Table
- NAT Gateway なし方針
- CloudTrail の基本方針

### 2.4 分離するもの

以下は production と staging で必ず分離します。

- GitHub Environment
- GitHub Actions 用 Variables
- GitHub Actions 用 IAM Role
- Parameter Store パス
- Elastic Beanstalk Environment
- frontend 配信用 S3 bucket
- CloudFront Distribution
- backend artifact bucket
- RDS
- CloudWatch Alarm
- SNS Topic（できれば分離）

---

## 3. 事前準備

### 3.1 作業に必要なもの

作業前に以下を準備してください。

- AWS コンソールへログインできること
- GitHub リポジトリの Settings を変更できること
- GitHub Actions を実行できること
- AWS CLI が使えること
- 対象リポジトリの `.github/workflows/` を編集できること
- ローカルで Git 操作ができること

### 3.2 作業前チェック

作業開始前に以下を確認してください。

#### 3.2.1 AWS アカウント確認

ターミナルで以下を実行します。

```bash
aws sts get-caller-identity
```

確認ポイント:

- 意図した AWS アカウント ID であること
- 誤った AWS アカウントで作業していないこと

#### 3.2.2 production 環境の現状確認

以下を確認します。

- CloudFront Distribution が存在すること
- Elastic Beanstalk Environment が `Ready / Green / Ok` であること
- RDS が `available` であること
- production 用 GitHub Environment が存在すること

#### 3.2.3 設定値を控える

以下をメモに控えてください。

- production の CloudFront Distribution ID
- production の frontend bucket 名
- production の backend artifact bucket 名
- production の Elastic Beanstalk Application 名
- production の Elastic Beanstalk Environment 名
- production の deploy role ARN
- production の Parameter Store prefix
- production で使っている instance profile 名
- production で使っている Elastic Beanstalk service role 名

---

## 4. 全体の作業順序

作業は必ず次の順で進めてください。

1. `master` ブランチを最新化し、staging 環境構築用の作業ブランチを作成する
2. production の GitHub Environment を master 用に見直す
3. staging 用 GitHub Environment を作成する
4. staging 用 workflow を用意する
5. staging 用 IAM Role / Policy / OIDC trust を作成する
6. staging 用 Elastic Beanstalk instance profile / service role を確認する
7. staging 用 Parameter Store を作成する
8. staging 用 backend artifact bucket を作成する
9. staging 用 RDS を作成する
10. staging 用 Elastic Beanstalk Environment を作成する
11. staging 用 frontend bucket を作成する
12. staging 用 CloudFront Distribution を作成する
13. staging 用 CloudWatch / SNS を整備する
14. staging へ deploy してスモークテストする
15. staging の総合確認を行う
16. staging / production の運用ルールを確定する
17. 作業内容を `master` へマージする

---

## 5. `master` ブランチを最新化し、staging 環境構築用の作業ブランチを作成する

### 5.1 目的
staging 環境構築に必要なコード・workflow・ドキュメント変更を、直接 `master` で作業せず、専用の作業ブランチで安全に進めます。

### 5.2 基本方針
- 作業開始時点の基準ブランチは `master` とする
- staging 環境構築の変更は、`master` から切った専用ブランチで実施する
- 作業完了後は Pull Request を作成し、レビュー後に `master` へマージする

### 5.3 ローカルでの作業手順
作業用 PC のターミナルで、リポジトリのルートへ移動してから次を実行します。

```bash
git checkout master
git pull origin master
git checkout -b chore/staging-environment-setup
```

ブランチ名の考え方:
- `chore/staging-environment-setup`
- `infra/staging-environment-setup`
- `feature/staging-environment`

いずれでもよいですが、後から見て「staging 環境構築用のブランチ」と分かる名前にしてください。

### 5.4 作業開始前の確認
以下を確認してください。

- `git branch --show-current` の結果が、作成した作業ブランチ名になっている
- `master` のまま作業していない
- `git status` がクリーンである

### 5.5 このブランチで行う作業
この手順書の以降の変更は、すべてこの作業ブランチ上で進めます。

対象例:
- `.github/workflows/` の追加・修正
- policy / trust policy / template の追加・修正
- staging 環境向けドキュメント整備
- README や設計書の更新

### 5.6 作業完了条件
- `master` を最新化できている
- staging 環境構築用の作業ブランチを作成できている
- 作業ブランチ上で変更を進める前提が確認できている

---

## 6. production 側の GitHub Environment を master 用に見直す

### 5.1 目的

現在 `develop` 向けになっている `production` Environment を、今後は `master` 用として扱える状態へ変更します。

### 5.2 GitHub 画面で開く場所

GitHub リポジトリを開き、次を選択します。

- `Settings`
- `Environments`
- `production`

### 5.3 確認・変更する項目

#### 5.3.1 Required reviewers

- `Required reviewers` が有効になっているか確認する
- 必要に応じて reviewer を設定する

#### 5.3.2 Deployment branches

- `Deployment branches` を開く
- 現在 `develop` になっている場合は、`master` に変更する

確認ポイント:

- `production` に対して `develop` では deploy できない状態になること
- 今後 `master` のみが production deploy 対象になること

#### 5.3.3 Variables

`production` Environment に登録されている Variables を確認します。

主な確認対象:

- `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN`
- `AWS_REGION`
- `EB_APPLICATION_NAME`
- `EB_ENVIRONMENT_NAME`
- `EB_S3_BUCKET`
- `VERSION_LABEL_PREFIX`
- `BACKEND_JAR_NAME`
- `FRONTEND_BUCKET`
- `CLOUDFRONT_DISTRIBUTION_ID`

ここでは値を変更しません。現状のまま production 用として維持します。

### 5.4 作業完了条件

- `production` Environment の deploy 対象ブランチが `master` になっている
- production 用 Variables が残っている
- Required reviewer 設定が確認できている

---

## 7. staging 用 GitHub Environment を作成する

### 6.1 目的

`develop` から staging へ deploy するための GitHub Environment を新規作成します。

### 6.2 作成手順

GitHub リポジトリで次を開きます。

- `Settings`
- `Environments`
- `New environment`

入力値:

- 名前: `staging`

### 6.3 設定する内容

#### 6.3.1 Required reviewers

- 必要であれば有効化する
- 開発運用のしやすさを優先するなら、最初は簡易設定でもよい

#### 6.3.2 Deployment branches

- `develop` のみを許可する

#### 6.3.3 Variables を登録する

staging 用の Variables を登録します。

登録対象の例:

- `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN`
- `AWS_REGION`
- `EB_APPLICATION_NAME`
- `EB_ENVIRONMENT_NAME`
- `EB_S3_BUCKET`
- `VERSION_LABEL_PREFIX`
- `BACKEND_JAR_NAME`
- `FRONTEND_BUCKET`
- `CLOUDFRONT_DISTRIBUTION_ID`

この時点では未作成の AWS リソースもあるため、後から値を埋める項目があってよいです。

### 6.4 作業完了条件

- `staging` Environment が作成されている
- `develop` のみ deploy 対象になっている
- Variables の登録先が用意できている

---

## 8. staging 用 workflow を作成する

### 7.1 目的

GitHub Actions から staging へ deploy できるようにします。

### 7.2 作成対象ファイル

以下を新規作成または複製します。

- `.github/workflows/deploy-backend-stg.yml`
- `.github/workflows/deploy-frontend-stg.yml`

### 7.3 複製元

production 用の以下を複製元にします。

- `.github/workflows/deploy-backend-prd.yml`
- `.github/workflows/deploy-frontend-prd.yml`

### 7.4 変更する内容

#### 7.4.1 Environment 名

- `environment: production` → `environment: staging`

#### 7.4.2 表示名

GitHub Actions 上で分かりやすい名前にします。

例:

- `deploy backend to staging`
- `deploy frontend to staging`

#### 7.4.3 CloudFront Function 更新処理

frontend deploy 側に、production で使っている CloudFront Function の適用処理があれば、そのまま staging 側にも含めてください。

#### 7.4.4 policy / template / 実体の同期ルール

staging 用 workflow を作るときは、以下の 3 か所をズレたままにしないでください。

- リポジトリ内の template / policy ファイル
- 実際に使う workflow ファイル
- AWS 上の inline policy / trust policy

### 7.5 作業完了条件

- staging 用 backend / frontend workflow が存在する
- `environment: staging` を参照する
- production 用 workflow を壊していない

---

## 9. staging 用 IAM Role / OIDC trust / Policy を作成する

### 8.1 目的

GitHub Actions が OIDC 経由で staging 用 AWS リソースへ deploy できるようにします。

### 8.2 作成対象

- staging 用 IAM Role（GitHub Actions OIDC 用）
- staging 用 trust policy
- staging 用 inline policy

### 8.3 IAM Role の作成

AWS コンソールで次を開きます。

- `IAM`
- `Roles`
- `Create role`

選択:

- Trusted entity type: `Web identity`
- Identity provider: `token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

Role 名の例:

- `taskflow-stg-github-actions-deploy-role`

### 8.4 trust policy の設定

trust policy では、GitHub リポジトリと `environment:staging` を条件にします。

考え方:

- production ではなく staging を許可する
- リポジトリ名を限定する

### 8.5 inline policy の設定

staging 用リソースに対して必要な権限を付与します。

少なくとも必要になる対象:

- Elastic Beanstalk
- S3（artifact bucket / frontend bucket）
- CloudFront
- CloudWatch Logs
- Auto Scaling 参照
- CloudFormation 参照
- 必要に応じて CloudFront Function 更新

重要:

- production 用 policy をそのまま使うのではなく、staging 用 bucket 名や resource 名に置き換える
- policy の複製元・リポジトリ内テンプレート・AWS 実体を揃える

### 8.6 GitHub Environment へ ARN を登録

staging Environment の Variables に以下を登録します。

- `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN`

### 8.7 作業完了条件

- staging 用 IAM Role が作成されている
- trust policy が `environment:staging` 前提になっている
- deploy role ARN が staging Environment に登録されている

---

## 10. staging 用 Elastic Beanstalk instance profile / service role を確認する

### 9.1 目的

Elastic Beanstalk 環境が staging 用 Parameter Store を参照し、health / logs の基本動作を行えるようにします。

### 9.2 instance profile で確認すること

staging 用 Elastic Beanstalk EC2 が Parameter Store を参照できるよう、instance profile 側の権限を確認します。

確認すること:

- EC2 instance profile が存在すること
- Systems Manager Parameter Store の参照権限があること
- 少なくとも staging 用 `/taskflow/stg/backend/*` を参照できること

重要:

- GitHub Actions 用 deploy role と、Elastic Beanstalk EC2 が使う instance profile は別物です
- deploy role だけ整備しても、EC2 側に Parameter Store 参照権限が無いと起動後に secret を読めません

### 9.3 service role で確認すること

staging 用 Elastic Beanstalk 環境で、health / managed action / ログ関連の基本動作に必要な service role を確認します。

確認すること:

- Elastic Beanstalk service role が存在すること
- staging 環境作成時にその role を指定できること

### 9.4 作業完了条件

- staging 用 instance profile が Parameter Store を参照できる
- staging 用 service role が確認できている

---

## 11. staging 用 Parameter Store を作成する

### 10.1 目的

staging 用 backend が参照する secret / 設定値を production と分離して管理します。

### 10.2 開く場所

AWS コンソールで次を開きます。

- `Systems Manager`
- `Parameter Store`

### 10.3 作成するパス

prefix は次を使います。

- `/taskflow/stg/backend/`

作成対象例:

- `/taskflow/stg/backend/DB_URL`
- `/taskflow/stg/backend/DB_USERNAME`
- `/taskflow/stg/backend/DB_PASSWORD`
- `/taskflow/stg/backend/JWT_SECRET`
- `/taskflow/stg/backend/JWT_EXPIRATION_MILLIS`
- `/taskflow/stg/backend/CORS_ALLOWED_ORIGINS`

### 10.4 作成時の注意

- `DB_PASSWORD` と `JWT_SECRET` は `SecureString` にする
- production の値をそのまま使わない
- `CORS_ALLOWED_ORIGINS` には staging の公開 URL を使う

### 10.5 作業完了条件

- staging 用 Parameter Store パスが作成されている
- production と異なる secret が設定されている

---

## 12. staging 用 backend artifact bucket を作成する

### 11.1 目的

backend deploy 用の source bundle 保存先を staging 用に分離します。

### 11.2 作成するバケット名の例

- `taskflow-stg-eb-artifacts-<account-id>`

### 11.3 AWS コンソールでの作成手順

AWS コンソールで次を開きます。

- `S3`
- `Create bucket`

入力値:

- bucket name: staging 用の名称
- region: `ap-northeast-1`

設定:

- Block Public Access: 有効のまま
- Versioning: 必要に応じて有効

### 11.4 staging Environment Variables へ反映

GitHub の `staging` Environment に以下を登録します。

- `EB_S3_BUCKET`

### 11.5 作業完了条件

- staging 用 artifact bucket が作成されている
- GitHub staging Environment に bucket 名が登録されている

---

## 13. staging 用 RDS を作成する

### 12.1 目的

staging 用 DB を production と分離して作成します。

### 12.2 作成前に確認すること

- 既存 VPC を共有利用するか決める
- private subnet を利用できるか確認する
- staging 用 DB security group を用意する

### 12.3 作成するリソース例

- DB instance: `taskflow-stg-rds`
- DB subnet group: `taskflow-stg-rds-subnet`
- app SG: `taskflow-stg-sg-app`
- db SG: `taskflow-stg-sg-db`

### 12.4 Security Group を作成・確認する

staging 用 DB は production と分離した Security Group で扱います。

作成または確認するもの:

- `taskflow-stg-sg-app`
- `taskflow-stg-sg-db`

設定ルール:

- `taskflow-stg-sg-db` の inbound は **taskflow-stg-sg-app**** からの 5432 のみ** 許可する
- 不要な source security group や `0.0.0.0/0` を設定しない
- production 用 SG をそのまま使い回さない

### 12.5 作成手順の考え方

production と同じ構成をベースに、次を守ります。

- PostgreSQL を選ぶ
- private subnet に置く
- Public access を `No` にする
- `taskflow-stg-sg-db` は `taskflow-stg-sg-app` からの 5432 のみ許可する
- production 用 DB は使わない

### 12.6 作成後にやること

- endpoint を確認する
- Parameter Store の `DB_URL` を staging 用値に更新する
- `DB_USERNAME` と `DB_PASSWORD` を staging 用値で登録する

### 12.7 作業完了条件

- staging 用 RDS が `available` になっている
- Public access = `No`
- staging 用 DB 情報が Parameter Store に反映されている

---

## 14. staging 用 Elastic Beanstalk Environment を作成する

### 13.1 目的

staging 用 backend 実行環境を作成します。

### 13.2 作成するリソース例

- Application: `taskflow-stg-eb-app`
- Environment: `Taskflow-stg-eb-app-env`

### 13.3 作成手順

AWS コンソールで次を開きます。

- `Elastic Beanstalk`
- `Create application`
- `Create environment`

選択:

- platform: production と同じ Java SE 系
- environment type: 単一構成から開始でも可

設定ポイント:

- staging 用 instance profile を指定する
- staging 用 service role を指定する
- staging 用 security group を設定する
- staging 用 Parameter Store を参照する環境変数を設定する
- RDS は production を参照しないこと

#### 13.3.1 instance profile の確認ポイント

- EC2 に割り当てる profile が staging 用 Parameter Store を読めること
- SSM Parameter 参照権限不足がないこと

#### 13.3.2 service role の確認ポイント

- Elastic Beanstalk 側の health / logs / managed action の基本動作に必要な role になっていること

### 13.4 反映する Variables

GitHub staging Environment に以下を登録します。

- `EB_APPLICATION_NAME`
- `EB_ENVIRONMENT_NAME`
- `VERSION_LABEL_PREFIX`
- `BACKEND_JAR_NAME`

### 13.5 初回 backend deploy 後に必ず確認すること

staging 用 Elastic Beanstalk Environment を作成しただけでは完了にしません。初回 backend deploy 後に次を確認してください。

確認項目:

- アプリケーションが起動していること
- `Health = Green / Ok` を確認できること
- Flyway migration が失敗していないこと
- `web.stdout.log` や `eb-engine.log` に致命的エラーが出ていないこと
- DB 接続エラーが出ていないこと

確認方法の例:

- Elastic Beanstalk Environment の health を見る
- CloudWatch Logs の `web.stdout.log` を確認する
- 必要に応じて `tail` / `bundle` ログを取得する

### 13.6 作業完了条件

- staging 用 Elastic Beanstalk Environment が作成されている
- health が確認できる
- staging 用 backend workflow の deploy 先に使える
- 初回 backend deploy 後に Flyway と起動確認が完了している

---

## 15. staging 用 frontend bucket を作成する

### 14.1 目的

staging 用 frontend の配信先を production と分離します。

### 14.2 作成するバケット名の例

- `taskflow-stg-frontend-site-<account-id>`

### 14.3 作成手順

AWS コンソールで次を開きます。

- `S3`
- `Create bucket`

設定:

- region: `ap-northeast-1`
- production 用 bucket を使い回さない

公開制御の考え方:

- frontend bucket を直接公開しない
- CloudFront 経由でのみ配信する
- Block Public Access は原則有効のまま運用する
- CloudFront からの参照に必要な bucket policy を設定する

### 14.4 CloudFront 参照用の bucket policy を設定する

staging 用 CloudFront Distribution を作成した後、その Distribution からのみ frontend bucket を参照できるように bucket policy を設定します。

確認ポイント:

- production 用 bucket policy を誤って編集していないこと
- staging 用 bucket に staging 用 policy が設定されていること

### 14.5 GitHub staging Environment へ反映

登録する変数:

- `FRONTEND_BUCKET`

### 14.6 作業完了条件

- staging 用 frontend bucket が作成されている
- GitHub staging Environment に登録済み
- Block Public Access と bucket policy の方針が確認できている

---

## 16. staging 用 CloudFront Distribution を作成する

### 15.1 目的

staging 用の公開 URL を production と分離します。

### 15.2 origin 構成

少なくとも以下を設定します。

- origin 1: staging frontend bucket
- origin 2: staging Elastic Beanstalk

behavior:

- `/api/*` → staging backend origin
- それ以外 → staging frontend origin

### 15.3 CloudFront 作成時に確認する項目

最低限、以下を確認してください。

- frontend bucket 参照に OAC を使うか方針を決める
- frontend bucket policy が CloudFront 経由の参照に合っている
- Default root object が適切に設定されている
- Viewer protocol policy が HTTPS 前提になっている
- `/api/*` behavior が staging backend origin を向いている
- キャッシュ設定が production と大きく乖離していない

### 15.4 注意点

- production の Distribution を流用しない
- staging 専用 URL を払い出す
- SPA ルーティング用 CloudFront Function を production と同じ考え方で設定する
- `/api/*` に対して HTML rewrite しないこと

### 15.5 GitHub staging Environment へ反映

登録する変数:

- `CLOUDFRONT_DISTRIBUTION_ID`

### 15.6 Parameter Store へ反映

staging 公開 URL が確定したら、以下を更新します。

- `/taskflow/stg/backend/CORS_ALLOWED_ORIGINS`

### 15.7 CORS 更新後に backend へ再反映する

重要:

- Parameter Store の値を更新しただけでは、既に起動している backend に即時反映されません
- `CORS_ALLOWED_ORIGINS` を更新した後は、staging backend を再反映してください

実施例:

- `deploy-backend-stg` を再実行する
- または Elastic Beanstalk 側の環境更新を行う

確認ポイント:

- 更新後に staging の CloudFront URL から API 呼び出しが CORS エラーなく通ること

### 15.8 作業完了条件

- staging 用 CloudFront Distribution が作成されている
- staging の公開 URL が確認できる
- CORS\_ALLOWED\_ORIGINS が更新済み
- backend への再反映まで完了している

---

## 17. staging 用 CloudWatch / SNS を整備する

### 16.1 目的

staging でも最低限の監視導線を持たせます。

### 16.2 作成対象例

- `taskflow-stg-eb-environment-health`
- `taskflow-stg-rds-cpu-high`
- `taskflow-stg-rds-storage-low`
- `taskflow-stg-rds-memory-low`
- `taskflow-stg-cf-5xx-rate-high`
- `taskflow-stg-alerts-apne1`
- `taskflow-stg-alerts-use1`

### 16.3 必須で確認すること

- Elastic Beanstalk の log streaming
- CloudWatch Logs の log group
- RDS alarm
- CloudFront 5xx alarm
- SNS 通知先

### 16.4 作成方針の注意

production 側では EB health alarm に不整合が残っているため、staging 側では alarm を機械的に複製せず、次の順で進めてください。

1. 最小構成で alarm を作る
2. 参照メトリクスを確認する
3. しきい値と評価期間を見直す
4. 誤検知が多すぎないか確認する

### 16.5 作業完了条件

- staging でも logs / alarm / notification の最小構成がある
- production と識別できる命名になっている
- EB health alarm は複製ではなく見直し前提で作成している

---

## 18. staging へ deploy して動作確認する

### 17.1 目的

staging 環境が deploy 可能で、最低限の動作確認ができることを確認します。

### 17.2 事前確認

- `develop` に staging 用 workflow が反映されている
- GitHub `staging` Environment の Variables が揃っている
- staging 用 AWS リソースが作成済みである

### 17.3 backend deploy

GitHub Actions から `deploy-backend-stg` を実行します。

確認ポイント:

- workflow が失敗しない
- Elastic Beanstalk が staging 用 environment へ deploy される
- version label が更新される

### 17.4 frontend deploy

GitHub Actions から `deploy-frontend-stg` を実行します。

確認ポイント:

- S3 upload 成功
- CloudFront invalidation 成功
- CloudFront Function 更新処理が必要なら成功している

### 17.5 deploy 直後のスモークテスト

まずは最小限の確認だけを行います。

確認項目:

- `/login` が開く
- register できる
- login できる
- task list が開く
- create / update / delete ができる
- CloudWatch Logs へ到達している
- `/api/*` の認可エラーが HTML に書き換わらない

### 17.6 追加の総合確認

スモークテスト完了後に、必要に応じて Phase7 相当の総合確認へ進みます。

確認対象の例:

- フィルタ
- 詳細
- session expired
- 認可境界
- ログ追跡
- 監視導線

### 17.7 作業完了条件

- staging へ backend / frontend deploy が成功している
- staging 公開 URL で主要導線が動作する
- production に影響が出ていない

---

## 19. staging / production の運用ルールを確定する

### 18.1 ルール化する内容

以下を文書化してください。

- `develop` → staging deploy
- `master` → production deploy
- deploy 順序は backend → frontend
- secret 更新時の反映手順
- monitoring / incident 初動の確認順
- staging で確認後に production へ反映する運用
- policy / template / 実 AWS 設定を同期する運用

### 18.2 policy / template / 実 AWS 設定の同期ルール

GitHub Actions 周りの設定は、次の 3 か所を常に同じ状態に保ちます。

- リポジトリ内の template / policy ファイル
- 実際に使っている workflow ファイル
- AWS 上の inline policy や trust policy

特に deploy role policy を変更した場合は、1 か所だけ更新して終わりにしないこと。

### 18.3 README / 設計書への反映

以下の文書も更新してください。

- README
- インフラ設計書
- deploy 手順書
- 運用メモ

### 18.4 作業完了条件

- staging / production の役割が文書上も明確になっている
- 今後の環境追加や運用時に迷わない
- policy / template / 実 AWS 設定の同期ルールが文書化されている

---

## 20. 作業内容を `master` へマージする

### 20.1 目的
staging 環境構築用の作業ブランチで実施した変更を、最終的に `master` へ取り込みます。

### 20.2 ローカル変更を確認する
作業ブランチ上で次を実行します。

```bash
git status
git diff
```

確認ポイント:
- 意図したファイルだけが変更されている
- 不要な一時ファイルやローカル用メモが混ざっていない

### 20.3 コミットする
変更内容をコミットします。

```bash
git add .
git commit -m "Add staging environment setup"
```

コミットメッセージ例:
- `Add staging environment setup`
- `Add staging deploy workflows and docs`
- `Prepare staging AWS environment configuration`

### 20.4 リモートへ push する

```bash
git push origin chore/staging-environment-setup
```

※ ブランチ名は実際に作成した名前へ置き換えてください。

### 20.5 Pull Request を作成する
GitHub で Pull Request を作成します。

設定:
- base: `master`
- compare: staging 環境構築用の作業ブランチ

Pull Request の説明に最低限書くこと:
- staging 環境構築のための変更であること
- 追加した workflow / policy / ドキュメント
- AWS 側で作成済みの staging リソース
- 実施済みの確認内容
- 残課題があればその内容

### 20.6 レビュー後に master へマージする
レビュー完了後、GitHub 上で `master` へマージします。

確認ポイント:
- `master` へ意図した変更が反映されること
- 直接 `master` へ push していないこと

### 20.7 develop への取り込み方針を確認する
今回の変更は staging 環境構築用ですが、将来的に `develop` でも workflow / ドキュメント差分を使う必要がある可能性があります。

そのため、`master` へマージした後は次も判断してください。

- `develop` へも取り込むか
- 取り込む場合は `master` から `develop` へ同期するか
- staging 専用変更として `master` 側だけに残すか

この判断はチーム運用ルールに従ってください。

### 20.8 作業完了条件
- 作業ブランチでの変更を commit できている
- Pull Request を作成できている
- レビュー後に `master` へマージできる状態になっている

---

## 21. 最終チェックリスト

最後に、以下を 1 件ずつ確認してください。

- [ ] `master` を最新化して作業ブランチを作成した
- [ ] production は `master` 用に整理できた
- [ ] staging Environment を作成した
- [ ] staging 用 workflow を作成した
- [ ] staging 用 IAM Role / trust policy / inline policy を作成した
- [ ] staging 用 instance profile / service role を確認した
- [ ] staging 用 Parameter Store を作成した
- [ ] staging 用 artifact bucket を作成した
- [ ] staging 用 RDS を作成した
- [ ] staging 用 Elastic Beanstalk を作成した
- [ ] staging 用 frontend bucket を作成した
- [ ] staging 用 CloudFront を作成した
- [ ] staging 用 Alarm / SNS / Logs を整備した
- [ ] CORS 更新後の backend 再反映まで完了した
- [ ] 初回 backend deploy 後の Flyway 確認が完了した
- [ ] staging deploy が成功した
- [ ] staging 公開 URL で主要導線を確認した
- [ ] production へ影響が出ていない
- [ ] 運用ルールと設計書を更新した
- [ ] 作業内容を commit し、Pull Request から `master` へマージできる状態にした

---

## 22. 補足

最初から production の完全コピーを目指すより、まずは以下を優先しても構いません。

- staging deploy が通ること
- staging でログインとタスク CRUD ができること
- production と DB / secret / URL が分離されていること

ただし、次の 5 点は最初から必ず意識してください。

- DB
- secret
- 配信 URL
- instance profile / service role
- CloudFront と S3 の公開制御

これらを後回しにすると、production と staging の混線事故が起きやすくなります。

