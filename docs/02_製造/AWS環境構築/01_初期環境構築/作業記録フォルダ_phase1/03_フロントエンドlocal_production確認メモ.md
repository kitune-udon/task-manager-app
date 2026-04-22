# フロントエンド local / production 確認メモ

## 目的

- frontend が `local` と `production` の両方の設定で起動できることを確認する
- `VITE_API_BASE_URL` の切替が build / preview に反映されることを確認する

## 実施日時

- 2026年4月8日

## local 確認

- 実行コマンド:
  - `npm run dev -- --host 127.0.0.1 --port 4173`
- 確認内容:
  - Vite dev server が起動すること
  - `/login` が 200 を返すこと
- 結果:
  - 起動成功
  - `http://127.0.0.1:4173/login` で `HTTP/1.1 200 OK`

## production 確認

- 実行コマンド:
  - `npm run build`
  - `npm run preview -- --host 127.0.0.1 --port 4174`
- 確認内容:
  - production build が成功すること
  - preview server が起動すること
  - `/login` が 200 を返すこと
  - build 成果物に `.env.production` の `VITE_API_BASE_URL` が埋め込まれていること
- 結果:
  - build 成功
  - preview 起動成功
  - `http://127.0.0.1:4174/login` で `HTTP/1.1 200 OK`
  - `frontend/dist/assets/...js` 内に以下を確認
    - `https://replace-me-with-backend-url.example.com`
    - `http://localhost:8080`

## 補足

- production 確認は、`frontend/.env.production` の placeholder を使って build / preview まで確認した
- 本番 backend の実 URL は未確定のため、production build での実 API 疎通までは未確認
- `http://localhost:8080` は fallback 用文字列として bundle に残るが、production 用の placeholder も同時に埋め込まれていることを確認した

## 判定

- `1.2-4 フロントを local / production それぞれで確認する` は対応済み

## 参照

- `frontend/.env.local`
- `frontend/.env.production`
- `frontend/src/lib/apiClient.ts`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/02_フロントエンドenv整備メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase0/10_Phase0_Phase1対応状況チェックリスト.md`
