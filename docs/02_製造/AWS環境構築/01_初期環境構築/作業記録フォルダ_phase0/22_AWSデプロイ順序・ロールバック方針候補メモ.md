# AWSデプロイ順序・ロールバック方針候補メモ

## 目的

- 初回 AWS デプロイをどの順番で進めるかを固定する
- 障害発生時に、どこまで戻すかの基準を明確にする
- frontend / backend / DB を切り分けながら、安全に初回公開できるようにする

## 前提

- リージョンは `ap-northeast-1` 東京で確定している
- ネットワークは `専用 VPC + public 2 + private 2` で確定している
- frontend は `S3 / CloudFront`、backend は `Elastic Beanstalk`、DB は `RDS` を前提とする
- backend secret は Parameter Store `SecureString` を使う方針で確定している
- 初回公開は AWS 標準ドメインで開始する方針で確定している

## 方針

- 依存関係の下位から順に作る
- 単独で確認できる単位で区切りながら進める
- frontend と backend は、可能な限り個別に戻せるようにする
- DB は削除よりも snapshot を優先する

## 推奨デプロイ順序

### 1. 共有前提の作成

1. VPC / subnet / Security Group を作成する
2. Parameter Store に backend 用 secret を登録する
3. 必要な IAM role / policy を確認または作成する

### 2. DB の作成

4. RDS subnet group / parameter group を作成する
5. RDS を作成する
6. 接続確認と migration 実行前提を確認する

### 3. backend の作成

7. Elastic Beanstalk application を作成する
8. Elastic Beanstalk environment を作成する
9. backend をデプロイする
10. `/actuator/health` と主要 API の疎通を確認する

### 4. frontend の作成

11. S3 バケットを作成する
12. CloudFront distribution を作成する
13. frontend を build して配置する
14. CloudFront 経由で画面が表示できることを確認する

### 5. 総合確認

15. 公開 URL でログイン、一覧、作成、編集、削除を確認する
16. デプロイ後スモークテストを実施する
17. 問題なければ公開継続と判断する

## この順序を推奨する理由

- DB と backend を先に安定させることで、frontend 側の不具合切り分けがしやすい
- frontend は静的配信なので、最後に入れても公開判断を急ぎすぎずに済む
- ネットワークと secret を最初に確定しておくと、後半の設定ブレが減る

## ロールバック方針

### frontend

- 直前の build 成果物へ差し戻す
- 必要なら S3 上の前バージョンへ戻し、CloudFront の配信を確認する
- frontend のみ不具合なら backend / DB は原則そのまま維持する

### backend

- 直前の安定版アプリケーションバージョンへ戻す
- health check と主要 API 疎通を再確認する
- backend のみの問題なら frontend 資産は維持する

### DB

- schema 変更や migration に問題がある場合は、snapshot / 復元戦略を優先する
- RDS を安易に削除しない
- データ破壊リスクがあるときは、まず新規変更を止めて原因を切り分ける

## ロールバック判断の目安

- frontend だけ NG:
  frontend のみ差し戻し
- backend API が NG だが DB は健全:
  backend を差し戻し
- DB migration が NG:
  新規デプロイを止め、snapshot / 復旧方針を優先
- 原因切り分けが難しい:
  frontend -> backend の順に切り戻し、DB は最後まで保全優先

## 初回公開で特に注意する点

- backend の接続先 RDS と secret 名の不一致
- CORS / API Base URL の設定漏れ
- CloudFront 反映待ちによる見かけ上の差異
- RDS の public access を誤って有効にしないこと

## 今回の判断候補

- デプロイ順序は `ネットワーク -> secret -> RDS -> backend -> frontend -> 総合確認`
- ロールバックは `frontend と backend を個別に戻せる形` を基本とする
- DB は削除より snapshot 保全を優先する

## 最終決定

- 2026年4月8日、初回公開のデプロイ順序は `ネットワーク -> secret -> RDS -> backend -> frontend -> 総合確認` で確定した
- frontend と backend は個別に切り戻す方針で確定した
- DB は削除より snapshot 保全を優先する方針で確定した

## 補足

- Phase2 以降の詳細手順では、この順序を具体的な作業手順へ落とし込む
- もし実装段階で service dependency による見直しが必要になった場合も、この方針を基準に最小変更で調整する

## 参照

- `10_Phase0_Phase1対応状況チェックリスト.md`
- `12_デプロイ後スモークテスト項目一覧.md`
- `19_AWSネットワーク前提候補メモ.md`
- `20_AWS公開URL・HTTPS・独自ドメイン方針候補メモ.md`
- `21_AWSシークレット管理方針候補メモ.md`
