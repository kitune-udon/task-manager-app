# 07_staging用GitHubEnvironment作成 作業メモ

## 更新日時

- 2026-04-19

## 対象

- 7. staging 用 GitHub Environment を作成する

## 現状確認結果

- GitHub Environment 一覧に `staging` の追加を確認
- `staging` Environment は 2026-04-19 11:45:55 UTC 作成
- `Deployment branches` は `develop`
- branch policy は 1 件で `develop` のみ許可

## 作成方針

- Environment 名: `staging`
- Deployment branches: `develop` のみ許可
- Required reviewers: 必要なら設定
- Variables は先に登録枠を用意し、未作成リソース分は後から更新してよい

## staging Environment に登録する Variables 案

- `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN`
  - 想定値: `arn:aws:iam::359429618625:role/taskflow-stg-github-actions-deploy-role`
  - 状態: step 9 の IAM Role 作成後に確定
- `AWS_REGION`
  - 想定値: `ap-northeast-1`
  - 状態: 先に登録可
- `EB_APPLICATION_NAME`
  - 想定値: `taskflow-stg-eb-app`
  - 状態: step 14 の Elastic Beanstalk 作成後に確定
- `EB_ENVIRONMENT_NAME`
  - 想定値: `Taskflow-stg-eb-app-env`
  - 状態: step 14 の Elastic Beanstalk 作成後に確定
- `EB_S3_BUCKET`
  - 想定値: `taskflow-stg-eb-artifacts-359429618625`
  - 状態: step 12 の S3 bucket 作成後に確定
- `VERSION_LABEL_PREFIX`
  - 想定値: `taskflow-stg-backend`
  - 状態: 先に登録可
- `BACKEND_JAR_NAME`
  - 想定値: `task-0.0.1-SNAPSHOT.jar`
  - 状態: 先に登録可
- `FRONTEND_BUCKET`
  - 想定値: `taskflow-stg-frontend-site-359429618625`
  - 状態: step 15 の S3 bucket 作成後に確定
- `CLOUDFRONT_DISTRIBUTION_ID`
  - 想定値: `EY2A56GLZXQQ4`
  - 状態: step 16 の CloudFront 作成後に確定

## 注意事項

- 現在のターミナル環境からは GitHub Environment の作成や編集を実行できない
- GitHub 画面で作成後、公開 API で `staging` Environment の存在確認は可能
- Variables 実値の取得は認証が必要なため、画面確認またはユーザー共有内容で補完する

## 対応状況

- 手順書 7 の完了条件は満たした
  - `staging` Environment が作成されている
  - `develop` のみ deploy 対象になっている
  - Variables の登録先が用意できている

## 証跡

- `https://api.github.com/repos/kitune-udon/task-manager-app/environments`
  - `staging` Environment の存在を確認
- `https://api.github.com/repos/kitune-udon/task-manager-app/environments/staging/deployment-branch-policies`
  - branch policy `develop` を確認

## 次に確認したいこと

- `staging` Environment Variables へ先行登録する値
  - `AWS_REGION`
  - `VERSION_LABEL_PREFIX`
  - `BACKEND_JAR_NAME`

## 後続章で確定した Variables

- `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN`: `arn:aws:iam::359429618625:role/taskflow-stg-github-actions-deploy-role`
- `EB_APPLICATION_NAME`: `taskflow-stg-eb-app`
- `EB_ENVIRONMENT_NAME`: `Taskflow-stg-eb-app-env`
- `EB_S3_BUCKET`: `taskflow-stg-eb-artifacts-359429618625`
- `FRONTEND_BUCKET`: `taskflow-stg-frontend-site-359429618625`
- `CLOUDFRONT_DISTRIBUTION_ID`: `EY2A56GLZXQQ4`

## 後続章で登録確認済みの Variables

- `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN`: ユーザー操作で登録済み
- `EB_S3_BUCKET`: ユーザー操作で登録済み
- `FRONTEND_BUCKET`: ユーザー操作で登録済み
- `CLOUDFRONT_DISTRIBUTION_ID`: ユーザー操作で登録済み
