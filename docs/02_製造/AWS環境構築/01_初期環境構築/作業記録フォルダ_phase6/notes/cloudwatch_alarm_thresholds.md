# CloudWatch Alarm Thresholds Memo

## 目的

`taskflow-prd` 本番環境で作成した `CloudWatch Alarm` のしきい値、対象メトリクス、通知先を 1 か所で確認できるようにする。

## SNS 通知先

- `ap-northeast-1`
  - topic: `taskflow-prd-alerts-apne1`
  - protocol: `Email`
  - endpoint: `whgd0765@gmail.com`
- `us-east-1`
  - topic: `taskflow-prd-alerts-use1`
  - protocol: `Email`
  - endpoint: `whgd0765@gmail.com`

## Alarm 一覧

| Region | Alarm Name | Namespace / Metric | Target | Condition | Period | Datapoints | Action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `ap-northeast-1` | `taskflow-prd-eb-environment-health` | `AWS/ElasticBeanstalk / EnvironmentHealth` | `Taskflow-prd-eb-app-env` | `< 15` | `1 minute` | `1/1` | `taskflow-prd-alerts-apne1` |
| `ap-northeast-1` | `taskflow-prd-rds-cpu-high` | `AWS/RDS / CPUUtilization` | `taskflow-prd-rds` | `> 80` | `5 minutes` | `1/1` | `taskflow-prd-alerts-apne1` |
| `ap-northeast-1` | `taskflow-prd-rds-storage-low` | `AWS/RDS / FreeStorageSpace` | `taskflow-prd-rds` | `< 5368709120` (`5 GiB`) | `5 minutes` | `1/1` | `taskflow-prd-alerts-apne1` |
| `ap-northeast-1` | `taskflow-prd-rds-memory-low` | `AWS/RDS / FreeableMemory` | `taskflow-prd-rds` | `< 134217728` (`128 MiB`) | `5 minutes` | `1/1` | `taskflow-prd-alerts-apne1` |
| `us-east-1` | `taskflow-prd-cf-5xx-rate-high` | `AWS/CloudFront / 5xxErrorRate` | `E688SH91TX10P` | `> 1` | `5 minutes` | `1/1` | `taskflow-prd-alerts-use1` |

## しきい値メモ

### Elastic Beanstalk

- `EnvironmentHealth` は Enhanced Health の数値を利用する
- 実運用上の目安:
  - `Green = 25`
  - `Yellow = 20`
  - `Red = 15`
  - `Grey = 0`
- 今回は過検知を避けるため、`< 15` のときだけ通知する
- 作成直後に `データ不足` になるのは通常動作

### RDS

- `CPUUtilization`
  - `> 80` を通知閾値とした
  - 一時的な負荷上昇より、継続的な高負荷の検知を優先する
- `FreeStorageSpace`
  - `5 GiB` 未満を通知閾値とした
  - 現在は `20 GiB` 開始 + storage autoscaling `100 GiB` 上限のため、逼迫手前の保守的通知として設定
- `FreeableMemory`
  - `256 MiB` では現在値に近すぎたため、`128 MiB` 未満へ調整した
  - 現在のグラフ水準を見て、即時 `ALARM` 化しない値に合わせた

### CloudFront

- `5xxErrorRate > 1` を通知閾値とした
- `CloudFront` のメトリクス / アラームは `us-east-1` で扱う
- 直近に `5xx` が無い場合、作成時のグラフが `データがありません` になることがある

## 今後の見直し候補

- `CloudWatch Alarm` に `OK` / `INSUFFICIENT_DATA` 通知も付けるか
- `eventId` 単位の `Metric Filter / Alarm` を追加するか
- `CloudFront 4xxErrorRate` や `RDS DatabaseConnections` も監視対象へ広げるか
