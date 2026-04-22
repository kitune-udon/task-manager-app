# 13_staging用RDS作成 作業メモ

## 更新日時

- 2026-04-19

## 対象

- 13. staging 用 RDS を作成する

## production 参考構成

- DB instance identifier: `taskflow-prd-rds`
- DB name: `taskflow`
- Engine: `postgres`
- Engine version: `15.17`
- Instance class: `db.t4g.micro`
- Allocated storage: `20`
- Storage type: `gp3`
- Storage encrypted: `true`
- Publicly accessible: `false`
- Multi-AZ: `false`
- Port: `5432`
- DB subnet group: `taskflow-prd-rds-subnet`
- VPC: `vpc-0c923d9f3616e4f65`
- DB security group: `taskflow-prd-sg-db`
- App security group: `taskflow-prd-sg-app`

## 共有利用するネットワーク

- VPC: `vpc-0c923d9f3616e4f65`
- private subnet
  - `subnet-0142da2df4d49ac04` (`ap-northeast-1a`)
  - `subnet-0e7ac4e833ccd98e9` (`ap-northeast-1c`)

## staging で新規作成するもの

- DB subnet group: `taskflow-stg-rds-subnet`
- app SG: `taskflow-stg-sg-app`
- db SG: `taskflow-stg-sg-db`
- DB instance: `taskflow-stg-rds`

## security group 方針

- `taskflow-stg-sg-app`
  - VPC: `vpc-0c923d9f3616e4f65`
  - 用途: staging Elastic Beanstalk app 用
  - 備考: この step では先に作成だけしてよい
- `taskflow-stg-sg-db`
  - VPC: `vpc-0c923d9f3616e4f65`
  - inbound
    - Type: `PostgreSQL`
    - Port: `5432`
    - Source: `taskflow-stg-sg-app`
  - outbound
    - デフォルトのままで可
- 注意
  - `0.0.0.0/0` を DB inbound に入れない
  - production の `taskflow-prd-sg-db` を使い回さない
  - production には bastion SG からの許可があるが、staging はまず app SG のみに絞る

## RDS 入力値

- Engine: `PostgreSQL`
- Engine version: production と同じ系統
  - 推奨: `15.17` が選べれば合わせる
  - ない場合は `15.x` の最新 patch
- Template: `Dev/Test`
- DB instance identifier: `taskflow-stg-rds`
- Master username: `taskflow_stg`
- Master password: Parameter Store の `/taskflow/stg/backend/DB_PASSWORD` と同じ値
- DB instance class: `db.t4g.micro`
- Storage type: `gp3`
- Allocated storage: `20 GiB`
- Storage encryption: `有効`
- Public access: `No`
- Multi-AZ: `No`
- VPC: `vpc-0c923d9f3616e4f65`
- DB subnet group: `taskflow-stg-rds-subnet`
- VPC security group: `taskflow-stg-sg-db`
- Initial database name: `taskflow`

## 作成後に更新する parameter

- `/taskflow/stg/backend/DB_URL`
  - `jdbc:postgresql://<staging-rds-endpoint>:5432/taskflow`
- `/taskflow/stg/backend/DB_USERNAME`
  - `taskflow_stg`
- `/taskflow/stg/backend/DB_PASSWORD`
  - RDS 作成時に入力した値と同一にする

## 既存有無

- `taskflow-stg-rds` は未作成
- `taskflow-stg-sg-app` は未作成
- `taskflow-stg-sg-db` は未作成

## 証跡

- `aws rds describe-db-instances --db-instance-identifier taskflow-prd-rds`
- `aws rds describe-db-subnet-groups`
- `aws ec2 describe-security-groups`
- `aws ec2 describe-subnets --subnet-ids subnet-0142da2df4d49ac04 subnet-0e7ac4e833ccd98e9`

## 次に確認すること

- `taskflow-stg-sg-app` が作成されているか
- `taskflow-stg-sg-db` が作成されているか
- `taskflow-stg-rds-subnet` が作成されているか
- `taskflow-stg-rds` が `available` になっているか
- `Public access = No` か
- endpoint を使って Parameter Store の DB 系 3 件を更新したか

## 実施結果

- staging security groups
  - `taskflow-stg-sg-app`
    - GroupId: `sg-0d30ced331019dba9`
    - inbound: なし
  - `taskflow-stg-sg-db`
    - GroupId: `sg-04cfeca4a9514bc1c`
    - inbound: `PostgreSQL 5432` from `taskflow-stg-sg-app`
- DB subnet group
  - `taskflow-stg-rds-subnet`
  - subnets
    - `subnet-0142da2df4d49ac04`
    - `subnet-0e7ac4e833ccd98e9`
- RDS
  - DB instance identifier: `taskflow-stg-rds`
  - status: `available`
  - endpoint: `taskflow-stg-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com`
  - engine version: `15.17`
  - master username: `taskflow_stg`
  - Publicly accessible: `false`
  - DB subnet group: `taskflow-stg-rds-subnet`
  - VPC security group: `sg-04cfeca4a9514bc1c`

## Parameter Store 更新結果

- `/taskflow/stg/backend/DB_URL`
  - `jdbc:postgresql://taskflow-stg-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com:5432/taskflow`
- `/taskflow/stg/backend/DB_USERNAME`
  - `taskflow_stg`
- `/taskflow/stg/backend/DB_PASSWORD`
  - RDS 作成時に Parameter Store の値を使用したため整合済み

## 追加証跡

- `aws ec2 describe-security-groups --group-ids sg-04cfeca4a9514bc1c sg-0d30ced331019dba9`
- `aws rds describe-db-subnet-groups --db-subnet-group-name taskflow-stg-rds-subnet`
- `aws rds create-db-instance --db-instance-identifier taskflow-stg-rds ...`
- `aws rds describe-db-instances --db-instance-identifier taskflow-stg-rds`
- `aws ssm put-parameter --name /taskflow/stg/backend/DB_URL --overwrite ...`
- `aws ssm put-parameter --name /taskflow/stg/backend/DB_USERNAME --overwrite ...`

## 完了判定

- 手順書 13 の完了条件を満たした
  - staging 用 RDS が `available` になっている
  - Public access = `No`
  - staging 用 DB 情報が Parameter Store に反映されている
