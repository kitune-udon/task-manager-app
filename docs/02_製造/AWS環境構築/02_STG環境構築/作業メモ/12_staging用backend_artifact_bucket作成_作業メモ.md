# 12_staging用backend_artifact_bucket作成 作業メモ

## 更新日時

- 2026-04-19

## 対象

- 12. staging 用 backend artifact bucket を作成する

## 確認結果

- 既存 bucket
  - `taskflow-prd-eb-artifacts-359429618625`
  - `taskflow-prd-frontend-site-359429618625`
- staging 用 backend artifact bucket は未作成

## 作成する bucket

- bucket 名: `taskflow-stg-eb-artifacts-359429618625`
- region: `ap-northeast-1`

## 推奨設定

- Block Public Access: 有効のまま
- Versioning: 任意
- Object Ownership: デフォルトで可

## 用途

- `deploy-backend-stg.yml` が backend の source bundle をアップロードする
- Elastic Beanstalk application version の source bundle 保存先として使う

## GitHub へ反映する値

- `staging` Environment
  - `EB_S3_BUCKET = taskflow-stg-eb-artifacts-359429618625`

## 注意事項

- production 用 bucket を使い回さない
- region は workflow と同じ `ap-northeast-1`
- bucket 作成後に GitHub `staging` Environment の `EB_S3_BUCKET` を更新する

## 次に確認すること

- bucket が存在するか
- region が `ap-northeast-1` か
- `staging` Environment の `EB_S3_BUCKET` に登録したか

## 確認結果

- bucket は作成済み
  - bucket 名: `taskflow-stg-eb-artifacts-359429618625`
  - bucket ARN: `arn:aws:s3:::taskflow-stg-eb-artifacts-359429618625`
  - region: `ap-northeast-1`

## 証跡

- `aws s3api head-bucket --bucket taskflow-stg-eb-artifacts-359429618625`
- `aws s3api get-bucket-location --bucket taskflow-stg-eb-artifacts-359429618625`

## 保留

- なし

## GitHub 反映

- `staging` Environment
  - `EB_S3_BUCKET = taskflow-stg-eb-artifacts-359429618625`
  - ユーザー確認により登録済み

## 完了判定

- 手順書 12 の完了条件を満たした
  - staging 用 artifact bucket が作成されている
  - GitHub `staging` Environment に bucket 名が登録されている
