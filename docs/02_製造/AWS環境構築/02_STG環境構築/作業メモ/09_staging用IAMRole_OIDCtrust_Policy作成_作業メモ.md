# 09_staging用IAMRole_OIDCtrust_Policy作成 作業メモ

## 更新日時

- 2026-04-19

## 対象

- 9. staging 用 IAM Role / OIDC trust / Policy を作成する

## 方針

- AWS コンソールでユーザーが作業する
- repo 側には console に貼り付ける staging 用テンプレートを残す
- policy / template / 実 AWS 設定をずらさない

## 作成対象

- IAM Role 名: `taskflow-stg-github-actions-deploy-role`
- trust policy: `environment:staging` を許可
- inline policy: staging 用 bucket 名で分離

## テンプレートファイル

- trust policy:
  - `.github/workflows/github_actions_oidc_trust_policy_stg.json`
- inline policy:
  - `.github/workflows/github_actions_deploy_role_policy_stg.json`

## コンソール入力メモ

- Trusted entity type: `Web identity`
- Identity provider: `token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`
- Role 名: `taskflow-stg-github-actions-deploy-role`

## 想定 ARN

- `arn:aws:iam::359429618625:role/taskflow-stg-github-actions-deploy-role`

## 注意事項

- trust policy の `sub` は `repo:kitune-udon/task-manager-app:environment:staging`
- production 用 role や policy を上書きしない
- staging 用 bucket はまだ未作成でも、ARN を先に policy に書いてよい
- GitHub Environment `staging` の `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN` は role 作成後に更新する

## 次に確認すること

- role が IAM に存在するか
- trust policy が `environment:staging` になっているか
- inline policy が staging bucket 名を向いているか
- role ARN を `staging` Environment に登録したか

## 確認結果

- 実施済み
- IAM Role
  - role 名: `taskflow-stg-github-actions-deploy-role`
  - ARN: `arn:aws:iam::359429618625:role/taskflow-stg-github-actions-deploy-role`
- trust policy
  - `Federated`: `arn:aws:iam::359429618625:oidc-provider/token.actions.githubusercontent.com`
  - `aud`: `sts.amazonaws.com`
  - `sub`: `repo:kitune-udon/task-manager-app:environment:staging`
- role policy
  - attached managed policies: なし
  - inline policy 名: `taskflow-stg-github-actions-deploy-policy`
  - staging 向け S3 ARN を参照
    - `taskflow-stg-eb-artifacts-359429618625`
    - `taskflow-stg-frontend-site-359429618625`
- GitHub
  - `staging` Environment の `AWS_GITHUB_ACTIONS_DEPLOY_ROLE_ARN` 登録済み
  - 登録値: `arn:aws:iam::359429618625:role/taskflow-stg-github-actions-deploy-role`

## 証跡

- `aws iam get-role --role-name taskflow-stg-github-actions-deploy-role`
- `aws iam list-attached-role-policies --role-name taskflow-stg-github-actions-deploy-role`
- `aws iam list-role-policies --role-name taskflow-stg-github-actions-deploy-role`
- `aws iam get-role-policy --role-name taskflow-stg-github-actions-deploy-role --policy-name taskflow-stg-github-actions-deploy-policy`

## 完了判定

- 手順書 9 の完了条件を満たした
  - staging 用 IAM Role が作成されている
  - trust policy が `environment:staging` 前提になっている
  - deploy role ARN が `staging` Environment に登録されている
