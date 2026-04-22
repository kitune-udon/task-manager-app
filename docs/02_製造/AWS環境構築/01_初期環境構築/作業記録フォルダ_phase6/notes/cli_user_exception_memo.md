# CLI User Exception Memo

## 対象

- IAM user: `taskflow-cli-operator`

## 現状

- attached policy:
  - `AdministratorAccess`

## 例外として残す理由

- Phase6 時点では、Phase3〜Phase6 の AWS 作業で使った操作範囲が広く、直近の Phase でも CLI 継続利用が見込まれる
- この時点で急いで最小権限化すると、次フェーズ作業の停止や切り分けコスト増につながる可能性がある

## リスク

- `AdministratorAccess` は権限が広く、誤操作時の影響範囲が大きい
- access key が残るため、保管・利用手順を誤るとリスクが高い

## 期限

- `2026-04-30` までに最小権限化方針を確定し、可能なら customer managed policy へ移行する

## 担当

- `未記入`

## 後続対応

- Phase5 / Phase6 / 次フェーズで実際に使った AWS CLI コマンドを棚卸しする
- `sts / s3 / cloudfront / elasticbeanstalk / rds / iam / ssm / ec2` の必要範囲を整理する
- `AdministratorAccess` を外し、必要最小限の customer managed policy へ置き換える
