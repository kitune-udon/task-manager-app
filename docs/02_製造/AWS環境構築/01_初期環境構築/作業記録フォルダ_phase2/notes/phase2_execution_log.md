# Phase2 Execution Log

## 基本情報

- 作業日: 2026年4月9日
- 作業者: `未記入`
- AWS アカウント: `359429618625`
- リージョン: `ap-northeast-1`
- 対象 Phase: `Phase2`
- 参照手順書: `docs/02_製造/AWSデプロイ/作業手順_phase2.docx`

## Phase2 で固定して使う値

| 項目 | 内容 |
| --- | --- |
| AWS リージョン | `ap-northeast-1` |
| 環境コード | `prd` |
| VPC 名 | `taskflow-prd-vpc` |
| VPC CIDR | `10.0.0.0/16` |
| public subnet A | `taskflow-prd-subnet-public-a / 10.0.1.0/24` |
| public subnet C | `taskflow-prd-subnet-public-c / 10.0.2.0/24` |
| private subnet A | `taskflow-prd-subnet-private-a / 10.0.11.0/24` |
| private subnet C | `taskflow-prd-subnet-private-c / 10.0.12.0/24` |
| app Security Group | `taskflow-prd-sg-app` |
| db Security Group | `taskflow-prd-sg-db` |
| DB subnet group | `taskflow-prd-rds-subnet` |
| RDS identifier | `taskflow-prd-rds` |
| Initial database name | `taskflow` |
| Parameter Store prefix | `/taskflow/prd/backend/` |

## 証跡フォルダ

- screenshots: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase2/screenshots`
- notes: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase2/notes`
- exports: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase2/exports`

## 実行ログ

### 2.0 作業用の証跡フォルダと記録ファイルを作成する

- 実施日時: 2026年4月9日
- 結果: 完了
- メモ:
  - Phase2 フォルダを初期化済み
  - `screenshots` / `notes` / `exports` を作成済み
  - 本ファイルを実行ログとして作成済み

### 2.1 AWS コンソールのリージョンと権限前提を確認する

- 実施日時: `2026年4月9日`
- リージョン確認: `ap-northeast-1 を確認済み`
- VPC コンソールアクセス: `可`
- RDS コンソールアクセス: `可`
- Systems Manager コンソールアクセス: `可`
- IAM コンソールアクセス: `可`
- Billing / Cost Management アクセス: `未記入`
- メモ:
  - 右上リージョン選択のスクリーンショットで `東京 ap-northeast-1` を確認
  - VPC / RDS / Systems Manager / IAM の各コンソール画面へ遷移できることを確認
  - IAM はグローバルサービスのため、画面上は `グローバル` 表示で問題なし

## 以降の記録欄

### 2.2 VPC 作成

- VPC ID: `vpc-0c923d9f3616e4f65`
- スクリーンショット: `VPC 詳細画面確認済み`
- メモ:
  - VPC 名は `taskflow-prd-vpc`
  - 状態は `Available`
  - IPv4 CIDR は `10.0.0.0/16`
  - `DNS 解決` は有効
  - `DNS ホスト名` は有効化済み

### 2.3 subnet 作成

- public subnet A ID: `subnet-0620ef37ddb586208`
- public subnet C ID: `subnet-02cc8b52e22d96aec`
- private subnet A ID: `subnet-0142da2df4d49ac04`
- private subnet C ID: `subnet-0e7ac4e833ccd98e9`
- スクリーンショット: `サブネット一覧画面確認済み`
- メモ:
  - 4 件とも状態は `Available`
  - CIDR は `10.0.1.0/24`、`10.0.2.0/24`、`10.0.11.0/24`、`10.0.12.0/24` で想定どおり
  - `taskflow-prd-subnet-public-a` の名称は想定どおり `taskflow-prd-subnet-public-a`
  - `パブリック IPv4 アドレスを自動割り当て` は public 2 件が `はい`、private 2 件が `いいえ`

### 2.4 IGW / public route table

- IGW ID: `igw-02fa72bad69103922`
- public route table ID: `rtb-0d41db5d78c712f94`
- スクリーンショット: `IGW 一覧 / route / subnet 関連付け確認済み`
- メモ:
  - `taskflow-prd-igw` は `taskflow-prd-vpc` に `Attached`
  - `taskflow-prd-rt-public` に `0.0.0.0/0 -> igw-02fa72bad69103922` が設定済み
  - `taskflow-prd-subnet-public-a` と `taskflow-prd-subnet-public-c` が明示関連付け済み
  - `taskflow-prd-subnet-private-a` と `taskflow-prd-subnet-private-c` は明示関連付けなしで、public route table に紐づいていない

### 2.5 Security Group 作成

- app SG ID: `sg-0e79604022597200d`
- db SG ID: `sg-06bfddce58a3b9cec`
- スクリーンショット: `SG 一覧 + sg-db inbound 確認済み`
- メモ:
  - `taskflow-prd-sg-app` は `vpc-0c923d9f3616e4f65` 上に作成済み
  - `taskflow-prd-sg-db` は `vpc-0c923d9f3616e4f65` 上に作成済み
  - `taskflow-prd-sg-db` の inbound は `PostgreSQL / TCP / 5432 / source=taskflow-prd-sg-app` の 1 本のみ
  - `0.0.0.0/0` や自分の IP からの 5432 許可は入っていない

### 2.6 タグ適用

- 結果: `完了`
- スクリーンショット: `VPC / route table / IGW / subnet / SG のタグ反映画面確認済み`
- メモ:
  - 以下リソースへ共通タグを反映済み
    - `taskflow-prd-vpc`
    - `taskflow-prd-rt-public`
    - `taskflow-prd-igw`
    - `taskflow-prd-subnet-public-a`
    - `taskflow-prd-subnet-public-c`
    - `taskflow-prd-subnet-private-a`
    - `taskflow-prd-subnet-private-c`
    - `taskflow-prd-sg-app`
    - `taskflow-prd-sg-db`
  - 共通タグの内容
    - `Project=taskflow`
    - `System=task-manager-app`
    - `Environment=prd`
    - `Region=ap-northeast-1`
    - `Scope=mvp`
    - `ManagedBy=manual`
  - 各リソースにはそれぞれの `Name` タグも設定済み

### 2.7 Parameter Store 登録

- 登録済みキー:
  - `/taskflow/prd/backend/JWT_SECRET`: `作成済み`
  - `/taskflow/prd/backend/JWT_EXPIRATION_MILLIS`: `3600000`
  - `/taskflow/prd/backend/CORS_ALLOWED_ORIGINS`: `frontend 公開 URL 確定後`
  - `/taskflow/prd/backend/DB_USERNAME`: `taskapp`
  - `/taskflow/prd/backend/DB_PASSWORD`: `作成済み`
  - `/taskflow/prd/backend/DB_URL`: `jdbc:postgresql://taskflow-prd-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com:5432/taskflow`
- スクリーンショット: `DB_USERNAME / DB_PASSWORD / JWT_SECRET / JWT_EXPIRATION_MILLIS / DB_URL 確認済み`
- メモ:
  - `DB_USERNAME` は `String` で登録済み
  - `DB_PASSWORD` は `SecureString` で登録済み
  - `JWT_SECRET` は `SecureString` で登録済み
  - `JWT_EXPIRATION_MILLIS` は `String` で登録済み
  - `DB_URL` は RDS endpoint 確定後に登録済み
  - `CORS_ALLOWED_ORIGINS` は frontend 公開 URL 確定後に登録予定

### 2.8 RDS 入力値確定

- Master username: `taskapp`
- DB instance class: `db.t4g.micro`
- Backup retention: `1日`
- Deletion protection: `有効`
- スクリーンショット: `RDS 作成画面確認済み`
- メモ:
  - エンジンは `PostgreSQL 15.17-R1`
  - DB インスタンス識別子は `taskflow-prd-rds`
  - ストレージは `gp3 / 20 GiB`
  - ストレージ自動スケーリングは `100 GiB`
  - VPC は `taskflow-prd-vpc`
  - DB subnet group は `taskflow-prd-rds-subnet`
  - VPC セキュリティグループは `taskflow-prd-sg-db`
  - `Public access` は `なし`
  - 初期データベース名は `taskflow`
  - 削除保護は `有効`
  - 無料利用枠制約によりバックアップ保持期間は `1日` で作成

### 2.9 DB subnet group 作成

- DB subnet group: `taskflow-prd-rds-subnet`
- スクリーンショット: `DB subnet group 詳細画面確認済み`
- メモ:
  - VPC は `vpc-0c923d9f3616e4f65`
  - 説明は `private subnets for taskflow prd rds`
  - 含まれる subnet は `taskflow-prd-subnet-private-a` と `taskflow-prd-subnet-private-c`

### 2.10 RDS 作成

- RDS status: `利用可能`
- Endpoint: `taskflow-prd-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com`
- Port: `5432`
- ARN: `未記入`
- スクリーンショット: `RDS 詳細画面 / endpoint 確認済み`
- メモ:
  - DB 識別子は `taskflow-prd-rds`
  - エンジンは `PostgreSQL`
  - クラスは `db.t4g.micro`
  - AZ は `ap-northeast-1a`
  - `taskflow-prd-sg-db` が関連付け済み
  - RDS 作成後に `DB_URL` を Parameter Store へ登録済み
