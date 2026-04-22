# CloudWatch Log Design Alignment

## 目的

- `作業手順_phase6_追補版.docx` に基づき、最新のログ設計と実装内容を AWS 運用手順へ反映する
- CloudWatch Logs 上での確認観点を、旧来のロググループ名中心ではなく `eventId / requestId / errorCode / status` 中心へそろえる

## 参照資料

- `docs/01_設計/11_ログ設計書.xlsx`
- `docs/02_製造/AWSデプロイ/作業手順_phase6_追補版.docx`
- `docs/02_製造/AWSデプロイ/作業記録フォルダ_phase6/01_Phase6完了判定メモ.md`

## 更新履歴

- `2026-04-11`
  - 追補版手順書の `5.1` に従い新規作成
  - 追補版手順書の `5.2` に従い、CloudWatch 上でのログの見え方と確認キーを整理

## 5.2 最新ログ設計の要点整理

### CloudWatch 上での見え方

- `application / security / audit` は物理的なロググループ名ではなく、アプリケーション内の論理ロガー名である
- アプリケーションは構造化 JSON ログを標準出力へ出す
- Elastic Beanstalk では、そのコンソール出力を `instance log streaming` で CloudWatch Logs に転送して確認する
- そのため CloudWatch Logs 上では、専用ロググループ分離よりも `JSON イベントを検索できること` を重視する
- `application / security / audit` ごとに別ロググループを作る作業は、今回の必須対象にしない

### CloudWatch Logs での主な確認キー

- `eventId`
- `requestId`
- `errorCode`
- `status`
- `userId`
- `taskId`

### 運用上の注意

- Spring Boot / Web サーバ / OS の通常ログは root logger 側のプレーンテキストとして混在し得る
- CloudWatch Logs では、プレーンテキストと構造化 JSON を混同せず、`eventId` を持つレコードを優先して確認する
- `LOG-REQ-001` はすべての 2xx 応答で出るわけではない
- `/actuator/health` の `2xx` と `/api/auth-test/**` の `2xx` では `LOG-REQ-001` を出さない
- 上記の access log 欠損は障害ではなく、実装どおりの正常動作として扱う

### 今回の確認方針

- CloudWatch Logs 上では `ロググループ名中心` ではなく `eventId ベース` で確認する
- 代表イベントとして、起動、アクセス、認証、登録、監査、業務エラーの各 eventId を安全な範囲で確認する
- `LOG-SYS-001` や `LOG-APP-004` のような本番で安全に再現しにくいイベントは、無理に再現せず `設計確認のみ` と記録してよい

## 5.6 Logs Insights 保存クエリ

### 1. 全体確認クエリ

- 保存名: `taskflow-prd-all-events`

```sql
fields @timestamp, @message
| parse @message /(?<json>\{.*\})/
| filter json like /"eventId":/
| parse json /"eventId":"(?<eventId>[^"]+)"/
| parse json /"requestId":"(?<requestId>[^"]+)"/
| parse json /"path":"(?<path>[^"]+)"/
| parse json /"method":"(?<method>[^"]+)"/
| parse json /"status":(?<status>\d+)/
| parse json /"errorCode":"(?<errorCode>[^"]+)"/
| parse json /"userId":(?<userId>\d+)/
| parse json /"taskId":(?<taskId>\d+)/
| display @timestamp, eventId, requestId, path, method, status, errorCode, userId, taskId
| sort @timestamp desc
| limit 100
```

### 2. 認証失敗確認クエリ

- 保存名: `taskflow-prd-auth-failures`

```sql
fields @timestamp, @message
| parse @message /(?<json>\{.*\})/
| filter json like /"eventId":/
| parse json /"eventId":"(?<eventId>[^"]+)"/
| parse json /"requestId":"(?<requestId>[^"]+)"/
| parse json /"path":"(?<path>[^"]+)"/
| parse json /"status":(?<status>\d+)/
| parse json /"errorCode":"(?<errorCode>[^"]+)"/
| parse json /"email":"(?<email>[^"]+)"/
| parse json /"ip":"(?<ip>[^"]+)"/
| filter eventId in ["LOG-AUTH-002","LOG-AUTH-003","LOG-AUTH-005","LOG-AUTH-006"]
| display @timestamp, eventId, requestId, path, status, errorCode, email, ip
| sort @timestamp desc
| limit 100
```

### 3. タスク変更監査確認クエリ

- 保存名: `taskflow-prd-task-audit`

```sql
fields @timestamp, @message
| parse @message /(?<json>\{.*\})/
| filter json like /"eventId":/
| parse json /"eventId":"(?<eventId>[^"]+)"/
| parse json /"requestId":"(?<requestId>[^"]+)"/
| parse json /"userId":(?<userId>\d+)/
| parse json /"taskId":(?<taskId>\d+)/
| parse json /"status":(?<status>\d+)/
| parse json /"changedFields":\[(?<changedFields>[^\]]*)\]/
| filter eventId in ["LOG-TASK-001","LOG-TASK-002","LOG-TASK-003"]
| display @timestamp, eventId, requestId, userId, taskId, changedFields, status
| sort @timestamp desc
| limit 100
```

### 補足

- 手順書の元クエリは生 JSON 前提だが、実際の Elastic Beanstalk `web.stdout.log` では syslog 風プレフィックス付きで CloudWatch Logs に格納される
- そのため保存クエリは `@message` から JSON 本文を `parse` で抽出する形へ補正している

## 5.7 LOG-REQ-001 除外条件の運用メモ

- 通常の業務 API リクエストでは `LOG-REQ-001` を確認対象にする
- `/api/auth-test/**` の `2xx` では `LOG-REQ-001` は出ない
- `/actuator/health` の `2xx` でも `LOG-REQ-001` は出ない
- そのため、health check 系や auth-test 系で access log が見えないことだけを理由に障害扱いしない

### 今回の確認結果

- 通常 API では `LOG-REQ-001` を確認済み
- `/api/auth-test/me` は `200` 応答を確認済み
- 同一パスで見つかった `LOG-REQ-001` は `401` であり、`2xx` 除外条件とは矛盾しない
- `/actuator/health` の `2xx` は現行構成では `CloudFront origin-facing only` 制限により外部ブラウザから直接再現しない
- `/actuator/health` の除外は実コードと設計書に基づく設計確認として扱う
