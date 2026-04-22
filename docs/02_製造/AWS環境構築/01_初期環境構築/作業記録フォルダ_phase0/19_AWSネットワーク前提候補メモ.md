# AWSネットワーク前提候補メモ

## 目的

- 初回 AWS 公開で採用するネットワーク前提を整理する
- VPC、subnet、Security Group、公開範囲の考え方を先に揃える
- セキュリティとコストのバランスが取れた MVP 向け構成を決める

## 前提

- リージョンは `ap-northeast-1` 東京で確定している
- 初回公開構成は `RDS -> Elastic Beanstalk -> S3 / CloudFront` を前提とする
- 予算上限は月額 10,000 円で、固定費はできるだけ抑えたい
- 初回公開では可用性を一定確保しつつ、構成はできるだけシンプルにする

## 判断で重視すること

- RDS を外部公開しない
- frontend 配信を S3 直公開ではなく CloudFront 経由に寄せる
- backend API は利用者から到達できる必要がある
- NAT Gateway の固定費は、初回 MVP ではできるだけ避けたい

## 候補案

### 案A: MVP 向け簡易分離構成

- 専用 VPC を 1 つ作成する
- public subnet を 2 つ作成する
- private subnet を 2 つ作成する
- Elastic Beanstalk / ALB は public subnet 側で動かす
- RDS は private subnet 側に置く
- S3 バケットは Block Public Access を有効化し、CloudFront 経由のみ配信する

#### 利点

- RDS を閉じつつ、NAT Gateway を置かない構成にしやすい
- 初回公開としてはコストと運用のバランスがよい
- subnet と SG の責務が分かりやすい

#### 注意点

- app インスタンスは public subnet 配置になる
- 将来的により厳密な private app 構成へ移行する余地を残す

### 案B: より本番寄りの private app 構成

- 専用 VPC を 1 つ作成する
- public subnet を 2 つ、private app subnet を 2 つ、private db subnet を 2 つ作成する
- ALB は public subnet、app は private app subnet、RDS は private db subnet に置く
- app の外向き通信のために NAT Gateway を使う

#### 利点

- app インスタンスまで private 化できる
- 将来拡張を見据えると構成としてはきれい

#### 注意点

- NAT Gateway の固定費が増える
- 初回 MVP としては構築・運用コストが上がる

## 推奨案

- 初回公開は案Aの `専用 VPC + public 2 subnet + private 2 subnet` を推奨する

## 推奨理由

- RDS を private に置ける最低限の安全性を確保できる
- NAT Gateway を省略しやすく、月額 10,000 円の運用方針と相性がよい
- 初回公開後に private app 構成へ拡張しやすい

## 推奨構成イメージ

### VPC

- `taskflow-prd-vpc`
- CIDR は `10.0.0.0/16` を第一候補とする

### subnet

- public subnet A: `10.0.1.0/24`
- public subnet C: `10.0.2.0/24`
- private subnet A: `10.0.11.0/24`
- private subnet C: `10.0.12.0/24`

### 配置方針

- ALB: public subnet
- Elastic Beanstalk app: public subnet
- RDS: private subnet
- S3: public 開放しない
- CloudFront: internet 公開の入口

## Security Group 方針

### `sg-alb`

- inbound:
  - `80/tcp` from `0.0.0.0/0`
  - `443/tcp` from `0.0.0.0/0`
- outbound:
  - app 用 SG へ許可

### `sg-app`

- inbound:
  - app port のみ `sg-alb` から許可
- outbound:
  - `443/tcp` を外向けに許可
  - `5432/tcp` を `sg-db` 向けに許可

### `sg-db`

- inbound:
  - `5432/tcp` を `sg-app` からのみ許可
- outbound:
  - デフォルトのまま、または最小化

## 公開範囲の方針

- frontend:
  - CloudFront のみ public
  - S3 バケットは direct access を原則禁止
- backend:
  - ALB 経由で public 提供
  - app インスタンスへの直接アクセスは不要
- DB:
  - public access 無効
  - app SG からのみ接続許可

## 初回公開でやらないこと

- NAT Gateway を置いた private app 構成
- bastion サーバー常設
- DB の public 開放
- S3 バケットの public website hosting 前提公開

## 補足

- DB へ直接メンテナンス接続が必要になった場合は、その時だけ別手段を検討する
- HTTPS の詳細は `0.3-6 公開URL・HTTPS・独自ドメイン方針` で確定する
- シークレット配置の詳細は `0.3-7` で確定する

## 今回の判断候補

- 専用 VPC を作る
- subnet は `public 2 + private 2`
- app は public subnet、DB は private subnet
- RDS は public access 無効
- frontend は CloudFront 公開、S3 は direct public を禁止

## 最終決定

- 2026年4月8日、初回公開では専用 VPC を 1 つ作成する方針で確定した
- subnet は `public 2 + private 2` 構成で確定した
- Elastic Beanstalk / ALB は public subnet、RDS は private subnet に置く方針で確定した
- RDS の public access は無効で確定した
- frontend は CloudFront 公開、S3 の direct public access は禁止で確定した
- 初回公開では NAT Gateway を置かない方針で確定した

## 補足

- より厳密な private app 構成は、後続フェーズで必要に応じて検討する
- HTTPS の終端方法や独自ドメインは `0.3-6` で別途確定する

## 参照

- `10_Phase0_Phase1対応状況チェックリスト.md`
- `15_AWSリージョン候補整理メモ.md`
- `16_AWS命名規約・タグ規約候補メモ.md`
- `17_AWS予算上限・停止方針・削除方針候補メモ.md`
