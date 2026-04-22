# 11_staging用ParameterStore作成 作業メモ

## 更新日時

- 2026-04-19

## 対象

- 11. staging 用 Parameter Store を作成する

## 確認結果

- production 配下の parameter は 6 件存在
  - `/taskflow/prd/backend/CORS_ALLOWED_ORIGINS` : `String`
  - `/taskflow/prd/backend/DB_PASSWORD` : `SecureString`
  - `/taskflow/prd/backend/DB_URL` : `String`
  - `/taskflow/prd/backend/DB_USERNAME` : `String`
  - `/taskflow/prd/backend/JWT_EXPIRATION_MILLIS` : `String`
  - `/taskflow/prd/backend/JWT_SECRET` : `SecureString`
- staging 配下 `/taskflow/stg/backend/` は未作成

## backend が期待する parameter

- `/taskflow/stg/backend/DB_URL`
- `/taskflow/stg/backend/DB_USERNAME`
- `/taskflow/stg/backend/DB_PASSWORD`
- `/taskflow/stg/backend/JWT_SECRET`
- `/taskflow/stg/backend/JWT_EXPIRATION_MILLIS`
- `/taskflow/stg/backend/CORS_ALLOWED_ORIGINS`

## 型

- `DB_PASSWORD`: `SecureString`
- `JWT_SECRET`: `SecureString`
- それ以外: `String`

## 初期値の方針

- `JWT_EXPIRATION_MILLIS`
  - production 実値: `3600000`
  - staging も同値で開始可
- `DB_URL`
  - production 実値フォーマット: `jdbc:postgresql://taskflow-prd-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com:5432/taskflow`
  - staging は RDS 作成後に `jdbc:postgresql://<staging-rds-endpoint>:5432/taskflow` へ更新
- `DB_USERNAME`
  - staging RDS 作成時に使うユーザー名を先に決めて同じ値にする
- `DB_PASSWORD`
  - staging 専用の強い値を決めて、RDS 作成時も同じ値を使う
- `JWT_SECRET`
  - production と別の staging 専用 secret を設定する
- `CORS_ALLOWED_ORIGINS`
  - production 実値: `https://d3jotedl3xn7u4.cloudfront.net`
  - staging CloudFront 作成後に staging URL へ更新
  - step 11 時点では仮値で作成し、step 16 で更新する

## console 入力候補

- `/taskflow/stg/backend/JWT_EXPIRATION_MILLIS`
  - Type: `String`
  - Value: `3600000`
- `/taskflow/stg/backend/DB_URL`
  - Type: `String`
  - Value: `jdbc:postgresql://replace-after-rds:5432/taskflow`
- `/taskflow/stg/backend/DB_USERNAME`
  - Type: `String`
  - Value: `taskflow_stg`
- `/taskflow/stg/backend/DB_PASSWORD`
  - Type: `SecureString`
  - Value: staging 専用パスワード
- `/taskflow/stg/backend/JWT_SECRET`
  - Type: `SecureString`
  - Value: staging 専用 secret
- `/taskflow/stg/backend/CORS_ALLOWED_ORIGINS`
  - Type: `String`
  - Value: `https://replace-after-cloudfront.invalid`

## 注意事項

- production の secret を流用しない
- `DB_PASSWORD` と `JWT_SECRET` は `SecureString`
- `DB_URL` と `CORS_ALLOWED_ORIGINS` は後続 step で更新前提
- `taskflow-stg-eb-ssm-read-policy` はこの 6 件を読む前提で作成済み

## 次に確認すること

- staging 配下 6 件が作成されているか
- 型が意図どおりか
- `SecureString` が 2 件になっているか
- RDS 作成後に DB 系 3 件を更新したか
- CloudFront 作成後に `CORS_ALLOWED_ORIGINS` を更新したか

## 確認結果

- 6 件の parameter は作成済み
- non-secret の現在値
  - `/taskflow/stg/backend/DB_URL`
    - `jdbc:postgresql://replace-after-rds:5432/taskflow`
  - `/taskflow/stg/backend/DB_USERNAME`
    - `taskflow_stg`
  - `/taskflow/stg/backend/JWT_EXPIRATION_MILLIS`
    - `3600000`
  - `/taskflow/stg/backend/CORS_ALLOWED_ORIGINS`
    - `https://replace-after-cloudfront.invalid`
- 型の確認結果
  - `/taskflow/stg/backend/CORS_ALLOWED_ORIGINS` : `String`
  - `/taskflow/stg/backend/DB_PASSWORD` : `String`
  - `/taskflow/stg/backend/DB_URL` : `String`
  - `/taskflow/stg/backend/DB_USERNAME` : `String`
  - `/taskflow/stg/backend/JWT_EXPIRATION_MILLIS` : `String`
  - `/taskflow/stg/backend/JWT_SECRET` : `SecureString`

## 差分と対応

- `DB_PASSWORD` は当初 `String` だったが、その後 `SecureString` に修正済み
- 現在は `DB_PASSWORD` と `JWT_SECRET` の 2 件が `SecureString`

## 証跡

- `aws ssm describe-parameters --parameter-filters Key=Name,Option=BeginsWith,Values=/taskflow/stg/backend/`
- `aws ssm get-parameters --names /taskflow/stg/backend/DB_URL /taskflow/stg/backend/DB_USERNAME /taskflow/stg/backend/JWT_EXPIRATION_MILLIS /taskflow/stg/backend/CORS_ALLOWED_ORIGINS`

## 再確認結果

- `/taskflow/stg/backend/DB_PASSWORD` : `SecureString`
- `/taskflow/stg/backend/JWT_SECRET` : `SecureString`

## 完了判定

- 手順書 11 の完了条件を満たした
  - staging 用 Parameter Store パスが作成されている
  - production と異なる secret を設定する前提が整っている

## CloudFront 作成後の更新結果

- `/taskflow/stg/backend/CORS_ALLOWED_ORIGINS`
  - Type: `String`
  - Value: `https://d25w3ecu7nozfz.cloudfront.net`
  - Version: `2`
- 反映タイミング
  - backend は初回 staging deploy 時にこの値を読み込む
