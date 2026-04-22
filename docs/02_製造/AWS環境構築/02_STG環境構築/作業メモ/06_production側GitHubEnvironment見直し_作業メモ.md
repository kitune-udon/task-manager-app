# 06_production側GitHubEnvironment見直し 作業メモ

## 更新日時

- 2026-04-19

## 対象

- 6. production 側の GitHub Environment を master 用に見直す

## 現状確認結果

- GitHub Environment `production` は存在する
- `Required reviewers` は設定済み
  - reviewer: `kitune-udon`
- `Deployment branches` は `master`
- 手順書の期待値どおりに修正済み

## 変数確認状況

- ユーザー共有画面で Environment Variables の存在を確認
  - `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN`
  - `AWS_REGION`: `ap-northeast-1`
  - `EB_APPLICATION_NAME`: `taskflow-prd-eb-app`
  - `EB_ENVIRONMENT_NAME`: `Taskflow-prd-eb-app-env`
  - `EB_S3_BUCKET`: `taskflow-prd-eb-artifacts-359429618625`
  - `VERSION_LABEL_PREFIX`: `taskflow-prd-backend`
  - `BACKEND_JAR_NAME`: `task-0.0.1-SNAPSHOT.jar`
  - `FRONTEND_BUCKET`: `taskflow-prd-frontend-site-359429618625`
  - `CLOUDFRONT_DISTRIBUTION_ID`: `E688SH91TX10P`
- `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN` は画面上では省略表示だったが、事前確認済みの production deploy role ARN と整合する想定

## 対応状況

- 手順書 6 の確認項目は対応完了
- `Deployment branches: develop -> master` の変更完了
- production 用 Variables は残っていることを確認済み

## 証跡

- ユーザー共有スクリーンショットで確認
  - Deployment branches and tags: `master`
  - Environment variables 一覧: 9 項目表示あり

## 補足

- ターミナル環境からは GitHub Variables 実値 API を直接取得できないため、画面確認結果を正式な確認結果として採用
