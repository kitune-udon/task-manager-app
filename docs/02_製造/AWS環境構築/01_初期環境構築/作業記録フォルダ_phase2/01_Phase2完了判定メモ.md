# Phase2 完了判定メモ

## 結論

- 判定: `条件付きで Phase2 完了`
- 判定日: `2026年4月9日`

Phase2 の主目的だった AWS 基盤の初期準備は完了しており、Phase3 の backend デプロイ準備へ進める状態にあります。
一方で、frontend の公開 URL が未確定のため `CORS_ALLOWED_ORIGINS` は未登録です。また、作業途中で `DB_PASSWORD` と `JWT_SECRET` の復号値を画面表示しているため、現在の値をそのまま公開環境で使う場合はローテーションを推奨します。

## 完了した内容

- `2.0` 証跡フォルダと実行ログを作成
- `2.1` リージョンと主要 AWS サービスへのアクセス確認
- `2.2` VPC `taskflow-prd-vpc` を作成
- `2.3` public 2 / private 2 の subnet を作成
- `2.4` IGW と public route table を作成し、public subnet のみ関連付け
- `2.5` `taskflow-prd-sg-app` / `taskflow-prd-sg-db` を作成
- `2.6` VPC / subnet / IGW / route table / SG へ共通タグを反映
- `2.7` Parameter Store へ backend 用の主要キーを登録
- `2.8` RDS 入力値を確定
- `2.9` DB subnet group `taskflow-prd-rds-subnet` を作成
- `2.10` RDS `taskflow-prd-rds` を作成し、`DB_URL` を登録

## 主要な作成済みリソース

### Network

- VPC: `taskflow-prd-vpc`
  - VPC ID: `vpc-0c923d9f3616e4f65`
  - CIDR: `10.0.0.0/16`
- public subnet A: `taskflow-prd-subnet-public-a`
  - subnet ID: `subnet-0620ef37ddb586208`
  - CIDR: `10.0.1.0/24`
- public subnet C: `taskflow-prd-subnet-public-c`
  - subnet ID: `subnet-02cc8b52e22d96aec`
  - CIDR: `10.0.2.0/24`
- private subnet A: `taskflow-prd-subnet-private-a`
  - subnet ID: `subnet-0142da2df4d49ac04`
  - CIDR: `10.0.11.0/24`
- private subnet C: `taskflow-prd-subnet-private-c`
  - subnet ID: `subnet-0e7ac4e833ccd98e9`
  - CIDR: `10.0.12.0/24`
- Internet Gateway: `taskflow-prd-igw`
  - IGW ID: `igw-02fa72bad69103922`
- public route table: `taskflow-prd-rt-public`
  - route table ID: `rtb-0d41db5d78c712f94`

### Security Group

- app SG: `taskflow-prd-sg-app`
  - SG ID: `sg-0e79604022597200d`
- db SG: `taskflow-prd-sg-db`
  - SG ID: `sg-06bfddce58a3b9cec`
  - inbound: `PostgreSQL / 5432 / source=taskflow-prd-sg-app`

### RDS

- DB subnet group: `taskflow-prd-rds-subnet`
- RDS identifier: `taskflow-prd-rds`
- endpoint: `taskflow-prd-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com`
- port: `5432`
- DB name: `taskflow`
- status: `利用可能`
- deletion protection: `有効`

### Parameter Store

- `/taskflow/prd/backend/DB_USERNAME`
- `/taskflow/prd/backend/DB_PASSWORD`
- `/taskflow/prd/backend/JWT_SECRET`
- `/taskflow/prd/backend/JWT_EXPIRATION_MILLIS`
- `/taskflow/prd/backend/DB_URL`

## Phase2 の残課題

- `/taskflow/prd/backend/CORS_ALLOWED_ORIGINS`
  - frontend の公開 URL 確定後に登録
- `DB_PASSWORD` と `JWT_SECRET` のローテーション要否確認
  - 作業途中で復号値を表示しているため、公開前に新しい値へ更新するのが安全
- 実行ログの補足記入
  - `作業者`
  - `Billing / Cost Management アクセス`
  - RDS ARN

## Phase3 への引き継ぎ

- backend 側は、Parameter Store の値を使って Elastic Beanstalk から RDS 接続確認を行う
- frontend の公開 URL が確定したら `CORS_ALLOWED_ORIGINS` を登録する
- backend / frontend の公開前に、必要なら `DB_PASSWORD` と `JWT_SECRET` をローテーションする

## 参照資料

- `docs/02_製造/AWSデプロイ/作業手順_phase2.docx`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase2/notes/phase2_execution_log.md`
