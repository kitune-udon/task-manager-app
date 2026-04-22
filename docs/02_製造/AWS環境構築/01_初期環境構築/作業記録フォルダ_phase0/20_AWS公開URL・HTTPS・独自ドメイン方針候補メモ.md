# AWS公開URL・HTTPS・独自ドメイン方針候補メモ

## 目的

- 初回 AWS 公開で採用する URL と HTTPS の方針を決める
- 独自ドメインを最初から使うか、AWS 標準ドメインで始めるかを整理する
- Phase2 以降の実装順に迷わないよう、公開入口の方針を固定する

## 前提

- リージョンは `ap-northeast-1` 東京で確定している
- frontend は `S3 / CloudFront`、backend は `Elastic Beanstalk` で公開する前提とする
- 初回公開は MVP であり、まずは早く・安全に公開することを優先する
- 独自ドメインは必須要件としてはまだ固定されていない

## 比較観点

- 初回公開までの作業量
- HTTPS をどこまで素直に設定できるか
- 運用開始後に独自ドメインへ移行しやすいか
- DNS / 証明書まわりの説明コスト

## 候補案

### 案A: 初回は AWS 標準ドメインで公開する

#### frontend

- CloudFront の標準ドメイン `xxxxx.cloudfront.net` を使う
- HTTPS は CloudFront 標準ドメインで利用する

#### backend

- Elastic Beanstalk の標準ドメインを使う
- API は frontend から backend 標準ドメインへ接続する

#### 利点

- 初回公開が最もシンプル
- 独自ドメイン取得や DNS 切替がなくても開始できる
- ACM 証明書や Route 53 の前提がなくても進めやすい

#### 注意点

- 利用者向け URL としては見栄えが弱い
- 後から独自ドメインへ差し替える作業は別途必要

### 案B: frontend だけ独自ドメイン、backend は標準ドメイン

#### frontend

- CloudFront に独自ドメインを割り当てる
- ACM 証明書と DNS 設定を行う

#### backend

- Elastic Beanstalk 標準ドメインをそのまま使う

#### 利点

- 利用者が触る URL は見栄えを整えやすい
- backend は標準ドメインのままなので、全面導入よりは軽い

#### 注意点

- frontend 用の DNS / 証明書設定は初回から必要
- API ドメインだけ標準のまま残る

### 案C: frontend / backend ともに独自ドメインで開始する

#### 利点

- URL 設計としては最もきれい
- 公開後の見栄えと説明は分かりやすい

#### 注意点

- CloudFront 用 ACM 証明書、DNS、backend 側公開名などの整理が必要
- 初回公開の作業量と切り分けポイントが増える

## 推奨案

- 初回公開は案Aの `AWS 標準ドメインで開始` を推奨する

## 推奨理由

- 初回 MVP 公開では、まずアプリを安定して公開することを優先したい
- 標準ドメインなら HTTPS を比較的シンプルに扱える
- 独自ドメインは後から CloudFront 側へ段階導入しやすい

## HTTPS 方針

### frontend

- CloudFront の標準ドメインを利用する
- viewer protocol policy は HTTPS redirect 相当を採用する

### backend

- 初回は Elastic Beanstalk 標準ドメインで開始する
- frontend 側からの API 接続先は HTTPS URL を使う前提で整理する
- もし backend 側で HTTPS 終端の扱いが複雑になる場合は、公開順序の中で追加確認する

## 独自ドメインを後から導入する場合の前提

- CloudFront に alternate domain name を追加する
- そのドメインをカバーする ACM 証明書が必要
- CloudFront の独自ドメイン用証明書は `us-east-1` で管理する必要がある
- DNS プロバイダで CNAME または alias 相当の設定が必要

## 今回の判断候補

- 初回公開 URL: AWS 標準ドメイン
- frontend 公開: CloudFront 標準ドメイン + HTTPS
- backend 公開: Elastic Beanstalk 標準ドメイン
- 独自ドメイン: 初回公開後の後続対応

## 最終決定

- 2026年4月8日、初回公開 URL は AWS 標準ドメインで開始する方針で確定した
- frontend は CloudFront 標準ドメイン + HTTPS で公開する方針で確定した
- backend は Elastic Beanstalk 標準ドメインで公開する方針で確定した
- 独自ドメインは初回公開後の後続対応とする方針で確定した

## 補足

- 独自ドメインが必要になった時点で、CloudFront 側に段階導入する
- backend 側の公開名を独自ドメイン化するかは、その時点で再判断する

## 参考

- CloudFront alternate domain names: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/CNAMEs.html
- CloudFront custom domain and HTTPS: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/cnames-and-https-procedures.html
- CloudFront custom certificate region note: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/using-https-alternate-domain-names.html

## 参照

- `10_Phase0_Phase1対応状況チェックリスト.md`
- `19_AWSネットワーク前提候補メモ.md`
