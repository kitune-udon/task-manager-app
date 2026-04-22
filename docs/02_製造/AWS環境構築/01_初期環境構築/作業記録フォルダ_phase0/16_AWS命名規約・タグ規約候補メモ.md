# AWS命名規約・タグ規約候補メモ

## 目的

- AWS リソース作成時の命名ゆれを防ぐ
- 初回公開後に、画面上や請求画面で対象リソースを追いやすくする
- MVP 段階でも、後続フェーズへ引き継ぎやすい最低限のルールを決める

## 前提

- リージョンは `ap-northeast-1` 東京で確定している
- 初回公開は MVP スコープで、まずは 1 環境中心で構築する
- 構成は `RDS -> Elastic Beanstalk -> S3 / CloudFront` を前提とする
- リポジトリ名は `task-manager-app`、画面上のサービス名は `TaskFlow`

## 命名規約で重視すること

- リソース名だけで用途が分かる
- 長すぎず、AWS 画面で見切れにくい
- 後から `dev / stg / prod` を増やしても破綻しにくい
- 手作業で作る場面でも迷いにくい

## 候補案

### 案A: サービス名ベース

- 形式: `taskflow-<env>-<resource>`
- 例:
  - `taskflow-prd-eb-app`
  - `taskflow-prd-eb-env`
  - `taskflow-prd-rds`
  - `taskflow-prd-static`
  - `taskflow-prd-cf`

#### 利点

- 短くて見やすい
- UI 上のサービス名と揃うので把握しやすい
- MVP の小規模構成では十分実用的

#### 注意点

- リポジトリ名やシステム名との完全一致はしない

### 案B: リポジトリ名ベース

- 形式: `task-manager-app-<env>-<resource>`
- 例:
  - `task-manager-app-prd-eb-app`
  - `task-manager-app-prd-eb-env`
  - `task-manager-app-prd-rds`

#### 利点

- GitHub リポジトリ名とそのまま揃えられる
- システム識別としてはぶれにくい

#### 注意点

- 名前が長くなりやすい
- AWS 画面上で一覧したときに見づらくなりやすい

## 推奨案

- 命名規約は案Aの `taskflow-<env>-<resource>` を推奨する

## 推奨理由

- 初回公開では、短くて識別しやすい名前のほうが運用しやすい
- 画面上のサービス名 `TaskFlow` と対応し、認知コストが低い
- タグ側で `System=task-manager-app` を持たせれば、リポジトリ名との対応も補える

## 環境コード

- `local`: ローカル確認用
- `dev`: 開発環境
- `stg`: ステージング環境
- `prd`: 本番環境

## リソース種別の推奨表記

| 用途 | 推奨サフィックス例 |
| --- | --- |
| Elastic Beanstalk application | `eb-app` |
| Elastic Beanstalk environment | `eb-env` |
| RDS instance | `rds` |
| RDS subnet group | `rds-subnet` |
| RDS parameter group | `rds-param` |
| S3 静的配信バケット | `static` |
| CloudFront distribution | `cf` |
| IAM role | `role-<purpose>` |
| Security Group | `sg-<purpose>` |

## 本タスクでの命名例

- `taskflow-prd-eb-app`
- `taskflow-prd-eb-env`
- `taskflow-prd-rds`
- `taskflow-prd-static`
- `taskflow-prd-cf`
- `taskflow-prd-sg-app`
- `taskflow-prd-sg-db`

## タグ規約

### 必須タグ

| キー | 値の例 | 目的 |
| --- | --- | --- |
| `Project` | `taskflow` | プロジェクト単位の識別 |
| `System` | `task-manager-app` | リポジトリ/システム名との対応 |
| `Environment` | `prd` | 環境識別 |
| `Region` | `ap-northeast-1` | リージョン識別 |
| `Scope` | `mvp` | 初回公開範囲の識別 |
| `ManagedBy` | `manual` | 作成主体の識別 |

### 任意タグ

| キー | 値の例 | 用途 |
| --- | --- | --- |
| `Owner` | `team-taskflow` | 管理責任の明確化 |
| `Purpose` | `frontend-static-hosting` | 用途の補足 |
| `Phase` | `phase2` | WBS/工程との対応 |

## 推奨ルール

- リソース名は英小文字、ハイフン区切りで統一する
- 環境コードは `prd` を使い、`prod` と混在させない
- タグの `Project` は `taskflow`、`System` は `task-manager-app` で固定する
- MVP 初回公開では、必須タグだけでも必ず付与する

## 今回の判断候補

- 命名規約: 案A `taskflow-<env>-<resource>`
- 必須タグ: `Project / System / Environment / Region / Scope / ManagedBy`
- 任意タグ: `Owner / Purpose / Phase`

## 最終決定

- 2026年4月8日、命名規約は `taskflow-<env>-<resource>` で確定した
- 必須タグは `Project / System / Environment / Region / Scope / ManagedBy` で確定した
- 任意タグは `Owner / Purpose / Phase` を採用候補として保持し、必要に応じて付与する

## 補足

- 初回 AWS デプロイでは、少なくとも必須タグは全リソースに付与する
- `Environment` は `prd` を正式値として使い、`prod` と混在させない

## 参照

- `10_Phase0_Phase1対応状況チェックリスト.md`
- `15_AWSリージョン候補整理メモ.md`
