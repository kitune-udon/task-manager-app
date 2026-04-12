# GitHub Environment 変数 / Secrets

## Environment
- 名前: `production`
- 推奨する保護設定:
  - Required reviewers を有効化
  - Deployment branches は `develop` のみに制限

## Environment Variables
以下の値を **Settings > Environments > production > Variables** に登録する。

| 変数名 | 設定例 | 用途 |
| --- | --- | --- |
| `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN` | `arn:aws:iam::359429618625:role/taskflow-prd-github-actions-deploy-role` | OIDC で引き受ける AWS deploy role |
| `AWS_REGION` | `ap-northeast-1` | デプロイ先リージョン |
| `EB_APPLICATION_NAME` | `taskflow-prd-eb-app` | Elastic Beanstalk アプリケーション名 |
| `EB_ENVIRONMENT_NAME` | `Taskflow-prd-eb-app-env` | Elastic Beanstalk 環境名 |
| `EB_S3_BUCKET` | `taskflow-prd-eb-artifacts-359429618625` | backend の source bundle 保存先バケット |
| `VERSION_LABEL_PREFIX` | `taskflow-prd-backend` | Elastic Beanstalk version label の接頭辞 |
| `BACKEND_JAR_NAME` | `task-0.0.1-SNAPSHOT.jar` | Spring Boot の bootJar ファイル名 |
| `FRONTEND_BUCKET` | `taskflow-prd-frontend-site-359429618625` | frontend 配備先 S3 バケット |
| `CLOUDFRONT_DISTRIBUTION_ID` | `E688SH91TX10P` | frontend 配備先 CloudFront Distribution ID |

## Repository Secrets
- OIDC のみを使う構成であれば追加不要。

## リポジトリへ配置するファイル
- `.github/workflows/ci.yml`
- `.github/workflows/deploy-backend-prd.yml`
- `.github/workflows/deploy-frontend-prd.yml`

## 補足
- deploy workflow は `production` Environment の承認後にのみ実行される。
- OIDC trust policy をシンプルかつ厳格に保つため、`production` Environment の deploy 対象ブランチは `develop` のみに制限する。
- 単独作業者が初回検証を行う場合は、`Prevent self-review` を一時的に無効化してもよい。backend / frontend の初回 deploy 検証が終わったら再度有効化する。
- 初回の Elastic Beanstalk 環境 bootstrap では、Elastic Beanstalk / S3 / CloudFront の最小権限だけでは不足する場合がある。`github_actions_deploy_role_policy.json` と実際の inline policy は常にそろえて管理する。
