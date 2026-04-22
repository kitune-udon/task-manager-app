# フロントエンド env 整備メモ

## 目的

- frontend の `local` / `production` 用 env ファイルを揃える
- `VITE_API_BASE_URL` の管理方針を明確にする
- 本番デプロイ前に、どの値を差し替える必要があるかを残す

## 実施日時

- 2026年4月8日

## 作成したファイル

### `frontend/.env.local`

- 用途:
  - ローカル開発時の API 接続先
- 設定値:
  - `VITE_API_BASE_URL=http://localhost:8080`

### `frontend/.env.production`

- 用途:
  - 本番 build 時の API 接続先
- 設定値:
  - `VITE_API_BASE_URL=https://replace-me-with-backend-url.example.com`

## 方針

- `frontend/.env.local`
  - ローカル backend を向く既定値として使う
- `frontend/.env.production`
  - 本番 backend の HTTPS URL が確定したら置き換える
- 実値は Git へコミットしない
- サンプル値は `frontend/.env.example` に残す

## 補足

- `frontend/.env.local` と `frontend/.env.production` は `.gitignore` 対象
- `frontend/src/lib/apiClient.ts` は、未設定時でも `http://localhost:8080` にフォールバックする
- 本番デプロイ前に `frontend/.env.production` の placeholder を実 URL へ差し替える必要がある

## 判定

- `1.2-2 .env.local と .env.production の方針を決め、ファイルを作成する` は対応済み

## 参照

- `frontend/.env.example`
- `frontend/src/lib/apiClient.ts`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase0/07_環境依存設定一覧.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase0/10_Phase0_Phase1対応状況チェックリスト.md`
