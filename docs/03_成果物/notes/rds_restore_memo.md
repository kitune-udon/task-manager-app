# RDS Restore Memo

## 対象

- DB instance: `taskflow-prd-rds`
- endpoint: `taskflow-prd-rds.c5smu4yyoz4b.ap-northeast-1.rds.amazonaws.com:5432`

## Point-in-time restore

- AWS コンソール:
  - `RDS > データベース > taskflow-prd-rds > アクション > 特定時点への復元`
- CLI:
  - `aws rds restore-db-instance-to-point-in-time`

## 手動スナップショットからの復元

- AWS コンソール:
  - `RDS > スナップショット > 対象 snapshot > スナップショットを復元`
- CLI:
  - `aws rds restore-db-instance-from-db-snapshot`

## 注意事項

- 既存の本番 DB `taskflow-prd-rds` を直接上書きしない
- 復元時は新しい `DB instance identifier` を使う
- 復元後に接続確認、migration 状態確認、アプリ側接続先切替の要否を確認する
- 緊急時は `CloudFront / Elastic Beanstalk / RDS status / LatestRestorableTime` の順で状態を確認してから復旧方式を決める
