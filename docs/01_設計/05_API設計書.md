# API設計書

## 改訂履歴

| 版数 | 改訂日 | 改訂内容 | 作成者 |
|---|---|---|---|
| 1.0 | 2026-04-13 | 初版作成 | 佐伯 |
| 1.1 | 2026-04-14 | 成功レスポンス方針を明記 | 佐伯 |
| 1.2 | 2026-04-14 | コメント、添付ファイル、通知、履歴APIを追加 | 佐伯 |

<details>
<summary>1. 文書概要</summary>

## 1. 文書概要

- API方式: REST API
- データ形式: JSON
- 認証方式: JWT Bearer認証
- 作成方針: 実コードおよび設計方針をもとにAPI仕様を整理

---


</details>

<details>
<summary>2. API概要</summary>

## 2. API概要

本システムのAPIは以下で構成される。

### 認証API

| メソッド | パス | 用途 |
|---|---|---|
| POST | `/api/auth/register` | ユーザー登録 |
| POST | `/api/auth/login` | ログイン |

### タスクAPI

| メソッド | パス | 用途 |
|---|---|---|
| POST | `/api/tasks` | タスク作成 |
| GET | `/api/tasks` | タスク一覧取得 |
| GET | `/api/tasks/{taskId}` | タスク詳細取得 |
| PUT | `/api/tasks/{taskId}` | タスク更新 |
| DELETE | `/api/tasks/{taskId}` | タスク削除 |

### ユーザーAPI

| メソッド | パス | 用途 |
|---|---|---|
| GET | `/api/users` | ユーザー一覧取得 |

### コメントAPI

| メソッド | パス | 用途 |
|---|---|---|
| GET | `/api/tasks/{taskId}/comments` | コメント一覧取得 |
| POST | `/api/tasks/{taskId}/comments` | コメント投稿 |
| PUT | `/api/comments/{commentId}` | コメント更新 |
| DELETE | `/api/comments/{commentId}` | コメント削除 |

### 添付ファイルAPI

| メソッド | パス | 用途 |
|---|---|---|
| GET | `/api/tasks/{taskId}/attachments` | 添付ファイル一覧取得 |
| POST | `/api/tasks/{taskId}/attachments` | 添付ファイルアップロード |
| GET | `/api/attachments/{attachmentId}/download` | 添付ファイルダウンロード |
| DELETE | `/api/attachments/{attachmentId}` | 添付ファイル削除 |

### 通知API

| メソッド | パス | 用途 |
|---|---|---|
| GET | `/api/notifications` | 通知一覧取得 |
| GET | `/api/notifications/unread-count` | 未読通知件数取得 |
| PATCH | `/api/notifications/{notificationId}/read` | 通知既読化 |
| PATCH | `/api/notifications/read-all` | 通知一括既読化 |

### 履歴API

| メソッド | パス | 用途 |
|---|---|---|
| GET | `/api/tasks/{taskId}/activities` | タスク別履歴一覧取得 |

---


</details>

<details>
<summary>3. 認証・認可方針</summary>

## 3. 認証・認可方針

### 3.1 公開エンドポイント

以下は認証不要で利用可能。

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /actuator/health`
- `/error`

### 3.2 認証必須エンドポイント

以下はJWTによる認証が必要。

- `/api/tasks/**`
- `/api/users`
- `/api/comments/**`
- `/api/attachments/**`
- `/api/notifications/**`
- `/api/tasks/*/activities`

### 3.3 認証ヘッダー

```http
Authorization: Bearer <JWT>
```

### 3.4 権限制御

#### タスク参照

以下の条件で参照可能。

- 現行実装: タスク作成者またはタスク担当者
- 将来拡張: `ADMIN` 導入時は ADMIN も許可

#### タスク更新

以下の条件で更新可能。

- 現行実装: タスク作成者またはタスク担当者
- 将来拡張: `ADMIN` 導入時は ADMIN も許可

#### タスク削除

以下の条件で削除可能。

- 現行実装: タスク作成者
- 将来拡張: `ADMIN` 導入時は ADMIN も許可

#### コメント参照 / 投稿

以下を満たす場合に利用可能。

- 対象タスクを参照できること

#### コメント更新 / 削除

以下の条件で利用可能。

- 現行実装: コメント投稿者本人
- 将来拡張: `ADMIN` 導入時は ADMIN も許可

#### 添付ファイル一覧 / ダウンロード

以下を満たす場合に利用可能。

- 対象タスクを参照できること

#### 添付ファイルアップロード

以下を満たす場合に利用可能。

- 対象タスクを更新できること

#### 添付ファイル削除

以下の条件で利用可能。

- 現行実装: 添付登録者本人
- 将来拡張: `ADMIN` 導入時は ADMIN も許可

#### 通知参照 / 既読化

以下を満たす場合に利用可能。

- 自分宛の通知であること

#### タスク別履歴参照

以下を満たす場合に利用可能。

- 現行実装: タスク作成者またはタスク担当者
- 将来拡張: `ADMIN` 導入時は ADMIN も許可

---


</details>

<details>
<summary>4. 共通仕様</summary>

## 4. 共通仕様

## 4.1 リクエストヘッダー

| ヘッダー名 | 必須 | 内容 |
|---|---|---|
| Content-Type | POST/PUT/PATCH時は必須 | 原則 `application/json` |
| Authorization | 認証必須APIで必須 | `Bearer <JWT>` |

添付ファイルアップロードAPIでは、`Content-Type` に `multipart/form-data` を使用する。

## 4.2 正常レスポンス

- APIごとに個別DTOを返却する
- 共通ラッパーは使用しない
- `DELETE` はレスポンスボディなし

### 正常レスポンス方針

- 成功時は `success` フラグ付きの共通 envelope ではなく、raw DTO / raw JSON 配列 / ページングレスポンス / ボディなしを返す
- 一覧系はデータ特性に応じて JSON 配列またはページングレスポンスを返す
- 単票系は API ごとの DTO を返す
- 削除系は `204 No Content` を基本とする
- 正常系と異常系で責務を分け、失敗時のみ `ErrorResponse` を共通形式として扱う
- 今後 API を追加する場合も、現時点ではこの方針を踏襲する

### 方針採用理由

- 成功レスポンスまで共通ラッパーへ統一するメリットよりも、DTO をそのまま扱える単純さを優先できるため
- フロント側でレスポンス型を素直に扱いやすく、画面実装やテストの見通しを保ちやすいため
- 成功 envelope の導入は API 契約の横断変更になりやすいため、必要になった時点で別タスクとして判断するため

## 4.3 一覧APIのページング

件数が増えやすい一覧APIでは、ページングレスポンスを返却する。

### 対象API

- `GET /api/tasks/{taskId}/comments`
- `GET /api/notifications`
- `GET /api/tasks/{taskId}/activities`

### クエリパラメータ

| 項目名 | 型 | 必須 | デフォルト | 説明 |
|---|---|---|---|---|
| page | number |  | 0 | ページ番号 |
| size | number |  | 20 | 1ページあたり件数 |

### レスポンス形式

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

## 4.4 エラーレスポンス形式

```json
{
  "timestamp": "2026-04-12T22:00:00+09:00",
  "status": 400,
  "errorCode": "ERR-INPUT-001",
  "message": "入力内容に誤りがあります",
  "details": [
    {
      "field": "title",
      "message": "タイトルを入力してください"
    }
  ],
  "path": "/api/tasks",
  "requestId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

### エラーレスポンス項目

| 項目名 | 型 | 説明 |
|---|---|---|
| timestamp | string | エラー発生日時 |
| status | number | HTTPステータス |
| errorCode | string | アプリケーション固有エラーコード |
| message | string | ユーザー向けメッセージ |
| details | array | 項目単位のエラー詳細 |
| path | string | リクエストパス |
| requestId | string | リクエスト識別子 |

### details 項目

| 項目名 | 型 | 説明 |
|---|---|---|
| field | string | エラー対象項目名 |
| message | string | 項目別エラーメッセージ |

## 4.5 更新競合制御

- タスク更新APIとコメント更新APIは、クライアントが保持する `version` を受け取り、サーバー最新の `version` と比較する楽観ロック方式を採用する。
- リクエストの `version` がサーバー最新値と一致する場合のみ更新する。
- `version` が不一致の場合は `409 Conflict` を返却する。
- タスク更新APIでは `ERR-TASK-007`、コメント更新APIでは `ERR-COMMENT-006` を返却する。
- 対象が削除済みまたは未存在の場合は、競合より優先して `404 ERR-TASK-004` または `404 ERR-COMMENT-002` を返却する。
- `version` は作成時 `0` で開始し、更新成功時に `+1` する。
- 本フェーズでは `Idempotency-Key` は導入しない。

## 4.6 冪等性方針

- `PATCH /api/notifications/{notificationId}/read` は冪等とし、既読済み通知への再実行でも `200 OK` を返す。
- `PATCH /api/notifications/read-all` は冪等とし、未読通知が0件でも `204 No Content` を返す。
- `POST /api/tasks`、`POST /api/tasks/{taskId}/comments`、`POST /api/tasks/{taskId}/attachments` は本フェーズでは冪等化しない。
- 作成系APIは `Idempotency-Key` を受け付けず、画面側の二重送信防止で重複作成を抑止する。

---


</details>

<details>
<summary>5. エラーコード一覧</summary>

## 5. エラーコード一覧

| 区分 | エラーコード | HTTPステータス | メッセージ |
|---|---|---:|---|
| 認証 | ERR-AUTH-001 | 401 | 認証が必要です |
| 認証 | ERR-AUTH-002 | 401 | 認証に失敗しました |
| 認証 | ERR-AUTH-003 | 401 | トークンが不正です |
| 認証 | ERR-AUTH-004 | 401 | セッションの有効期限が切れています |
| 認可 | ERR-AUTH-005 | 403 | 操作権限がありません |
| 入力 | ERR-INPUT-001 | 400 | 入力内容に誤りがあります |
| タスク入力 | ERR-TASK-001 | 400 | 入力内容に誤りがあります |
| タスク入力 | ERR-TASK-002 | 400 | 入力内容に誤りがあります |
| タスク入力 | ERR-TASK-003 | 400 | 入力内容に誤りがあります |
| ユーザー | ERR-USR-001 | 409 | メールアドレスは既に登録されています |
| タスク | ERR-TASK-007 | 409 | 他のユーザーによりタスクが更新されました。最新状態を再読み込みしてください。 |
| コメント | ERR-COMMENT-006 | 409 | 他のユーザーによりコメントが更新されました。最新状態を再読み込みしてください。 |
| ユーザー | ERR-USR-002 | 404 | ユーザーが存在しません |
| タスク | ERR-TASK-004 | 404 | タスクが存在しません |
| タスク | ERR-TASK-005 | 403 | タスク更新権限がありません |
| タスク | ERR-TASK-006 | 403 | タスク削除権限がありません |
| コメント | ERR-COMMENT-001 | 400 | コメント内容を入力してください |
| コメント | ERR-COMMENT-002 | 404 | コメントが存在しません |
| コメント | ERR-COMMENT-003 | 400 | コメント内容が長すぎます |
| コメント | ERR-COMMENT-004 | 403 | コメント更新権限がありません |
| コメント | ERR-COMMENT-005 | 403 | コメント削除権限がありません |
| 添付ファイル | ERR-FILE-001 | 400 | ファイルを選択してください |
| 添付ファイル | ERR-FILE-002 | 404 | 添付ファイルが存在しません |
| 添付ファイル | ERR-FILE-003 | 403 | 添付アップロード権限がありません |
| 添付ファイル | ERR-FILE-004 | 403 | 添付削除権限がありません |
| 添付ファイル | ERR-FILE-005 | 400 | ファイルサイズが上限を超えています |
| 添付ファイル | ERR-FILE-006 | 400 | 許可されていないファイル形式です |
| 添付ファイル | ERR-FILE-007 | 400 | 添付件数が上限を超えています |
| 添付ファイル | ERR-FILE-008 | 400 | 添付ファイル合計サイズが上限を超えています |
| 添付ファイル | ERR-FILE-009 | 500 | ファイルの保存に失敗しました。しばらくしてから再度お試しください。 |
| 添付ファイル | ERR-FILE-010 | 500 | ファイルの取得に失敗しました。しばらくしてから再度お試しください。 |
| 通知 | ERR-NOTIFY-001 | 404 | 通知が存在しません |
| 通知 | ERR-NOTIFY-002 | 403 | 通知参照権限がありません |
| 履歴 | ERR-ACTIVITY-001 | 404 | 履歴が存在しません |
| 履歴 | ERR-ACTIVITY-002 | 403 | 履歴参照権限がありません |
| システム | ERR-SYS-001 | 500 | データの取得または更新に失敗しました。しばらくしてから再度お試しください。 |
| システム | ERR-SYS-999 | 500 | システムエラーが発生しました。しばらくしてから再度お試しください。 |

---


</details>

<details>
<summary>6. API詳細</summary>

# 6. API詳細

## 6.1 認証API

<details>
<summary>6.1.1 ユーザー登録</summary>

## 6.1.1 ユーザー登録

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | ユーザー登録 |
| メソッド | POST |
| パス | `/api/auth/register` |
| 認証 | 不要 |
| リクエスト形式 | JSON |
| レスポンス形式 | JSON |

### リクエストボディ

| 項目名 | 型 | 必須 | 説明 | バリデーション |
|---|---|---|---|---|
| name | string | ○ | ユーザー名 | 未入力不可、100文字以内 |
| email | string | ○ | メールアドレス | 未入力不可、メール形式、255文字以内 |
| password | string | ○ | パスワード | 未入力不可、8〜100文字 |

### リクエスト例

```json
{
  "name": "山田太郎",
  "email": "yamada@example.com",
  "password": "password123"
}
```

### 正常レスポンス

- HTTPステータス: `201 Created`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | ユーザーID |
| name | string | ユーザー名 |
| email | string | メールアドレス |
| role | string | ロール |
| createdAt | string | 作成日時 |

### レスポンス例

```json
{
  "id": 1,
  "name": "山田太郎",
  "email": "yamada@example.com",
  "role": "MEMBER",
  "createdAt": "2026-04-12T21:00:00"
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-INPUT-001 | 入力不正 |
| 409 | ERR-USR-001 | メールアドレス重複 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.1.2 ログイン</summary>

## 6.1.2 ログイン

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | ログイン |
| メソッド | POST |
| パス | `/api/auth/login` |
| 認証 | 不要 |
| リクエスト形式 | JSON |
| レスポンス形式 | JSON |

### リクエストボディ

| 項目名 | 型 | 必須 | 説明 | バリデーション |
|---|---|---|---|---|
| email | string | ○ | メールアドレス | 未入力不可、メール形式、255文字以内 |
| password | string | ○ | パスワード | 未入力不可、8〜100文字 |

### リクエスト例

```json
{
  "email": "yamada@example.com",
  "password": "password123"
}
```

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| token | string | JWT |
| user.id | number | ユーザーID |
| user.name | string | ユーザー名 |
| user.email | string | メールアドレス |
| user.role | string | ロール |

### レスポンス例

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "name": "山田太郎",
    "email": "yamada@example.com",
    "role": "MEMBER"
  }
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-INPUT-001 | 入力不正 |
| 401 | ERR-AUTH-002 | 認証失敗 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

## 6.2 タスクAPI

<details>
<summary>6.2.1 タスク作成</summary>

## 6.2.1 タスク作成

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | タスク作成 |
| メソッド | POST |
| パス | `/api/tasks` |
| 認証 | 必要 |
| リクエスト形式 | JSON |
| レスポンス形式 | JSON |

### リクエストボディ

| 項目名 | 型 | 必須 | 説明 | バリデーション |
|---|---|---|---|---|
| title | string | ○ | タイトル | 未入力不可、100文字以内 |
| description | string |  | 説明 | 5000文字以内 |
| status | string | ○ | ステータス | `TODO` / `DOING` / `DONE` |
| priority | string | ○ | 優先度 | `LOW` / `MEDIUM` / `HIGH` |
| dueDate | string |  | 期限日 | `yyyy-MM-dd` |
| assignedUserId | number |  | 担当ユーザーID | 存在するユーザーID |

### リクエスト例

```json
{
  "title": "レビュー対応",
  "description": "API設計書のレビューを行う",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-04-20",
  "assignedUserId": 2
}
```

### 正常レスポンス

- HTTPステータス: `201 Created`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | タスクID |
| title | string | タイトル |
| description | string | 説明 |
| status | string | ステータス |
| priority | string | 優先度 |
| dueDate | string | 期限日 |
| assignedUser.id | number | 担当者ID |
| assignedUser.name | string | 担当者名 |
| createdBy.id | number | 作成者ID |
| createdBy.name | string | 作成者名 |
| createdAt | string | 作成日時 |
| updatedAt | string | 更新日時 |
| version | number | 楽観ロック用バージョン |

### レスポンス例

```json
{
  "id": 10,
  "title": "レビュー対応",
  "description": "API設計書のレビューを行う",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-04-20",
  "assignedUser": {
    "id": 2,
    "name": "佐藤花子"
  },
  "createdBy": {
    "id": 1,
    "name": "山田太郎"
  },
  "createdAt": "2026-04-12T21:30:00",
  "updatedAt": "2026-04-12T21:30:00",
  "version": 0
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-TASK-001 | title不正 |
| 400 | ERR-TASK-002 | status不正 |
| 400 | ERR-TASK-003 | dueDate不正 |
| 400 | ERR-INPUT-001 | priority等その他入力不正 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 404 | ERR-USR-002 | assignedUserIdのユーザーが存在しない |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.2.2 タスク一覧取得</summary>

## 6.2.2 タスク一覧取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | タスク一覧取得 |
| メソッド | GET |
| パス | `/api/tasks` |
| 認証 | 必要 |
| リクエスト形式 | Query Parameter |
| レスポンス形式 | JSON配列 |

### クエリパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| status | string |  | ステータス絞り込み |
| priority | string |  | 優先度絞り込み |
| assignedUserId | number |  | 担当者絞り込み |
| keyword | string |  | タイトルの部分一致検索 |

### 補足

- 取得対象はログインユーザーが参照可能なタスクのみ
- 削除済みタスクは返却しない
- `keyword` はタイトルに対する小文字化した部分一致検索
- ソート順は `createdAt DESC, id DESC`

### リクエスト例

```http
GET /api/tasks?status=TODO&priority=HIGH&assignedUserId=2&keyword=レビュー
Authorization: Bearer <JWT>
```

### 正常レスポンス

- HTTPステータス: `200 OK`

### レスポンス項目

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | タスクID |
| title | string | タイトル |
| status | string | ステータス |
| priority | string | 優先度 |
| dueDate | string | 期限日 |
| assignedUser.id | number | 担当者ID |
| assignedUser.name | string | 担当者名 |
| updatedAt | string | 更新日時 |
| version | number | 楽観ロック用バージョン |

### レスポンス例

```json
[
  {
    "id": 10,
    "title": "レビュー対応",
    "status": "TODO",
    "priority": "HIGH",
    "dueDate": "2026-04-20",
    "assignedUser": {
      "id": 2,
      "name": "佐藤花子"
    },
    "updatedAt": "2026-04-12T21:30:00",
    "version": 3
  }
]
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.2.3 タスク詳細取得</summary>

## 6.2.3 タスク詳細取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | タスク詳細取得 |
| メソッド | GET |
| パス | `/api/tasks/{taskId}` |
| 認証 | 必要 |
| レスポンス形式 | JSON |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| taskId | number | ○ | タスクID |

### 正常レスポンス

- HTTPステータス: `200 OK`
- レスポンス項目は「タスク作成」と同一

### 補足

- 削除済みタスクは未存在として扱う。

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-AUTH-005 | 参照権限なし |
| 404 | ERR-TASK-004 | タスク未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.2.4 タスク更新</summary>

## 6.2.4 タスク更新

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | タスク更新 |
| メソッド | PUT |
| パス | `/api/tasks/{taskId}` |
| 認証 | 必要 |
| リクエスト形式 | JSON |
| レスポンス形式 | JSON |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| taskId | number | ○ | タスクID |

### リクエストボディ

| 項目名 | 型 | 必須 | 説明 | バリデーション |
|---|---|---|---|---|
| title | string | ○ | タイトル | 未入力不可、100文字以内 |
| description | string |  | 説明 | 5000文字以内 |
| status | string | ○ | ステータス | `TODO` / `DOING` / `DONE` |
| priority | string | ○ | 優先度 | `LOW` / `MEDIUM` / `HIGH` |
| dueDate | string |  | 期限日 | `yyyy-MM-dd` |
| assignedUserId | number |  | 担当ユーザーID | 存在するユーザーID |
| version | number | ○ | 取得時点のタスクバージョン | 0以上の整数 |

### 正常レスポンス

- HTTPステータス: `200 OK`
- レスポンス項目は「タスク作成」と同一

### 補足

- 本APIはタスク詳細画面の編集モードで保存時に実行する。
- 専用のタスク編集画面は設けない。
- 更新成功後、画面は表示モードへ戻る。
- 画面側はレスポンス内容または再取得したタスク詳細で表示内容を更新する。
- リクエストボディの `version` には、編集開始時または直前再取得時に取得したタスク最新値を指定する。
- サーバー側の `version` と不一致の場合は `409 ERR-TASK-007` を返却し、タスク更新・履歴記録・通知生成は行わない。
- 削除済みタスクは未存在として扱う。

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-TASK-001 | title不正 |
| 400 | ERR-TASK-002 | status不正 |
| 400 | ERR-TASK-003 | dueDate不正 |
| 400 | ERR-INPUT-001 | priority等その他入力不正 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 409 | ERR-TASK-007 | 更新競合 |
| 403 | ERR-TASK-005 | 更新権限なし |
| 404 | ERR-TASK-004 | タスク未存在 |
| 404 | ERR-USR-002 | assignedUserIdのユーザーが存在しない |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.2.5 タスク削除</summary>

## 6.2.5 タスク削除

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | タスク削除 |
| メソッド | DELETE |
| パス | `/api/tasks/{taskId}` |
| 認証 | 必要 |
| レスポンス形式 | ボディなし |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| taskId | number | ○ | タスクID |

### 正常レスポンス

- HTTPステータス: `204 No Content`

### 補足

- 本APIは物理削除ではなく論理削除を行う。
- 削除時は `tasks.deleted_at`、`tasks.deleted_by` を設定する。
- 削除成功時に `TASK_DELETED` のアクティビティログを記録する。
- 削除済みタスクは未存在として扱う。
- タスク削除通知は本フェーズでは作成しない。

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-TASK-006 | 削除権限なし |
| 404 | ERR-TASK-004 | タスク未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

## 6.3 ユーザーAPI

<details>
<summary>6.3.1 ユーザー一覧取得</summary>

## 6.3.1 ユーザー一覧取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | ユーザー一覧取得 |
| メソッド | GET |
| パス | `/api/users` |
| 認証 | 必要 |
| レスポンス形式 | JSON配列 |

### 補足

- タスク担当者選択用のユーザー一覧を返す
- 並び順は `name ASC, id ASC`
- 現行実装の権限制御では Spring Security 上の権限は `ROLE_USER` のみを利用する
- `ADMIN` は将来拡張時のロール値として予約している

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | ユーザーID |
| name | string | ユーザー名 |
| email | string | メールアドレス |
| role | string | ロール |

### レスポンス例

```json
[
  {
    "id": 1,
    "name": "山田太郎",
    "email": "yamada@example.com",
    "role": "MEMBER"
  },
  {
    "id": 2,
    "name": "佐藤花子",
    "email": "sato@example.com",
    "role": "MEMBER"
  }
]
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

## 6.4 コメントAPI

<details>
<summary>6.4.1 コメント一覧取得</summary>

## 6.4.1 コメント一覧取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | コメント一覧取得 |
| メソッド | GET |
| パス | `/api/tasks/{taskId}/comments` |
| 認証 | 必要 |
| リクエスト形式 | Query Parameter |
| レスポンス形式 | ページングJSON |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| taskId | number | ○ | タスクID |

### クエリパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| page | number |  | ページ番号 |
| size | number |  | 1ページあたり件数 |

### 補足

- 対象タスクを参照できるユーザーのみ取得可能
- 削除済みコメントは返却しない
- 削除済みコメントを `削除済み` などのプレースホルダに置き換えて返却しない
- ソート順は `createdAt ASC, id ASC`

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | コメントID |
| taskId | number | タスクID |
| content | string | コメント本文 |
| createdBy.id | number | 投稿者ID |
| createdBy.name | string | 投稿者名 |
| createdAt | string | 投稿日時 |
| updatedAt | string | 更新日時 |
| version | number | 楽観ロック用バージョン |

### レスポンス例

```json
{
  "content": [
    {
      "id": 1,
      "taskId": 10,
      "content": "レビューお願いします。",
      "createdBy": {
        "id": 1,
        "name": "山田太郎"
      },
      "createdAt": "2026-04-14T10:00:00",
      "updatedAt": "2026-04-14T10:00:00",
      "version": 0
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-AUTH-005 | タスク参照権限なし |
| 404 | ERR-TASK-004 | タスク未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.4.2 コメント投稿</summary>

## 6.4.2 コメント投稿

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | コメント投稿 |
| メソッド | POST |
| パス | `/api/tasks/{taskId}/comments` |
| 認証 | 必要 |
| リクエスト形式 | JSON |
| レスポンス形式 | JSON |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| taskId | number | ○ | タスクID |

### リクエストボディ

| 項目名 | 型 | 必須 | 説明 | バリデーション |
|---|---|---|---|---|
| content | string | ○ | コメント本文 | 未入力不可、1000文字以内 |

### リクエスト例

```json
{
  "content": "レビューお願いします。"
}
```

### 正常レスポンス

- HTTPステータス: `201 Created`
- レスポンス項目は「コメント一覧取得」のコメント項目と同一

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-COMMENT-001 / ERR-COMMENT-003 | 未入力 / 文字数超過 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-AUTH-005 | タスク参照権限なし |
| 404 | ERR-TASK-004 | タスク未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.4.3 コメント更新</summary>

## 6.4.3 コメント更新

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | コメント更新 |
| メソッド | PUT |
| パス | `/api/comments/{commentId}` |
| 認証 | 必要 |
| リクエスト形式 | JSON |
| レスポンス形式 | JSON |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| commentId | number | ○ | コメントID |

### リクエストボディ

| 項目名 | 型 | 必須 | 説明 | バリデーション |
|---|---|---|---|---|
| content | string | ○ | コメント本文 | 未入力不可、1000文字以内 |
| version | number | ○ | 取得時点のコメントバージョン | 0以上の整数 |

### 正常レスポンス

- HTTPステータス: `200 OK`
- レスポンス項目は「コメント一覧取得」のコメント項目と同一

### 補足

- リクエストボディの `version` には、コメント一覧取得APIなどで取得した対象コメントの最新値を指定する。
- サーバー側の `version` と不一致の場合は `409 ERR-COMMENT-006` を返却し、コメント更新・履歴記録・通知生成は行わない。

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-COMMENT-001 / ERR-COMMENT-003 | 未入力 / 文字数超過 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 409 | ERR-COMMENT-006 | 更新競合 |
| 403 | ERR-COMMENT-004 | コメント更新権限なし |
| 404 | ERR-COMMENT-002 | コメント未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.4.4 コメント削除</summary>

## 6.4.4 コメント削除

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | コメント削除 |
| メソッド | DELETE |
| パス | `/api/comments/{commentId}` |
| 認証 | 必要 |
| レスポンス形式 | ボディなし |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| commentId | number | ○ | コメントID |

### 正常レスポンス

- HTTPステータス: `204 No Content`

### 補足

- 削除成功後、対象コメントはコメント一覧取得APIの返却対象から除外する。
- コメント削除の事実は、履歴一覧取得APIで `COMMENT_DELETED` イベントとして確認する。
- 削除済みコメント本文は API レスポンスで再表示しない。

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-COMMENT-005 | コメント削除権限なし |
| 404 | ERR-COMMENT-002 | コメント未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

## 6.5 添付ファイルAPI

<details>
<summary>6.5.1 添付ファイル一覧取得</summary>

## 6.5.1 添付ファイル一覧取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | 添付ファイル一覧取得 |
| メソッド | GET |
| パス | `/api/tasks/{taskId}/attachments` |
| 認証 | 必要 |
| レスポンス形式 | JSON配列 |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| taskId | number | ○ | タスクID |

### 補足

- 対象タスクを参照できるユーザーのみ取得可能
- 削除済み添付ファイルは返却しない
- ソート順は `createdAt DESC, id DESC`

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | 添付ID |
| taskId | number | タスクID |
| originalFileName | string | 元ファイル名 |
| contentType | string | MIMEタイプ |
| fileSize | number | ファイルサイズ |
| storageType | string | 保存種別 |
| uploadedBy.id | number | アップロード者ID |
| uploadedBy.name | string | アップロード者名 |
| createdAt | string | アップロード日時 |

### レスポンス例

```json
[
  {
    "id": 1,
    "taskId": 10,
    "originalFileName": "design.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "storageType": "S3",
    "uploadedBy": {
      "id": 1,
      "name": "山田太郎"
    },
    "createdAt": "2026-04-14T10:30:00"
  }
]
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-AUTH-005 | タスク参照権限なし |
| 404 | ERR-TASK-004 | タスク未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.5.2 添付ファイルアップロード</summary>

## 6.5.2 添付ファイルアップロード

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | 添付ファイルアップロード |
| メソッド | POST |
| パス | `/api/tasks/{taskId}/attachments` |
| 認証 | 必要 |
| リクエスト形式 | multipart/form-data |
| レスポンス形式 | JSON |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| taskId | number | ○ | タスクID |

### フォームデータ

| 項目名 | 型 | 必須 | 説明 | バリデーション |
|---|---|---|---|---|
| file | file | ○ | 添付ファイル | 未選択不可、1ファイル10MB以内、1タスク20件以内、1タスク合計100MB以内、許可形式のみ |

### 補足

- 拡張子と MIME タイプの両方を検証する。
- 件数上限と合計サイズ上限は、既存の未削除添付を含めて判定する。
- 本フェーズの制約値は 1ファイル10MB、1タスク20件、1タスク合計100MB とする。
- 1ファイル10MBは業務バリデーション上限であり、HTTP multipart の受信上限は `max-file-size=12MB` / `max-request-size=12MB` とする。
- 10MB超過、または12MBのmultipart受信上限超過はいずれも `ERR-FILE-005` として扱う。

### 正常レスポンス

- HTTPステータス: `201 Created`
- レスポンス項目は「添付ファイル一覧取得」の添付項目と同一

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-FILE-001 / ERR-FILE-005 / ERR-FILE-006 / ERR-FILE-007 / ERR-FILE-008 | 未選択 / 1ファイル10MB業務上限超過または12MB multipart受信上限超過 / 形式不正 / 件数上限超過 / 合計サイズ上限超過 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-FILE-003 | 添付アップロード権限なし |
| 404 | ERR-TASK-004 | タスク未存在 |
| 500 | ERR-FILE-009 / ERR-SYS-999 / ERR-SYS-001 | S3保存失敗 / 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.5.3 添付ファイルダウンロード</summary>

## 6.5.3 添付ファイルダウンロード

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | 添付ファイルダウンロード |
| メソッド | GET |
| パス | `/api/attachments/{attachmentId}/download` |
| 認証 | 必要 |
| レスポンス形式 | ファイルバイナリ |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| attachmentId | number | ○ | 添付ID |

### 正常レスポンス

- HTTPステータス: `200 OK`
- `Content-Type`: 添付ファイルのMIMEタイプ
- `Content-Disposition`: `attachment; filename="<originalFileName>"`

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-AUTH-005 | ダウンロード権限なし |
| 404 | ERR-FILE-002 | 添付ファイル未存在 |
| 500 | ERR-FILE-010 / ERR-SYS-999 / ERR-SYS-001 | S3取得失敗 / 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.5.4 添付ファイル削除</summary>

## 6.5.4 添付ファイル削除

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | 添付ファイル削除 |
| メソッド | DELETE |
| パス | `/api/attachments/{attachmentId}` |
| 認証 | 必要 |
| レスポンス形式 | ボディなし |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| attachmentId | number | ○ | 添付ID |

### 正常レスポンス

- HTTPステータス: `204 No Content`

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-FILE-004 | 添付削除権限なし |
| 404 | ERR-FILE-002 | 添付ファイル未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

## 6.6 通知API

<details>
<summary>6.6.1 通知一覧取得</summary>

## 6.6.1 通知一覧取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | 通知一覧取得 |
| メソッド | GET |
| パス | `/api/notifications` |
| 認証 | 必要 |
| リクエスト形式 | Query Parameter |
| レスポンス形式 | ページングJSON |

### クエリパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| page | number |  | ページ番号 |
| size | number |  | 1ページあたり件数 |
| unreadOnly | boolean |  | 未読のみ取得する場合は `true` |

### 補足

- 自分宛の通知のみ取得する。
- 通知は `notifications` と `activity_logs` をもとに返却する。
- `notifications` は既読状態を管理し、通知本文や関連タスクは `activity_logs` から組み立てる。
- ソート順は `notifications.createdAt DESC, notifications.id DESC`。
- 通知レコードクリック時の遷移先として `relatedTaskId` を返却する。
- 画面側は通知レコードクリック時、未読なら `PATCH /api/notifications/{id}/read` を先に実行し、その後 `GET /api/tasks/{relatedTaskId}` で参照可否を確認する。
- `GET /api/tasks/{relatedTaskId}` が `200 OK` の場合のみ画面遷移する。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | 通知ID |
| activityLogId | number | 紐づく履歴ID |
| eventType | string | イベント種別 |
| message | string | 表示メッセージ。`activity_logs.summary` 由来 |
| relatedTaskId | number | 関連タスクID。`activity_logs.task_id` 由来 |
| targetType | string | 対象種別 |
| targetId | number | 対象ID |
| isRead | boolean | 既読フラグ |
| readAt | string | 既読日時 |
| createdAt | string | 通知作成日時 |

### レスポンス例

```json
{
  "content": [
    {
      "id": 1,
      "activityLogId": 10,
      "eventType": "COMMENT_CREATED",
      "message": "田中 花子さんがコメントを投稿しました。",
      "relatedTaskId": 18,
      "targetType": "COMMENT",
      "targetId": 5,
      "isRead": false,
      "readAt": null,
      "createdAt": "2026-04-14T11:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.6.2 未読通知件数取得</summary>

## 6.6.2 未読通知件数取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | 未読通知件数取得 |
| メソッド | GET |
| パス | `/api/notifications/unread-count` |
| 認証 | 必要 |
| レスポンス形式 | JSON |

### 補足

- サイドバーの通知一覧バッジ表示に利用する。
- 自分宛の未読通知のみを集計する。
- ログイン後、画面遷移時、一定間隔ポーリング時、既読化後に利用する。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| unreadCount | number | 未読通知件数 |

### レスポンス例

```json
{
  "unreadCount": 2
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.6.3 通知既読化</summary>

## 6.6.3 通知既読化

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | 通知既読化 |
| メソッド | PATCH |
| パス | `/api/notifications/{notificationId}/read` |
| 認証 | 必要 |
| レスポンス形式 | JSON |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| notificationId | number | ○ | 通知ID |

### 補足

- 自分宛通知のみ既読化できる。
- 既読化済み通知に対して再実行された場合も正常終了とする。
- 通知レコードクリック時、未読通知であれば本APIを先に実行してから関連タスクの参照可否確認へ進む。
- 既読化成功後に関連タスク参照確認が `404` または `403` となった場合でも、通知の既読状態は維持する。

### 正常レスポンス

- HTTPステータス: `200 OK`
- レスポンス項目は「通知一覧取得」の通知項目と同一

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-NOTIFY-002 | 通知参照権限なし |
| 404 | ERR-NOTIFY-001 | 通知未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.6.4 通知一括既読化</summary>

## 6.6.4 通知一括既読化

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | 通知一括既読化 |
| メソッド | PATCH |
| パス | `/api/notifications/read-all` |
| 認証 | 必要 |
| レスポンス形式 | ボディなし |

### 補足

- 対象は自分宛の未読通知のみ。
- 未読通知が0件の状態で再実行された場合も `204 No Content` で正常終了とする。
- 正常終了後、画面側は通知一覧および未読通知件数を再取得する。

### 正常レスポンス

- HTTPステータス: `204 No Content`

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

## 6.7 履歴API

<details>
<summary>6.7.1 タスク別履歴一覧取得</summary>

## 6.7.1 タスク別履歴一覧取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | タスク別履歴一覧取得 |
| メソッド | GET |
| パス | `/api/tasks/{taskId}/activities` |
| 認証 | 必要 |
| リクエスト形式 | Query Parameter |
| レスポンス形式 | ページングJSON |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| taskId | number | ○ | タスクID |

### クエリパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| page | number |  | ページ番号 |
| size | number |  | 1ページあたり件数 |
| eventType | string |  | イベント種別 |

### 補足

- 対象タスクを参照できるユーザーのみ取得可能。
- タスク詳細画面のアクティビティ領域にある履歴タブで利用する。
- 独立したアクティビティログ画面は設けない。
- ソート順は `createdAt DESC, id DESC`。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | 履歴ID |
| eventType | string | イベント種別 |
| actor.id | number | 操作ユーザーID |
| actor.name | string | 操作ユーザー名 |
| targetType | string | 対象種別 |
| targetId | number | 対象ID |
| taskId | number | 関連タスクID |
| summary | string | 表示用概要 |
| detailJson | object | 詳細情報 |
| createdAt | string | 作成日時 |

### レスポンス例

```json
{
  "content": [
    {
      "id": 1,
      "eventType": "COMMENT_CREATED",
      "actor": {
        "id": 1,
        "name": "山田太郎"
      },
      "targetType": "COMMENT",
      "targetId": 5,
      "taskId": 10,
      "summary": "山田太郎さんがコメントを投稿しました。",
      "detailJson": null,
      "createdAt": "2026-04-14T11:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-ACTIVITY-002 | 履歴参照権限なし |
| 404 | ERR-TASK-004 | タスク未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

</details>

<details>
<summary>7. 補足事項</summary>

## 7. 補足事項

## 7.1 ステータス値

| 値 | 意味 |
|---|---|
| TODO | 未着手 |
| DOING | 対応中 |
| DONE | 完了 |

## 7.2 優先度値

| 値 | 意味 |
|---|---|
| LOW | 低 |
| MEDIUM | 中 |
| HIGH | 高 |

## 7.3 日付形式

| 項目 | 形式 |
|---|---|
| dueDate | `yyyy-MM-dd` |
| createdAt / updatedAt / readAt | ISO-8601形式の日時文字列 |

## 7.4 ロール値

| 値 | 意味 | 適用 |
|---|---|---|
| MEMBER | 一般ユーザー | 現行 / 将来 |
| ADMIN | 管理者ユーザー | 将来拡張 |

## 7.5 アクティビティイベント種別

| 値 | 意味 |
|---|---|
| TASK_CREATED | タスク作成 |
| TASK_UPDATED | タスク更新 |
| TASK_DELETED | タスク削除 |
| COMMENT_CREATED | コメント投稿 |
| COMMENT_UPDATED | コメント更新 |
| COMMENT_DELETED | コメント削除 |
| ATTACHMENT_UPLOADED | 添付追加 |
| ATTACHMENT_DELETED | 添付削除 |

## 7.6 アクティビティ対象種別

| 値 | 意味 |
|---|---|
| TASK | タスク |
| COMMENT | コメント |
| ATTACHMENT | 添付ファイル |

## 7.7 添付保存種別

| 値 | 意味 |
|---|---|
| LOCAL | ローカル保存 |
| S3 | S3保存 |

---


</details>

<details>
<summary>8. 備考</summary>

## 8. 備考

- 本設計書は `develop` ブランチのAPI仕様を定義する
- 認証は JWT Bearer 認証を使用する
- 成功レスポンスは共通 envelope を使用せず、raw DTO / raw JSON 配列 / ページングレスポンス / ボディなしを返す
- 失敗時のみ `ErrorResponse` を共通形式として扱う
- 一覧APIは対象データの特性に応じてページングを行う
- タスク更新はタスク詳細画面の編集モードから既存の `PUT /api/tasks/{taskId}` を利用する
- タスク削除は物理削除ではなく、`deleted_at`、`deleted_by` を設定する論理削除とする
- 通知一覧は `notifications` と `activity_logs` をもとにレスポンスを組み立てる
- 履歴は独立画面ではなく、タスク詳細画面内の履歴タブで表示する

</details>
