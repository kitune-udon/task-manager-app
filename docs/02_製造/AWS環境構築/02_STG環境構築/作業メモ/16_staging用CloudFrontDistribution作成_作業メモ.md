# 16_staging用CloudFrontDistribution作成 作業メモ

## 更新日時

- 2026-04-20

## 対象

- 16. staging 用 CloudFront Distribution を作成する

## 目的

- staging 用の公開 URL を production と分離する
- `/api/*` は staging backend、その他は staging frontend bucket へ振り分ける
- frontend bucket は CloudFront 経由のみで参照できるようにする

## production 参考構成

- Distribution ID: `E688SH91TX10P`
- DomainName: `d3jotedl3xn7u4.cloudfront.net`
- Default root object: `index.html`
- Default behavior: frontend S3 origin
- `/api/*` behavior: Elastic Beanstalk backend origin
- OAC: `taskflow-prd-oac-frontend`
- SPA router Function: `taskflow-prd-spa-router`
- Viewer protocol policy: `redirect-to-https`

## staging 作成リソース

- Distribution ID: `EY2A56GLZXQQ4`
- DomainName: `d25w3ecu7nozfz.cloudfront.net`
- Distribution ARN: `arn:aws:cloudfront::359429618625:distribution/EY2A56GLZXQQ4`
- Status: `Deployed`
- OAC: `taskflow-stg-oac-frontend`
- OAC ID: `E3FYQJ4L83GZ6F`
- SPA router Function: `taskflow-stg-spa-router`
- Function ARN: `arn:aws:cloudfront::359429618625:function/taskflow-stg-spa-router`
- Function Status: `DEPLOYED`

## origin / behavior

- frontend origin
  - Origin ID: `taskflow-stg-origin-frontend`
  - DomainName: `taskflow-stg-frontend-site-359429618625.s3.ap-northeast-1.amazonaws.com`
  - OAC: `E3FYQJ4L83GZ6F`
- backend origin
  - Origin ID: `taskflow-stg-origin-backend`
  - DomainName: `taskflow-stg-eb-app-env.eba-tmhj2uhb.ap-northeast-1.elasticbeanstalk.com`
  - Origin protocol policy: `http-only`
- default behavior
  - Target: `taskflow-stg-origin-frontend`
  - Viewer protocol policy: `redirect-to-https`
  - SPA router Function: `taskflow-stg-spa-router`
- `/api/*` behavior
  - Target: `taskflow-stg-origin-backend`
  - Viewer protocol policy: `redirect-to-https`
  - Function association: なし

## 追加で実施した設定

- staging EB app Security Group `sg-0d30ced331019dba9` に CloudFront origin-facing prefix list `pl-58a04531` からの HTTP 80 inbound を追加した
- frontend bucket `taskflow-stg-frontend-site-359429618625` に、Distribution `EY2A56GLZXQQ4` からの `s3:GetObject` のみ許可する bucket policy を設定した
- bucket policy status は `IsPublic=false`
- `/taskflow/stg/backend/CORS_ALLOWED_ORIGINS` を `https://d25w3ecu7nozfz.cloudfront.net` に更新した

## 追加・更新したテンプレート

- `.github/workflows/cloudfront_origin_access_control_config_stg.json`
- `.github/workflows/cloudfront_spa_function_config_stg.json`
- `.github/workflows/cloudfront_distribution_config_stg.json`
- `.github/workflows/s3_frontend_bucket_policy_stg.json`

## GitHub Environment へ反映する値

- `CLOUDFRONT_DISTRIBUTION_ID`: `EY2A56GLZXQQ4`

## 保留事項

- `CORS_ALLOWED_ORIGINS` 更新後の backend 再反映は、18章の初回 backend deploy で実施する
- frontend bucket はまだ空のため、frontend deploy 前は CloudFront からの画面表示確認は未実施

## GitHub Environment 登録結果

- `CLOUDFRONT_DISTRIBUTION_ID`: `EY2A56GLZXQQ4`
- ユーザー操作により GitHub staging Environment へ登録済み

## 完了確認

- Distribution は `Deployed`
- Default root object は `index.html`
- origin は frontend / backend の 2 件
- `/api/*` behavior は staging backend origin に設定済み
- default behavior は staging frontend origin に設定済み
- bucket policy status は `IsPublic=false`
- GitHub staging Environment へ `CLOUDFRONT_DISTRIBUTION_ID` 登録済み
