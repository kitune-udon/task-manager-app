# 14_staging用ElasticBeanstalkEnvironment作成 作業メモ

## 更新日時

- 2026-04-20

## 対象

- 14. staging 用 Elastic Beanstalk Environment を作成する

## production 参考構成

- Application: `taskflow-prd-eb-app`
- Environment: `Taskflow-prd-eb-app-env`
- Platform: `Corretto 17 running on 64bit Amazon Linux 2023/4.11.0`
- Environment type: `SingleInstance`
- Instance type: `t3.micro`
- Instance profile: `aws-elasticbeanstalk-ec2-role`
- Service role: `aws-elasticbeanstalk-service-role`
- App SG: `sg-0e79604022597200d`
- Public subnet
  - `subnet-02cc8b52e22d96aec`
  - `subnet-0620ef37ddb586208`

## staging で作成するもの

- Application: `taskflow-stg-eb-app`
- Environment: `Taskflow-stg-eb-app-env`

## staging 設定方針

- Platform: production と同じ
- Environment type: `SingleInstance`
- Instance type: `t3.micro`
- Instance profile: `taskflow-stg-eb-ec2-role`
- Service role: `aws-elasticbeanstalk-service-role`
- VPC: `vpc-0c923d9f3616e4f65`
- Subnets: `subnet-02cc8b52e22d96aec,subnet-0620ef37ddb586208`
- App SG: `sg-0d30ced331019dba9` (`taskflow-stg-sg-app`)
- environment secrets: `/taskflow/stg/backend/*`

## テンプレートファイル

- `.github/workflows/elastic_beanstalk_environment_settings_stg.json`

## 実施結果

- Elastic Beanstalk Application `taskflow-stg-eb-app` を作成した
- Elastic Beanstalk Environment `Taskflow-stg-eb-app-env` を作成した
- 作成後ステータス
  - Status: `Ready`
  - Health: `Green`
  - HealthStatus: `Ok`
  - CNAME: `Taskflow-stg-eb-app-env.eba-tmhj2uhb.ap-northeast-1.elasticbeanstalk.com`
  - VersionLabel: 未設定
- 主要設定確認結果
  - Instance profile: `taskflow-stg-eb-ec2-role`
  - Service role: `arn:aws:iam::359429618625:role/aws-elasticbeanstalk-service-role`
  - VPC: `vpc-0c923d9f3616e4f65`
  - Subnets: `subnet-02cc8b52e22d96aec,subnet-0620ef37ddb586208`
  - Security groups: `sg-0d30ced331019dba9,sg-0c395e9e934d64dff`
  - Environment secrets: `/taskflow/stg/backend/*` の SSM Parameter ARN が設定済み

## GitHub Environment へ反映する値

- `EB_APPLICATION_NAME`: `taskflow-stg-eb-app`
- `EB_ENVIRONMENT_NAME`: `Taskflow-stg-eb-app-env`
- `VERSION_LABEL_PREFIX`: `taskflow-stg-backend`
- `BACKEND_JAR_NAME`: `task-0.0.1-SNAPSHOT.jar`

## 保留事項

- 手順書 13.5 / 13.6 の初回 backend deploy 後確認は、18 章の staging deploy 時に実施する
- `VersionLabel` は初回 backend deploy で設定されるため、現時点では未設定

- 初回 backend deploy 後に health / Flyway / DB 接続確認を行うこと
