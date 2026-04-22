# Phase3 完了判定メモ

## 結論

- 判定: `条件付きで Phase3 完了`
- 判定日: `2026年4月9日`

Phase3 の主目的だった Elastic Beanstalk への backend 配備、環境プロパティの secrets 参照反映、RDS 接続確認までは完了しており、Phase4 の frontend 公開作業へ進める状態です。
一方で、frontend の公開 URL と backend の最終 HTTPS 公開経路はまだ未確定で、`CORS_ALLOWED_ORIGINS` も未登録です。また、`taskflow-prd-sg-app` の `HTTP/80` 公開は backend 単体確認用の暫定設定です。

## 完了した内容

- `4-1` Phase3 用の証跡フォルダと実行ログを作成
- `4-2` backend の `./gradlew clean test bootJar` を実行し、配備可能 JAR を生成
- `4-3` Elastic Beanstalk 用 source bundle と `Procfile` を作成
- `4-4` Parameter Store の ARN / type / SecureString 状況を確定
- `4-5` Elastic Beanstalk 用 service role / ec2 role / instance profile / SSM 読み取りポリシーを作成
- `4-6` app / db Security Group を backend 単体確認向けに確認
- `4-7` Elastic Beanstalk アプリケーションとアプリケーションバージョンを作成
- `4-8` Single instance の Elastic Beanstalk Environment を作成
- `4-9` plain text 環境変数と secrets 参照を反映
- `4-10` 「イベント」で deploy 成功を確認
- `4-11` `/actuator/health`、認証 API、`/api/users`、EB ログで RDS 接続と業務 API 動作を確認

## 主要な作成済みリソース

### Elastic Beanstalk

- Application: `taskflow-prd-eb-app`
- Environment: `Taskflow-prd-eb-app-env`
- Environment ID: `e-gvcyudrrcs`
- CNAME: `Taskflow-prd-eb-app-env.eba-8xzqpp6j.ap-northeast-1.elasticbeanstalk.com`
- Platform: `Corretto 17 running on 64bit Amazon Linux 2023 / 4.11.0`
- Running version label: `taskflow-prd-backend-20260409-1415-8d4d377`
- Source bundle file: `backend/deploy/eb/taskflow-prd-backend-20260409-1255-8d4d377.zip`

### Runtime 設定

- plain text:
  - `SPRING_PROFILES_ACTIVE=prod`
  - `SERVER_PORT=5000`
- SSM secrets:
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `JWT_SECRET`
  - `JWT_EXPIRATION_MILLIS`
- 未設定:
  - `CORS_ALLOWED_ORIGINS`

### IAM

- service role: `aws-elasticbeanstalk-service-role`
- ec2 role: `aws-elasticbeanstalk-ec2-role`
- instance profile: `aws-elasticbeanstalk-ec2-role`
- custom policy: `taskflow-prd-eb-ssm-read-policy`

### 検証結果

- `/actuator/health` は `200`、`status=UP`、`db=UP`
- `POST /api/auth/register` は `201`
- `POST /api/auth/login` は `200`
- `GET /api/users` は `200`
- EB ログで PostgreSQL 接続、Flyway `v3` までの migration、Tomcat `5000` 起動を確認

## Phase3 の残課題

- `/taskflow/prd/backend/CORS_ALLOWED_ORIGINS`
  - frontend の公開 URL 確定後に登録
- frontend から利用する最終 HTTPS API URL
  - Elastic Beanstalk の backend 単体確認用 URL をそのまま最終公開 URL とするか未確定
- `taskflow-prd-sg-app`
  - `HTTP / 80 / 0.0.0.0/0` は Phase3 backend verification 用の暫定設定
- CLI 用 IAM user
  - `taskflow-cli-operator` の access key は Phase4 完了後に削除または権限縮小を検討

## Phase4 への引き継ぎ

- frontend URL 確定後に `CORS_ALLOWED_ORIGINS` を登録する
- frontend から利用する最終 HTTPS API URL を確定する
- backend 単体確認用 URL と最終公開 URL を混同しない
- frontend 側の本番 API 接続先設定と、CloudFront / S3 公開 URL の対応を記録する
- frontend 疎通確認後に `taskflow-prd-sg-app` の一時的な `HTTP/80` 公開を見直す
- Phase4 で不要になったら `taskflow-cli-operator` の access key を削除する

## 参照資料

- `docs/02_製造/AWSデプロイ/作業手順_phase3.docx`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase3/phase3_execution_log.md`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase2/01_Phase2完了判定メモ.md`
