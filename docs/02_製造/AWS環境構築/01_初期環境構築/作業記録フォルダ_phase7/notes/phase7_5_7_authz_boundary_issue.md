# 5.7 未認証・セッション切れ・認可境界確認メモ

## 実施日時

- `2026-04-13 JST`

## 結論

- `未認証 direct URL 誘導`: 想定どおり
- `session expired`: 想定どおり
- `代表バリデーション`: 想定どおり
- `認可境界（他人タスクの詳細 / 更新 / 削除）`: `初回は要修正、修正後再試験で解消`

## 確認できたこと

### 1. 未認証 direct URL

- 未ログイン状態で `https://d3jotedl3xn7u4.cloudfront.net/tasks` を直接開くと `/login` へ誘導される
- 未ログインで `GET /api/tasks` を呼ぶと `401 ERR-AUTH-001`

### 2. session expired

- ログイン済みブラウザで token を `invalid-token` に差し替えて再読込すると `/login` へ戻る
- その際の `GET /api/tasks` は `401 ERR-AUTH-003`
- 画面には `認証期限が切れたため、再度ログインしてください。` が表示される

### 3. 入力バリデーション

- `タスク作成` 画面で未入力送信すると
  - `入力内容を確認してください。`
  - `タイトルを入力してください。`
- タイトルを `101` 文字にすると
  - `タイトルは100文字以内で入力してください。`

## 重大な不整合

### 他人タスク詳細

- ユーザー A が作成したタスクを、ユーザー B で `GET /api/tasks/{id}` すると
  - 想定: `403 ERR-AUTH-005`
  - 実際: `200` で `index.html` が返る
- 画面では権限エラーが出ず、詳細項目が `-` だらけの空表示になる

### 他人タスク更新

- ユーザー B で `PUT /api/tasks/{id}` を実行すると
  - 想定: `403 ERR-TASK-005`
  - 実際: `200` で `index.html` が返る
- 画面では `タスクを更新しました。` と成功表示される
- ただし、ユーザー A で実データを確認するとタイトル / ステータス / 優先度は変更されていない

### 他人タスク削除

- ユーザー B で `DELETE /api/tasks/{id}` を実行すると
  - 想定: `403 ERR-TASK-006`
  - 実際: `200` で `index.html` が返る
- 画面では `タスクを削除しました。` と成功表示される
- ただし、ユーザー A で実データを確認すると対象タスクは残存している

## 原因

- CloudFront distribution `E688SH91TX10P` には `/api/*` behavior が存在する
- 一方で distribution 全体の `CustomErrorResponses` が次の設定になっている
  - `403 -> /index.html -> 200`
  - `404 -> /index.html -> 200`
- そのため backend origin が返した `403` が API でも `index.html` に書き換えられ、frontend では成功扱いまたは空表示になる

## 修正対応

- SPA deep link の rewrite を distribution 全体の custom error response ではなく、`viewer-request` の CloudFront Function で扱う方針へ変更した
- `frontend/cloudfront/spa-viewer-request.js` を追加し、次だけを `/index.html` へ rewrite するようにした
  - 画面ルート
  - asset 拡張子を持たない path
- 逆に rewrite しない対象を明示した
  - `/api/*`
  - `.` を含む静的 asset path
- `frontend/scripts/ensure-cloudfront-spa-routing.mjs` を追加し、次を自動化した
  - CloudFront Function `taskflow-prd-spa-router` の作成または更新
  - `LIVE` publish
  - distribution `E688SH91TX10P` の `DefaultCacheBehavior` への関連付け
  - `403 / 404 -> /index.html -> 200` custom error response の除去
- `.github/workflows/deploy-frontend-prd.yml` に上記 script 実行を追加し、今後の frontend deploy でも設定が崩れないようにした
- `.github/workflows/github_actions_deploy_role_policy.json` を更新し、deploy role で CloudFront distribution / function の更新ができるようにした
- 実環境では inline policy `taskflow-prd-github-actions-deploy-policy` を最新化し、script を実行して production へ反映した

## 修正後再試験結果

- 実施日時: `2026-04-13 JST`
- 結論:
  - `未認証 direct URL 誘導`: 想定どおり
  - `session expired`: 想定どおり
  - `代表バリデーション`: 想定どおり
  - `認可境界（他人タスクの詳細 / 更新 / 削除）`: `解消`
- 詳細結果:
  - 未ログインで `GET /api/tasks` は `401 ERR-AUTH-001`
  - token 破損状態で `GET /api/tasks` は `401 ERR-AUTH-003`
  - 他人タスク詳細 `GET /api/tasks/{id}` は `403 ERR-AUTH-005`
  - 他人タスク更新 `PUT /api/tasks/{id}` は `403 ERR-TASK-005`
  - 他人タスク削除 `DELETE /api/tasks/{id}` は `403 ERR-TASK-006`
  - creator 側で実データを確認し、対象タスクが未変更のままであることを確認した

## 修正後の証跡

- 未認証 redirect 再試験:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_10_unauth_redirect_retest.png`
- A のタスク一覧 再試験:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_11_user_a_tasks_retest.png`
- 他人タスク詳細の `403` 再試験:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_12_forbidden_detail_retest.png`
- 他人タスク更新の `403` 再試験:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_13_forbidden_update_retest.png`
- 他人タスク削除の `403` 再試験:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_14_forbidden_delete_retest.png`
- バリデーション再試験:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_15_validation_required_retest.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_16_validation_length_retest.png`
- session expired 再試験:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_17_session_expired_retest.png`

## 証跡

- 未認証 redirect:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_01_unauth_redirect.png`
- A のタスク一覧:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_02_user_a_tasks.png`
- 他人タスク詳細の空表示:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_04_authz_detail_blank.png`
- 他人タスク更新の誤成功表示:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_05_authz_update_result.png`
- 他人タスク削除の誤成功表示:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_06_authz_delete_result.png`
- バリデーション:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_07_validation_required.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_08_validation_length.png`
- session expired:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase7/screenshots/phase7_5_7_09_session_expired.png`

## 後処理

- 認可境界確認で作成した代表タスク `taskId=13` は creator 権限で削除済み
- 途中切り分けで残った既知の一時タスク `8 / 9 / 10 / 11` も creator 権限で削除済み
- 修正後再試験で作成した代表タスク `taskId=15` も削除済み
