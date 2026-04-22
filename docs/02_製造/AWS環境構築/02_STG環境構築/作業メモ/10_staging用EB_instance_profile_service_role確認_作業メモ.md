# 10_staging用EB_instance_profile_service_role確認 作業メモ

## 更新日時

- 2026-04-19

## 対象

- 10. staging 用 Elastic Beanstalk instance profile / service role を確認する

## 確認結果

- production で使っている instance profile
  - instance profile 名: `aws-elasticbeanstalk-ec2-role`
  - role 名: `aws-elasticbeanstalk-ec2-role`
- attached policies
  - `AWSElasticBeanstalkMulticontainerDocker`
  - `AWSElasticBeanstalkWebTier`
  - `AWSElasticBeanstalkWorkerTier`
  - `taskflow-prd-eb-ssm-read-policy`
- `taskflow-prd-eb-ssm-read-policy` は `/taskflow/prd/backend/*` のみ許可
- staging 専用の instance profile は未作成

## service role 確認結果

- 既存 service role: `aws-elasticbeanstalk-service-role`
- trust policy
  - principal: `elasticbeanstalk.amazonaws.com`
- attached policies
  - `AWSElasticBeanstalkEnhancedHealth`
  - `AWSElasticBeanstalkManagedUpdatesCustomerRolePolicy`
- staging でも service role として流用可能と判断

## 判断

- 現在の `aws-elasticbeanstalk-ec2-role` をそのまま staging へ流用すると、prd / stg の secret 参照権限が混ざる
- 手順書の分離方針に合わせるため、staging 用 EC2 role / instance profile を新規作成する方針にする
- service role は app secret を持たないため、まずは既存 `aws-elasticbeanstalk-service-role` を流用する

## staging 用に作成するもの

- EC2 role 名: `taskflow-stg-eb-ec2-role`
- instance profile 名: `taskflow-stg-eb-ec2-role`
- custom policy 名: `taskflow-stg-eb-ssm-read-policy`

## staging EC2 role に付与する policy

- AWS managed
  - `AWSElasticBeanstalkMulticontainerDocker`
  - `AWSElasticBeanstalkWebTier`
  - `AWSElasticBeanstalkWorkerTier`
- custom
  - `taskflow-stg-eb-ssm-read-policy`

## テンプレートファイル

- `.github/workflows/elastic_beanstalk_ec2_ssm_read_policy_stg.json`

## 次に確認すること

- `taskflow-stg-eb-ec2-role` が作成されているか
- 同名 instance profile が作成されているか
- `taskflow-stg-eb-ssm-read-policy` が付与されているか
- Elastic Beanstalk 環境作成時に `taskflow-stg-eb-ec2-role` を指定できるか
- service role に `aws-elasticbeanstalk-service-role` を指定できるか

## 確認結果

- 実施済み
- staging EC2 role
  - role 名: `taskflow-stg-eb-ec2-role`
  - role ARN: `arn:aws:iam::359429618625:role/taskflow-stg-eb-ec2-role`
- staging instance profile
  - instance profile 名: `taskflow-stg-eb-ec2-role`
  - instance profile ARN: `arn:aws:iam::359429618625:instance-profile/taskflow-stg-eb-ec2-role`
- attached policies
  - `AWSElasticBeanstalkMulticontainerDocker`
  - `AWSElasticBeanstalkWebTier`
  - `AWSElasticBeanstalkWorkerTier`
  - `taskflow-stg-eb-ssm-read-policy`
- inline policies
  - なし
- service role
  - `aws-elasticbeanstalk-service-role` を後続の EB 環境作成で利用する方針

## 証跡

- `aws iam get-instance-profile --instance-profile-name taskflow-stg-eb-ec2-role`
- `aws iam list-attached-role-policies --role-name taskflow-stg-eb-ec2-role`
- `aws iam list-role-policies --role-name taskflow-stg-eb-ec2-role`

## 完了判定

- 手順書 10 の完了条件を満たした
  - staging 用 instance profile が Parameter Store を参照できる状態になった
  - staging 用 service role として `aws-elasticbeanstalk-service-role` を利用する前提が確認できた
