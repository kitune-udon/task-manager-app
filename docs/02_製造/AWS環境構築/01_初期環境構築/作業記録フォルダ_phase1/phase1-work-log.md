# Phase1 Work Log

## 目的

- Phase1 で実施した AWS デプロイ前準備の内容を時系列にまとめる
- 設定分離、方針決定、確認結果を 1 つの作業ログとして残す
- Phase1 完了判定の前提情報を参照しやすくする

## 基本情報

- 作成日: 2026年4月8日
- 作業対象: `backend` / `frontend` / `docs/02_製造/AWSデプロイ`
- 確認した current branch: `develop`

## 実施内容

### 1. Spring Boot 設定分離

- `application-local.yml` を追加
- `application-prod.yml` を追加
- DB / JWT / CORS を環境変数ベースで扱う構成へ整理
- `local` / `prod` プロファイルで backend 起動確認を実施

関連成果物:

- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/01_プロファイル別バックエンド起動確認メモ.md`

### 2. フロントエンド設定分離

- `frontend/.env.local` を作成
- `frontend/.env.production` を作成
- `VITE_API_BASE_URL` の運用方針を整理
- `local` / `production` の build / preview 確認を実施

関連成果物:

- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/02_フロントエンドenv整備メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/03_フロントエンドlocal_production確認メモ.md`

### 3. 添付ファイル機能の本番方針整理

- 初回 AWS 公開では添付機能を `A. 一時停止` として扱う方針を確定
- 後続で再開する場合は S3 前提で進める方針を整理
- `task_attachments` 独立テーブル案と必要項目を整理

関連成果物:

- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/04_添付ファイル本番方針決定メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/05_S3移行前提DB項目整理メモ.md`

### 4. 本番相当確認

- `backend` の `./gradlew test` を実施
- `frontend` の `npm run build` を実施
- `frontend` の `npm run test:e2e` を実施
- Playwright で主要 3 シナリオが通ることを確認

関連成果物:

- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/06_本番相当スモークテスト実施メモ.md`

## 実行確認コマンド

- `cd backend && ./gradlew test`
- `cd frontend && npm run build`
- `cd frontend && npm run test:e2e`

## 現時点の到達点

- Phase1 の主要実装・確認タスクは概ね完了
- 環境変数化、profile 分離、frontend env 分離、添付方針整理、本番相当スモークまで記録済み
- 初回 AWS 公開に向けた Phase1 残件は最終完了判定が中心

## 未了事項

- `1.1-1` のうち、専用作業ブランチ作成は未実施
- `1.4-4 Phase1 完了判定を実施する` は未対応

## 補足

- 現在の current branch は `develop`
- 専用の Phase1 作業ブランチが必要であれば、別途作成判断を行う

## 判定

- `phase1-work-log.md` の作成は完了

## 参照

- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase0/10_Phase0_Phase1対応状況チェックリスト.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/01_プロファイル別バックエンド起動確認メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/02_フロントエンドenv整備メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/03_フロントエンドlocal_production確認メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/04_添付ファイル本番方針決定メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/05_S3移行前提DB項目整理メモ.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase1/06_本番相当スモークテスト実施メモ.md`
