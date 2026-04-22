# 15_staging用frontend_bucket作成 作業メモ

## 更新日時

- 2026-04-20

## 対象

- 15. staging 用 frontend bucket を作成する

## 目的

- staging 用 frontend の配信先 S3 bucket を production と分離する
- frontend bucket は直接公開せず、CloudFront 経由のみで参照する構成にする

## production 参考構成

- Bucket: `taskflow-prd-frontend-site-359429618625`
- Region: `ap-northeast-1`
- Block Public Access: 全項目 `true`
- Object Ownership: `BucketOwnerEnforced`
- Default encryption: `AES256`
- Bucket Key: `true`
- Versioning: 未設定
- Bucket policy public status: `IsPublic=false`

## staging で作成するもの

- Bucket: `taskflow-stg-frontend-site-359429618625`
- Region: `ap-northeast-1`

## 設定方針

- production 用 bucket は使い回さない
- Block Public Access は全項目有効のままにする
- Object Ownership は `BucketOwnerEnforced` にする
- Default encryption は `AES256` にする
- bucket policy は staging 用 CloudFront Distribution 作成後に設定する

## GitHub Environment へ反映する値

- `FRONTEND_BUCKET`: `taskflow-stg-frontend-site-359429618625`

## 次に確認すること

- staging frontend bucket が作成されていること
- Block Public Access が全項目有効であること
- bucket policy は CloudFront 作成後に staging distribution のみに許可すること
- GitHub staging Environment に `FRONTEND_BUCKET` が登録されていること

## 実施結果

- Bucket `taskflow-stg-frontend-site-359429618625` を作成した
- Region: `ap-northeast-1`
- Block Public Access: 全項目 `true`
- Object Ownership: `BucketOwnerEnforced`
- Default encryption: `AES256`
- Bucket Key: `true`
- Blocked encryption types: `NONE`
- Versioning: 未設定
- Bucket policy: 未作成

## 保留事項

- frontend deploy 前のため、bucket 内の `index.html` 配信確認は未実施

## GitHub Environment 登録結果

- `FRONTEND_BUCKET`: `taskflow-stg-frontend-site-359429618625`
- ユーザー操作により GitHub staging Environment へ登録済み

## CloudFront 作成後の bucket policy 設定結果

- Distribution ID: `EY2A56GLZXQQ4`
- Distribution ARN: `arn:aws:cloudfront::359429618625:distribution/EY2A56GLZXQQ4`
- Policy: `.github/workflows/s3_frontend_bucket_policy_stg.json`
- 許可内容: CloudFront service principal からの `s3:GetObject`
- 制限条件: `AWS:SourceArn` が staging Distribution ARN と一致すること
- bucket policy status: `IsPublic=false`
