# Phase3 Execution Log

## 基本情報

- 作業日: 2026年4月9日
- 作業者: `未記入`
- AWS アカウント: `359429618625`
- リージョン: `ap-northeast-1`
- 対象 Phase: `Phase3`
- 参照手順書: `docs/02_製造/AWSデプロイ/作業手順_phase3.docx`

## Phase3 で固定して使う値

| 項目 | 内容 |
| --- | --- |
| AWS アカウント | `359429618625` |
| リージョン | `ap-northeast-1` |
| 命名規約 | `taskflow-<env>-<resource> / env=prd` |
| VPC | `taskflow-prd-vpc / vpc-0c923d9f3616e4f65 / 10.0.0.0/16` |
| public subnet | `taskflow-prd-subnet-public-a / subnet-0620ef37ddb586208 / 10.0.1.0/24` / `taskflow-prd-subnet-public-c / subnet-02cc8b52e22d96aec / 10.0.2.0/24` |
| private subnet | `taskflow-prd-subnet-private-a / subnet-0142da2df4d49ac04 / 10.0.11.0/24` / `taskflow-prd-subnet-private-c / subnet-0e7ac4e833ccd98e9 / 10.0.12.0/24` |
| Security Group | `taskflow-prd-sg-app / sg-0e79604022597200d` / `taskflow-prd-sg-db / sg-06bfddce58a3b9cec` |
| RDS | `taskflow-prd-rds / PostgreSQL / taskflow-prd-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com / 5432 / taskflow` |
| Parameter Store prefix | `/taskflow/prd/backend/` |
| 作成済み parameter | `DB_URL / DB_USERNAME / DB_PASSWORD / JWT_SECRET / JWT_EXPIRATION_MILLIS` |
| 未登録 parameter | `CORS_ALLOWED_ORIGINS` |
| Elastic Beanstalk Application | `taskflow-prd-eb-app` |
| Elastic Beanstalk Environment | `taskflow-prd-eb-env` |

## 証跡フォルダ

- screenshots: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/screenshots`
- notes: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/notes`
- exports: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/exports`

## 実行ログ

### 4-1 Phase3 用の証跡フォルダと実行ログを作成する

- 実施日時: `2026-04-09 12:49:44 JST`
- 結果: `完了`
- 作成したフォルダ:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/screenshots`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/notes`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/exports`
- phase3_execution_log.md 作成場所:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/phase3_execution_log.md`
- メモ:
  - Phase3 用の証跡フォルダを新規作成
  - 手順書 2 章の固定値を冒頭へ転記
  - `CORS_ALLOWED_ORIGINS` は未登録のまま記録

## 以降の記録欄

### 4-2 ローカルの backend 成果物を作成できる状態か確認する

- 実施日時: `2026-04-09 12:49-12:53 JST`
- ブランチ名: `develop`
- コミット ID: `8d4d377`
- Java version:
  - `openjdk version "17.0.14" 2025-01-21`
  - `OpenJDK Runtime Environment Homebrew (build 17.0.14+0)`
  - `OpenJDK 64-Bit Server VM Homebrew (build 17.0.14+0, mixed mode, sharing)`
- 実行コマンド: `./gradlew clean test bootJar`
- build 結果: `成功`
- 生成 JAR:
  - `backend/build/libs/task-0.0.1-SNAPSHOT.jar`
- application-prod.yml 確認結果:
  - `server.port` は `${SERVER_PORT:8080}` で環境変数上書き可能
  - `spring.config.activate.on-profile=prod` を使用
- 環境変数参照の確認結果:
  - `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` は `application.yml` で参照
  - `JWT_SECRET` / `JWT_EXPIRATION_MILLIS` は `application.yml` で参照
  - `CORS_ALLOWED_ORIGINS` は `application.yml` で参照
- 差分メモ:
  - `application-prod.yml` 単体には DB / JWT / CORS の定義はなく、共通設定の `application.yml` 側で環境変数を読む構成
  - prod プロファイルでは `SERVER_PORT` と `ddl-auto=validate`、`jackson.time-zone=Asia/Tokyo` を上書きしている

### 4-3 配備用 source bundle を作成する

- 実施日時: `2026-04-09 12:54-12:55 JST`
- deploy 作業フォルダ:
  - `backend/deploy/eb`
- 配備対象 JAR:
  - コピー元: `backend/build/libs/task-0.0.1-SNAPSHOT.jar`
  - コピー先: `backend/deploy/eb/app.jar`
- Procfile 内容:
  - `web: java -jar app.jar`
- ZIP ファイル名:
  - `backend/deploy/eb/taskflow-prd-backend-20260409-1255-8d4d377.zip`
- ZIP 解凍確認結果:
  - 最上位に `app.jar` と `Procfile` が存在
  - 親フォルダを含まない構成を確認
- メモ:
  - `deploy/eb` は新規作成で、古い ZIP / JAR は混在なし
  - 初回は手順書どおり最小構成とし、Procfile に余計な JVM オプションは追加していない

### 4-4 Parameter Store の ARN を収集し、環境変数の割当表を作る

- 実施日時: `2026-04-09 12:56-13:52 JST`
- 結果: `完了`
- CLI / 認証状況:
  - `aws-cli/2.34.27` を Homebrew でインストール
  - `aws configure` 実施後、`aws sts get-caller-identity` で `arn:aws:iam::359429618625:user/taskflow-cli-operator` を確認
  - `region=ap-northeast-1` を `~/.aws/config` で確認
- 環境変数割当表:
  - `DB_URL`
    - path: `/taskflow/prd/backend/DB_URL`
    - ARN: `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_URL`
    - type: `String`
    - KMS: `なし`
  - `DB_USERNAME`
    - path: `/taskflow/prd/backend/DB_USERNAME`
    - ARN: `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_USERNAME`
    - type: `String`
    - KMS: `なし`
  - `DB_PASSWORD`
    - path: `/taskflow/prd/backend/DB_PASSWORD`
    - ARN: `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_PASSWORD`
    - type: `SecureString`
    - KMS: `alias/aws/ssm`
  - `JWT_SECRET`
    - path: `/taskflow/prd/backend/JWT_SECRET`
    - ARN: `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/JWT_SECRET`
    - type: `SecureString`
    - KMS: `alias/aws/ssm`
  - `JWT_EXPIRATION_MILLIS`
    - path: `/taskflow/prd/backend/JWT_EXPIRATION_MILLIS`
    - ARN: `arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/JWT_EXPIRATION_MILLIS`
    - type: `String`
    - KMS: `なし`
  - `CORS_ALLOWED_ORIGINS`
    - path: `/taskflow/prd/backend/CORS_ALLOWED_ORIGINS`
    - ARN: `未登録`
    - type: `未登録`
    - メモ: frontend 公開 URL 確定後に登録
- 取得コマンド:
  - `aws ssm describe-parameters --parameter-filters 'Key=Name,Option=BeginsWith,Values=/taskflow/prd/backend/' --query 'Parameters[].{Name:Name,ARN:ARN,Type:Type,KeyId:KeyId}' --output json`
- 後続作業への影響:
  - 4-5 の SSM 読み取りポリシー Resource には上記 ARN を利用可能
  - `DB_PASSWORD` / `JWT_SECRET` は `alias/aws/ssm` のため、今回の SSM 読み取りポリシーには追加の `kms:Decrypt` を含めない

### 4-5 Elastic Beanstalk 用 IAM ロールを確認・作成する

- 実施日時: `2026-04-09 13:20-13:56 JST`
- 結果: `完了`
- CLI 実行ユーザー:
  - `arn:aws:iam::359429618625:user/taskflow-cli-operator`
- 作成した trust policy:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/exports/aws-elasticbeanstalk-service-role-trust-policy.json`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/exports/aws-elasticbeanstalk-ec2-role-trust-policy.json`
- 作成した custom policy:
  - policy name: `taskflow-prd-eb-ssm-read-policy`
  - policy ARN: `arn:aws:iam::359429618625:policy/taskflow-prd-eb-ssm-read-policy`
  - policy JSON: `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/exports/taskflow-prd-eb-ssm-read-policy.json`
- service role:
  - role name: `aws-elasticbeanstalk-service-role`
  - role ARN: `arn:aws:iam::359429618625:role/aws-elasticbeanstalk-service-role`
  - attached policies:
    - `arn:aws:iam::aws:policy/service-role/AWSElasticBeanstalkEnhancedHealth`
    - `arn:aws:iam::aws:policy/AWSElasticBeanstalkManagedUpdatesCustomerRolePolicy`
- ec2 role:
  - role name: `aws-elasticbeanstalk-ec2-role`
  - role ARN: `arn:aws:iam::359429618625:role/aws-elasticbeanstalk-ec2-role`
  - attached policies:
    - `arn:aws:iam::aws:policy/AWSElasticBeanstalkWebTier`
    - `arn:aws:iam::aws:policy/AWSElasticBeanstalkWorkerTier`
    - `arn:aws:iam::aws:policy/AWSElasticBeanstalkMulticontainerDocker`
    - `arn:aws:iam::359429618625:policy/taskflow-prd-eb-ssm-read-policy`
- instance profile:
  - profile name: `aws-elasticbeanstalk-ec2-role`
  - profile ARN: `arn:aws:iam::359429618625:instance-profile/aws-elasticbeanstalk-ec2-role`
  - attached role: `aws-elasticbeanstalk-ec2-role`
- 実行コマンドメモ:
  - `aws iam create-role`
  - `aws iam create-policy`
  - `aws iam create-instance-profile`
  - `aws iam attach-role-policy`
  - `aws iam add-role-to-instance-profile`
- 補足:
  - 初回の `AWSElasticBeanstalkManagedUpdatesCustomerRolePolicy` 付与は誤った ARN `...:policy/service-role/...` で失敗
  - 正しい ARN `arn:aws:iam::aws:policy/AWSElasticBeanstalkManagedUpdatesCustomerRolePolicy` で再実行して成功

### 4-6 app Security Group の受信ルールを backend 単体確認向けに調整する

- 実施日時: `2026-04-09 14:09 JST`
- 結果: `完了`
- sg-app 確認結果:
  - group name: `taskflow-prd-sg-app`
  - group id: `sg-0e79604022597200d`
  - inbound rule:
    - `HTTP / TCP / 80 / 0.0.0.0/0`
    - description: `temporary for phase3 backend verification`
- sg-db 確認結果:
  - group name: `taskflow-prd-sg-db`
  - group id: `sg-06bfddec58a3b9cec`
  - inbound rule:
    - `PostgreSQL / TCP / 5432 / source=sg-0e79604022597200d (taskflow-prd-sg-app)`
  - `0.0.0.0/0` で `5432` を許可するルールはなし
- メモ:
  - Web コンソール上の作業完了報告を受領後、AWS CLI で実測確認
  - 旧メモにあった db SG ID `sg-06bfddce58a3b9cec` は誤記で、実際の ID は `sg-06bfddec58a3b9cec`

### 4-7 Elastic Beanstalk アプリケーション と初回アプリケーションバージョンを作成する

- 実施日時: `2026-04-09 14:15 JST`
- 結果: `完了`
- Application 名:
  - `taskflow-prd-eb-app`
- Java platform:
  - `Corretto 17 running on 64bit Amazon Linux 2023 / 4.11.0`
- アプリケーションバージョン:
  - version label: `taskflow-prd-backend-20260409-1415-8d4d377`
- メモ:
  - 初回に `taskflow-prd-backend-20260409-1255-8d4d377` を指定した際は `already exists` で失敗
  - `Name` タグは予約済みキーのため削除して再実行
  - 再作成後、上記 version label で進行

### 4-8 Single instance の Elastic Beanstalk Environment を作成する

- 実施日時: `2026-04-09 14:52-14:56 JST`
- 結果: `完了`
- 作成開始を確認:
  - application name: `taskflow-prd-eb-app`
  - environment name: `Taskflow-prd-eb-app-env`
  - environment id: `e-gvcyudrrcs`
  - platform: `Corretto 17 running on 64bit Amazon Linux 2023 / 4.11.0`
  - running version: `taskflow-prd-backend-20260409-1415-8d4d377`
- 起動完了確認:
  - status: `Ready`
  - health: `Green`
  - health status: `Ok`
  - CNAME: `Taskflow-prd-eb-app-env.eba-8xzqpp6j.ap-northeast-1.elasticbeanstalk.com`
- 「イベント」初期状態:
  - `createEnvironment is starting.`
  - `Using elasticbeanstalk-ap-northeast-1-359429618625 as Amazon S3 storage bucket for environment data.`
  - `Created security group named: sg-0e83d125126940b93`
  - `Environment health has transitioned to Pending. Initialization in progress...`
  - `Created EIP: 13.113.189.119`
  - `Waiting for EC2 instances to launch. This may take a few minutes.`
- 成功イベント:
  - `Instance deployment used the commands in your 'Procfile' to initiate startup of your application.`
  - `Instance deployment completed successfully.`
  - `Environment health has transitioned from Pending to Ok. Initialization completed 12 seconds ago and took 2 minutes.`
  - `Application available at Taskflow-prd-eb-app-env.eba-8xzqpp6j.ap-northeast-1.elasticbeanstalk.com.`
  - `Successfully launched environment: Taskflow-prd-eb-app-env`
- メモ:
  - Web コンソール上で環境作成開始を確認
  - 手順書の想定名 `taskflow-prd-eb-env` ではなく、実際には `Taskflow-prd-eb-app-env` で作成開始

### 4-9 環境プロパティの secrets 参照と plain text 環境変数を設定する

- 実施日時: `2026-04-09 15:14-15:16 JST`
- 結果: `完了`
- plain text 環境変数:
  - `SPRING_PROFILES_ACTIVE=prod`
  - `SERVER_PORT=5000`
- SSM Parameter Store 参照:
  - `DB_URL=arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_URL`
  - `DB_USERNAME=arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_USERNAME`
  - `DB_PASSWORD=arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/DB_PASSWORD`
  - `JWT_SECRET=arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/JWT_SECRET`
  - `JWT_EXPIRATION_MILLIS=arn:aws:ssm:ap-northeast-1:359429618625:parameter/taskflow/prd/backend/JWT_EXPIRATION_MILLIS`
- 未設定:
  - `CORS_ALLOWED_ORIGINS`
- メモ:
  - CLI 確認では Elastic Beanstalk 既定の `GRADLE_HOME` / `M2` / `M2_HOME` も同居
  - `aws:elasticbeanstalk:application:environmentsecrets` に 5 件の ARN が設定済み

### 4-10 「イベント」を確認し、environment 作成と deploy の成否を判定する

- 実施日時: `2026-04-09 15:16 JST`
- 結果: `完了`
- 成功イベント:
  - `Environment update is starting.`
  - `Updating environment Taskflow-prd-eb-app-env's configuration settings.`
  - `Instance deployment used the commands in your 'Procfile' to initiate startup of your application.`
  - `Instance deployment completed successfully.`
  - `Successfully deployed new configuration to environment.`
  - `Environment update completed successfully.`
  - `Environment health has transitioned from Info to Ok. Configuration update completed 53 seconds ago and took 78 seconds.`
- 判定:
  - environment 作成成功
  - environment 変数 / SSM secrets 反映成功
  - 現時点の health は `Ok`

### 4-11 「ヘルス」、ログ、業務 API で RDS 接続を確認する

- 実施日時: `2026-04-09 15:22-15:27 JST`
- 結果: `完了`
- `/actuator/health` 確認:
  - URL: `http://Taskflow-prd-eb-app-env.eba-8xzqpp6j.ap-northeast-1.elasticbeanstalk.com/actuator/health`
  - status code: `200`
  - body 要点:
    - `status=UP`
    - `components.db.status=UP`
    - `components.db.details.database=PostgreSQL`
- 業務 API 確認:
  - register:
    - endpoint: `POST /api/auth/register`
    - status code: `201`
    - test email: `phase3-check-20260409152733@example.com`
    - response id: `3`
  - login:
    - endpoint: `POST /api/auth/login`
    - status code: `200`
    - token prefix: `eyJhbGciOiJIUzM4NCJ9`
  - users:
    - endpoint: `GET /api/users`
    - status code: `200`
    - returned count: `3`
    - 先頭ユーザー email: `phase3-check-20260409152107@example.com`
- Elastic Beanstalk ログ確認:
  - bundle 取得先: `/tmp/phase3-eb-bundle.zip`
  - 展開先: `/tmp/phase3-eb-bundle`
  - 成功ログ:
    - `HikariPool-1 - Added connection ...`
    - `Database: jdbc:postgresql://taskflow-prd-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com:5432/taskflow (PostgreSQL 15.17)`
    - `Successfully applied 3 migrations to schema "public", now at version v3`
    - `Tomcat started on port 5000 (http) with context path '/'`
    - `Started TaskApplication in 14.767 seconds`
  - 補足ログ:
    - 環境変数反映前の起動試行では `Connection to localhost:5432 refused` が残っていた
    - 最新の成功ログでは RDS 接続、Flyway migration、Tomcat 起動まで完了しており、現時点の異常ではない
- 判定:
  - `/actuator/health` の `db=UP` により RDS 接続を確認
  - 外部 URL 経由の `register -> login -> /api/users` 成功により backend の業務 API と DB 永続化を確認
  - EB ログ上でも PostgreSQL 接続と migration 完了を確認

### 4-12 Phase3 完了判定と Phase4 への引き継ぎを記録する

- 実施日時: `2026-04-09 15:30-15:33 JST`
- 結果: `完了`
- Phase3 完了判定:
  - `条件付きで Phase3 完了`
- 条件付き完了の理由:
  - `CORS_ALLOWED_ORIGINS` が未設定
  - frontend から利用する最終 HTTPS API URL が未確定
  - `taskflow-prd-sg-app` の `HTTP / 80 / 0.0.0.0/0` は backend 単体確認用の暫定設定
- Phase4 への引き継ぎ:
  - frontend URL 確定後に `CORS_ALLOWED_ORIGINS` を登録する
  - frontend から利用する最終 HTTPS API URL を確定する
  - backend 単体確認用 URL と最終公開 URL を混同しない
  - frontend 側の本番 API 接続先設定と CloudFront / S3 公開 URL の対応を記録する
  - frontend 疎通確認後に `taskflow-prd-sg-app` の一時的な `HTTP/80` 公開を見直す
  - `taskflow-cli-operator` の access key は Phase4 完了後に削除または権限縮小を検討する
- 成果物チェック:
  - 完了判定メモ:
    - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/01_Phase3完了判定メモ.md`
  - 実行ログ:
    - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/phase3_execution_log.md`
  - 検証レスポンス:
    - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/exports/phase3-health-response.json`
    - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/exports/phase3-register-response.json`
    - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/exports/phase3-login-response.json`
    - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/exports/phase3-users-response.json`
  - Elastic Beanstalk log bundle:
    - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/exports/phase3-eb-log-bundle.zip`
- 主要スクリーンショット一覧:
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/screenshots/ap-northeast-1.console.aws.amazon.com_elasticbeanstalk_home_region=ap-northeast-1.png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/screenshots/ap-northeast-1.console.aws.amazon.com_elasticbeanstalk_home_region=ap-northeast-1 (1).png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/screenshots/ap-northeast-1.console.aws.amazon.com_elasticbeanstalk_home_region=ap-northeast-1 (2).png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/screenshots/ap-northeast-1.console.aws.amazon.com_elasticbeanstalk_home_region=ap-northeast-1 (3).png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/screenshots/ap-northeast-1.console.aws.amazon.com_elasticbeanstalk_home_region=ap-northeast-1 (4).png`
  - `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/screenshots/ap-northeast-1.console.aws.amazon.com_elasticbeanstalk_home_region=ap-northeast-1 (5).png`
