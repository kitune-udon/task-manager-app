# Phase8 引き継ぎメモ

## 目的

Phase7 で確認した結果を、Phase8 の成果物整理や残課題対応へそのまま引き継げる形にまとめる。

## 結論

- `UI 不整合`: Phase7 で解消済み
- `認可不整合`: Phase7 で解消済み
- `ログ欠落`: Phase7 最終状態では新規未解消なし
- `監視課題`: `taskflow-prd-eb-environment-health` の不整合は未解消
- `Phase6 継続課題`: そのまま継続管理が必要

## 1. UI 不整合の分類

### 解消済み

- 公開 frontend が `VITE_API_BASE_URL` 未設定時に `http://localhost:8080` へ解決される問題
  - 影響:
    - `signup` 送信で `Network Error`
  - 対応:
    - `frontend/src/lib/apiClient.ts` を修正
- `deploy-backend-prd` の Procfile が `SERVER_PORT` と不整合だった問題
  - 影響:
    - backend が `8080` で起動し、nginx upstream `5000` と噛み合わず `502`
  - 対応:
    - `.github/workflows/deploy-backend-prd.yml` を修正
    - backend の `application-{dev,local,prod}.yml` も `${PORT:${SERVER_PORT:8080}}` へ補強

### Phase8 への扱い

- 再調査は不要
- 判定メモでは `解消済みの初回検出事項` として扱えばよい

## 2. 認可不整合の分類

### 解消済み

- `CloudFront` の distribution 全体 `403/404 -> /index.html -> 200`
  - 影響:
    - `/api/*` の `403` が HTML へ書き換わる
    - 他人タスク詳細が空表示
    - 更新 / 削除が誤成功表示
  - 対応:
    - `frontend/cloudfront/spa-viewer-request.js`
    - `frontend/scripts/ensure-cloudfront-spa-routing.mjs`
    - `.github/workflows/deploy-frontend-prd.yml`
  - 実環境:
    - `taskflow-prd-spa-router` を production distribution `E688SH91TX10P` へ反映済み
    - `403 / 404` custom error response は除去済み

### Phase8 への扱い

- 再調査は不要
- CloudFront ルーティングの恒久対策として維持する

## 3. ログ欠落の分類

### 未解消なし

- `CloudWatch Logs` で `requestId / eventId` ベースの追跡が可能
- 確認済み:
  - `LOG-AUTH-001`
  - `LOG-AUTH-003`
  - `LOG-AUTH-006`
  - `LOG-REQ-001`
- `tail` / `bundle` ログ取得も可能

### Phase8 への扱い

- 新規の `ログ欠落` 課題としては起票不要
- 将来監視として `eventId` 単位の `Metric Filter / Alarm` を検討対象に残す

## 4. 未解消の監視課題

- `taskflow-prd-eb-environment-health`
  - CloudWatch Alarm は存在する
  - ただし Phase7 spot check 時点で `ALARM`
  - 同時点の `Elastic Beanstalk` 本体は `Ready / Green / Ok`
  - `AWS/ElasticBeanstalk EnvironmentHealth` の datapoint は直近でも `0.0` が多く、実状態と整合していない

### Phase8 での優先対応

- Alarm の参照メトリクスとしきい値を再確認する
- 必要なら `alarm threshold` メモと実 alarm 設定を合わせる

## 5. deploy role policy と運用同期

- Phase7 で次を更新した
  - `.github/workflows/github_actions_deploy_role_policy.json`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6_5/workflow_templates/github_actions_deploy_role_policy.json`
  - AWS inline policy `taskflow-prd-github-actions-deploy-policy`
- 今後も policy 更新時は
  - repo 本体
  - workflow template
  - 実 AWS inline policy
  を常に同じ内容へ揃える

## 6. production Environment 設定

- `prevent_self_review`
  - 現在値: `false`
  - 初回検証で緩めたままなので、次フェーズで再有効化要否を判断する

## 7. Phase6 からの継続課題

- `db SG` 例外
  - `sg-06db941d256dcaaa1` からの `5432` inbound
- `DB_PASSWORD / JWT_SECRET` ローテーション
- `taskflow-cli-operator` の最小権限化
- `eventId` 単位の監視未整備

## 8. 次回 deploy 時の注意

- 本番再反映はローカル手動 deploy ではなく、必ず
  - `deploy-backend-prd`
  - `deploy-frontend-prd`
  を順に再実行してから再試験する
- Phase7 の恒久対策として、次を落とさない
  - `frontend/cloudfront/spa-viewer-request.js`
  - `frontend/scripts/ensure-cloudfront-spa-routing.mjs`
  - `.github/workflows/deploy-frontend-prd.yml`

## 9. 参照先

- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/01_Phase7完了判定メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/phase7_execution_log.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes/phase7_5_7_authz_boundary_issue.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes/phase7_5_8_network_api_cloudwatch.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/notes/phase7_5_9_monitoring_spot_check.md`
