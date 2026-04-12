# GitHub Environment Variables / Secrets

## Environment
- Name: `production`
- Recommended protection:
  - Required reviewers: enabled
  - Deployment branches: `develop` only

## Environment Variables
Register the following values in **Settings > Environments > production > Variables**.

| Variable | Example value | Purpose |
| --- | --- | --- |
| `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN` | `arn:aws:iam::359429618625:role/taskflow-prd-github-actions-deploy-role` | OIDC assume target role |
| `AWS_REGION` | `ap-northeast-1` | Deploy region |
| `EB_APPLICATION_NAME` | `taskflow-prd-eb-app` | Elastic Beanstalk application |
| `EB_ENVIRONMENT_NAME` | `Taskflow-prd-eb-app-env` | Elastic Beanstalk environment |
| `EB_S3_BUCKET` | `taskflow-prd-eb-artifacts-359429618625` | Backend source bundle bucket |
| `VERSION_LABEL_PREFIX` | `taskflow-prd-backend` | EB version label prefix |
| `BACKEND_JAR_NAME` | `task-0.0.1-SNAPSHOT.jar` | Spring Boot bootJar file name |
| `FRONTEND_BUCKET` | `taskflow-prd-frontend-site-359429618625` | Frontend S3 bucket |
| `CLOUDFRONT_DISTRIBUTION_ID` | `E688SH91TX10P` | Frontend CloudFront distribution |

## Repository Secrets
- None required when using OIDC only.

## Files to place in repository
- `.github/workflows/ci.yml`
- `.github/workflows/deploy-backend-prd.yml`
- `.github/workflows/deploy-frontend-prd.yml`

## Notes
- The deploy workflows run only after `production` environment approval.
- Restrict `production` environment deployment branches to `develop` so that the OIDC trust policy can stay simple and strict.
- For first-time verification by a single operator, `Prevent self-review` may be disabled temporarily. Re-enable it after the initial backend / frontend deploy verification is complete.
- The AWS deploy role needs more than the initial Elastic Beanstalk / S3 / CloudFront minimum when the environment is bootstrapped for the first time. Keep the inline policy aligned with `github_actions_deploy_role_policy.json`.
