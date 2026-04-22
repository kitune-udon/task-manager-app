# AWSシークレット管理方針候補メモ

## 目的

- AWS 上で `DB_URL` や `JWT_SECRET` などの秘密情報をどこに置くかを決める
- ローカル `.env` ベースの運用から、AWS 向けの安全な管理方式へ寄せる
- 初回 MVP 公開に対して、コストと運用負荷のバランスがよい方式を選ぶ

## 前提

- backend / frontend ともに環境変数ベースで設定を読める状態になっている
- backend では `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET` / `JWT_EXPIRATION_MILLIS` / `CORS_ALLOWED_ORIGINS` を使用する
- frontend では `VITE_API_BASE_URL` を使用する
- 初回公開では、まず運用しやすい方式を優先する

## シークレットとして扱う対象

### backend

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- 必要に応じて `JWT_EXPIRATION_MILLIS`
- 必要に応じて `CORS_ALLOWED_ORIGINS`

### frontend

- `VITE_API_BASE_URL`

補足:

- `VITE_API_BASE_URL` 自体は機密情報ではない
- ただし、デプロイ設定としてどこで管理するかは決めておいたほうがよい

## 候補案

### 案A: Elastic Beanstalk 環境変数へ直接登録する

#### 利点

- 最も単純で、初回構築が早い
- AWS Console 上で完結しやすい

#### 注意点

- secret 値が環境変数設定の中に直接入る
- 値の更新や参照の統制は、後から厳密化しにくい

### 案B: Systems Manager Parameter Store `SecureString` を使う

#### 利点

- `SecureString` で暗号化して管理できる
- 階層構造で整理しやすい
- Secrets Manager より軽量で、MVP 初期運用と相性がよい
- Elastic Beanstalk から参照する運用にも寄せやすい

#### 注意点

- 参照権限の設定を意識する必要がある
- 値更新時の反映手順は運用ルール化したほうがよい

### 案C: AWS Secrets Manager を使う

#### 利点

- シークレット管理としてはより本格的
- 将来のローテーション運用を見据えやすい

#### 注意点

- 初回 MVP としてはやや重い
- コストと設定項目が増える

## 推奨案

- 初回公開は案Bの `Systems Manager Parameter Store SecureString` を推奨する

## 推奨理由

- MVP 段階では、Secrets Manager ほど重くせずに安全性を確保しやすい
- `SecureString` と KMS ベースで secret を暗号化できる
- backend の secret 群を階層的に整理しやすい
- frontend の `VITE_API_BASE_URL` は secret ではないため、frontend 側は build / 配信設定として分離しやすい

## 推奨する格納方針

### backend secret

- Parameter Store に以下のような階層で配置する
- 例:
  - `/taskflow/prd/backend/DB_URL`
  - `/taskflow/prd/backend/DB_USERNAME`
  - `/taskflow/prd/backend/DB_PASSWORD`
  - `/taskflow/prd/backend/JWT_SECRET`
  - `/taskflow/prd/backend/JWT_EXPIRATION_MILLIS`
  - `/taskflow/prd/backend/CORS_ALLOWED_ORIGINS`

### frontend config

- `VITE_API_BASE_URL` は secret 扱いせず、frontend の build 設定値として管理する
- 例:
  - CloudFront/S3 へデプロイする build 時に設定する
  - または CI/CD 導入後に pipeline 側で注入する

## 運用ルール案

- secret 値は Git に置かない
- `.env.local` や `.env.production` に本番 secret を直書きしない
- 値を更新したら、更新日時と更新者をメモへ残す
- 本番 secret の参照権限は必要最小限にする

## Elastic Beanstalk との扱い

- 2025年以降の Elastic Beanstalk では、Secrets Manager / Parameter Store の参照に対応している
- 初回は Parameter Store を第一候補にし、Elastic Beanstalk 側の環境変数から参照させる構成を推奨する
- もし環境やプラットフォーム制約で参照が難しい場合のみ、暫定的に Elastic Beanstalk 環境変数へ直接入れる

## 今回の判断候補

- backend secret: Parameter Store `SecureString`
- frontend config: `VITE_API_BASE_URL` は build 設定として管理
- 秘密情報は Git / `.env.example` に実値を入れない
- 将来ローテーションが必要になったら Secrets Manager を再検討する

## 最終決定

- 2026年4月8日、backend secret は Systems Manager Parameter Store `SecureString` で管理する方針で確定した
- `VITE_API_BASE_URL` は secret 扱いせず、frontend の build 設定として管理する方針で確定した
- 実値は Git や `.env.example` に入れない方針で確定した
- 将来ローテーション要件が強くなった場合は Secrets Manager を再検討する

## 補足

- 初回公開では、まず Parameter Store を基準に運用し、必要最小限の管理負荷に寄せる
- Elastic Beanstalk 側の参照方式は Phase2 実装時に具体化する

## 参考

- Parameter Store: https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-parameter-store.html
- Parameter Store SecureString / KMS: https://docs.aws.amazon.com/kms/latest/developerguide/services-parameter-store.html
- Elastic Beanstalk secrets integration: https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/AWSHowTo.secrets.env-vars.html
- Elastic Beanstalk release note 2025-03-31: https://docs.aws.amazon.com/elasticbeanstalk/latest/relnotes/release-2025-03-31-environment-secrets.html

## 参照

- `07_環境依存設定一覧.md`
- `10_Phase0_Phase1対応状況チェックリスト.md`
