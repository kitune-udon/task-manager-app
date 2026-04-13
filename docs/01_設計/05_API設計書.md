# API設計書

## 改訂履歴

| 版数 | 改訂日 | 改訂内容 | 作成者 |
|---|---|---|---|
| 1.0 | 2026-04-13 | 初版作成 | 佐伯 |

## 目次

- 1 [文書概要](#1-文書概要)
- 2 [API概要](#2-api概要)
- 3 [認証・認可方針](#3-認証認可方針)
- 4 [共通仕様](#4-共通仕様)
- 5 [エラーコード一覧](#5-エラーコード一覧)
- 7 [補足事項](#7-補足事項)
- 8 [備考](#8-備考)

## 1. 文書概要

- システム名: task-manager-app
- 対象ブランチ: `develop`
- 対象ディレクトリ: `backend`
- API方式: REST API
- データ形式: JSON
- 認証方式: JWT Bearer認証
- 作成方針: 実コードベースで現行仕様を整理

---

## 2. API概要

本システムの現行APIは以下で構成される。

- 認証API
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- タスクAPI
  - `POST /api/tasks`
  - `GET /api/tasks`
  - `GET /api/tasks/{taskId}`
  - `PUT /api/tasks/{taskId}`
  - `DELETE /api/tasks/{taskId}`
- ユーザーAPI
  - `GET /api/users`

---

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

### 3.3 認証ヘッダー

```http
Authorization: Bearer <JWT>
```

### 3.4 権限制御

#### タスク参照
以下のいずれかを満たす場合に参照可能。

- タスク作成者
- タスク担当者

#### タスク更新
以下のいずれかを満たす場合に更新可能。

- タスク作成者
- タスク担当者

#### タスク削除
以下を満たす場合のみ削除可能。

- タスク作成者

---

## 4. 共通仕様

## 4.1 リクエストヘッダー

| ヘッダー名 | 必須 | 内容 |
|---|---|---|
| Content-Type | POST/PUT時は必須 | `application/json` |
| Authorization | 認証必須APIで必須 | `Bearer <JWT>` |

## 4.2 正常レスポンス

- APIごとに個別DTOを返却する
- 共通ラッパーは使用していない
- `DELETE` はレスポンスボディなし

## 4.3 エラーレスポンス形式

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

---

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
| ユーザー | ERR-USR-002 | 404 | ユーザーが存在しません |
| タスク | ERR-TASK-004 | 404 | タスクが存在しません |
| タスク | ERR-TASK-005 | 403 | タスク更新権限がありません |
| タスク | ERR-TASK-006 | 403 | タスク削除権限がありません |
| システム | ERR-SYS-001 | 500 | データの取得または更新に失敗しました。しばらくしてから再度お試しください。 |
| システム | ERR-SYS-999 | 500 | システムエラーが発生しました。しばらくしてから再度お試しください。 |

---

# 6. API詳細

## 6.1 認証API

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
| createdAt | string | 作成日時 |

### レスポンス例

```json
{
  "id": 1,
  "name": "山田太郎",
  "email": "yamada@example.com",
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

### レスポンス例

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "name": "山田太郎",
    "email": "yamada@example.com"
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

## 6.2 タスクAPI

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
  "updatedAt": "2026-04-12T21:30:00"
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

- 取得対象は「ログインユーザーが作成者」または「ログインユーザーが担当者」のタスクのみ
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
    "updatedAt": "2026-04-12T21:30:00"
  }
]
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---

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

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-AUTH-005 | 参照権限なし |
| 404 | ERR-TASK-004 | タスク未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---

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

### 正常レスポンス

- HTTPステータス: `200 OK`
- レスポンス項目は「タスク作成」と同一

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-TASK-001 | title不正 |
| 400 | ERR-TASK-002 | status不正 |
| 400 | ERR-TASK-003 | dueDate不正 |
| 400 | ERR-INPUT-001 | priority等その他入力不正 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-TASK-005 | 更新権限なし |
| 404 | ERR-TASK-004 | タスク未存在 |
| 404 | ERR-USR-002 | assignedUserIdのユーザーが存在しない |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---

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

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-TASK-006 | 削除権限なし |
| 404 | ERR-TASK-004 | タスク未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---

## 6.3 ユーザーAPI

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

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | ユーザーID |
| name | string | ユーザー名 |
| email | string | メールアドレス |

### レスポンス例

```json
[
  {
    "id": 1,
    "name": "山田太郎",
    "email": "yamada@example.com"
  },
  {
    "id": 2,
    "name": "佐藤花子",
    "email": "sato@example.com"
  }
]
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---

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
| createdAt / updatedAt | ISO-8601形式の日時文字列 |

---

## 8. 備考

- 本設計書は `develop` ブランチの実装を基準とした現行仕様である
- タスクコメント、添付ファイル、チーム管理に関するAPIは現時点では未実装
- 一覧APIのキーワード検索対象は `title` のみ
- タスク更新時の権限は「作成者または担当者」、削除時の権限は「作成者のみ」である