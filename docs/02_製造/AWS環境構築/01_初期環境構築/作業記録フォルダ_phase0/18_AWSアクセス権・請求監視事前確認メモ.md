# AWSアクセス権・請求監視事前確認メモ

## 目的

- AWS デプロイ着手前に、必要なアクセス権があるかを整理する
- 請求監視を有効化できる前提が揃っているかを確認する
- `root が必要な確認` と `管理者権限で確認できる項目` を分けて、確認漏れを防ぐ

## 前提

- リージョンは `ap-northeast-1` 東京で確定している
- 初回公開構成は `RDS -> Elastic Beanstalk -> S3 / CloudFront` を前提とする
- 予算監視は AWS Budgets と Cost Anomaly Detection を利用する前提とする

## 確認したいこと

- AWS Console にサインインできるか
- デプロイに必要な主要サービスの操作権限があるか
- Billing / Cost Management の参照と設定変更ができるか
- 請求アラート通知先を設定できるか

## root ユーザーでの確認が必要な項目

| ID | 項目 | 確認内容 | 期待状態 | 確認結果 |
| --- | --- | --- | --- | --- |
| ACC-01 | Billing コンソール IAM アクセス | `IAM User and Role Access to Billing Information` が有効か | 有効 | ユーザー申告ベースで有効化済み |
| ACC-02 | 請求先アカウント把握 | ルートログイン情報またはルート保有者が明確か | 明確 | root ユーザー本人 |

## 管理者権限で確認したい項目

| ID | サービス | 確認内容 | 期待状態 | 確認結果 |
| --- | --- | --- | --- | --- |
| ACC-03 | Elastic Beanstalk | Application / Environment の作成・更新・削除ができる | 可能 | 管理画面へアクセス可能 |
| ACC-04 | RDS | DB 作成、subnet group 作成、snapshot 取得ができる | 可能 | 管理画面へアクセス可能 |
| ACC-05 | S3 | バケット作成、オブジェクト配置、ポリシー設定ができる | 可能 | 管理画面へアクセス可能 |
| ACC-06 | CloudFront | Distribution 作成・更新ができる | 可能 | 管理画面へアクセス可能 |
| ACC-07 | IAM | Role / Policy の参照と必要に応じた作成ができる | 可能 | 管理画面へアクセス可能 |
| ACC-08 | VPC / Security Group | Security Group 作成・変更ができる | 可能 | 管理画面へアクセス可能 |
| ACC-09 | CloudWatch / Logs | ログ参照、アラーム参照ができる | 可能 | 管理画面へアクセス可能とみなす |
| ACC-10 | ACM | HTTPS 化が必要になった場合に証明書管理ができる | 可能または担当者明確 | root / 管理者権限前提で対応可能見込み |

## 請求監視の事前確認項目

| ID | 項目 | 確認内容 | 期待状態 | 確認結果 |
| --- | --- | --- | --- | --- |
| BILL-01 | Cost Explorer | 有効化済みか | 有効 | 実画面確認は未実施、後続設定時に確認 |
| BILL-02 | AWS Budgets | Budget 作成権限があるか | 可能 | root / 管理者権限前提で設定可能見込み |
| BILL-03 | Cost Anomaly Detection | 利用開始できるか | 可能 | root / 管理者権限前提で設定可能見込み |
| BILL-04 | 通知先メール | 予算通知を受けるメールアドレスが決まっているか | 決定済み | プライベートメールアドレスを使用予定 |
| BILL-05 | 請求確認担当 | 請求アラート受領後に確認する担当者が決まっているか | 決定済み | 現時点では root ユーザー本人を一次担当とする |

## 2026年4月8日時点の確認結果まとめ

- root ユーザー本人であることを確認済み
- Billing の `IAM User and Role Access to Billing Information` は、日本語画面上で有効化した認識
- Elastic Beanstalk / RDS / S3 / CloudFront / IAM / VPC の各管理画面へアクセス可能
- 請求通知先メールアドレスは、プライベートで使用しているメールアドレスを採用予定
- Cost Explorer / AWS Budgets / Cost Anomaly Detection の実作成は未実施だが、Phase0 の事前確認としては大きなブロッカーなしと判断する

## 今回の推奨確認順

1. root で `Billing への IAM アクセス` が有効か確認する
2. 管理者権限ユーザーで Elastic Beanstalk / RDS / S3 / CloudFront / IAM / VPC を触れるか確認する
3. Cost Explorer が有効か確認する
4. AWS Budgets と Cost Anomaly Detection を設定できるか確認する
5. 通知先メールアドレスと請求確認担当を決める

## このタスクで判定したいこと

- `0.3-4` を完了にできる条件は以下
- `ACC-01` から `ACC-10` のうち、今回必要なサービス権限が揃っている
- `BILL-01` から `BILL-05` のうち、少なくとも設定可否と通知先方針が明確になっている

## 判定

- 2026年4月8日時点で `0.3-4` は対応済みとする
- 理由:
  - root ユーザー本人での確認が取れている
  - 主要サービスの管理画面へアクセスできる
  - Billing IAM アクセスの有効化を実施した認識がある
  - 通知先メールアドレスと一次担当の方針が置けている

## ブロッカーになりやすい点

- root ユーザーで Billing の IAM アクセスが有効化されていない
- Cost Explorer が未有効で、Cost Anomaly Detection に進めない
- デプロイ実施者に RDS / Elastic Beanstalk / IAM の作成権限がない
- 請求通知の受け先が未決定

## ユーザー確認が必要な項目

- root ユーザーまたは root 保有者へ連絡できるか
- 現在の AWS ログインユーザーが管理者相当の権限を持っているか
- 請求通知を受けるメールアドレスをどこにするか
- 請求確認の一次担当を誰にするか

## 参考

- Billing console access: https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/control-access-billing.html
- Billing IAM access overview: https://docs.aws.amazon.com/cost-management/latest/userguide/control-access-billing.html
- Billing permissions reference: https://docs.aws.amazon.com/cost-management/latest/userguide/billing-permissions-ref.html
- Cost Anomaly Detection setup: https://docs.aws.amazon.com/cost-management/latest/userguide/settingup-ad.html
