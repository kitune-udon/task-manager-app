# 08_staging用workflow作成 作業メモ

## 更新日時

- 2026-04-19

## 対象

- 8. staging 用 workflow を作成する

## 実施内容

- production 用 workflow を複製元として staging 用 workflow を追加
  - `.github/workflows/deploy-backend-stg.yml`
  - `.github/workflows/deploy-frontend-stg.yml`
- staging workflow の `environment` を `staging` に設定
- staging frontend workflow の CloudFront Function 名を `taskflow-stg-spa-router` に設定
- production workflow の既定 `ref` を `master` に変更
- workflow 関連ドキュメントを staging / production 分離方針に更新

## 追加・更新ファイル

- `.github/workflows/deploy-backend-stg.yml`
- `.github/workflows/deploy-frontend-stg.yml`
- `.github/workflows/deploy-backend-prd.yml`
- `.github/workflows/deploy-frontend-prd.yml`
- `.github/workflows/github_environment_variables_and_secrets.md`
- `.github/workflows/notes.md`

## 確認ポイント

- staging 用 backend / frontend workflow が存在する
- staging workflow は `environment: staging` を参照する
- production workflow は削除や破壊をしていない
- production の既定 deploy ref は `master`
- staging の既定 deploy ref は `develop`

## 補足

- policy / trust policy の staging 実体作成は step 9 で対応する
- staging 用 Variables のうち未作成リソースに依存する値は後続 step で確定させる
