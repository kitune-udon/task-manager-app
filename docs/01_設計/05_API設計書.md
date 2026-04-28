# API設計書

## 改訂履歴

| 版数 | 改訂日 | 改訂内容 | 作成者 |
|---|---|---|---|
| 1.0 | 2026-04-13 | 初版作成 | 佐伯 |
| 1.1 | 2026-04-14 | 成功レスポンス方針を明記 | 佐伯 |
| 1.2 | 2026-04-14 | コメント、添付ファイル、通知、履歴APIを追加 | 佐伯 |
| 1.3 | 2026-04-28 | チーム管理API、チームメンバーAPI、タスクAPIのチーム対応、関連する認可・エラーコードを追加 | 佐伯 |

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

### 認証テストAPI

| メソッド | パス | 用途 |
|---|---|---|
| GET | `/api/auth-test/me` | 認証コンテキスト確認 |

### タスクAPI

| メソッド | パス | 用途 |
|---|---|---|
| POST | `/api/tasks` | タスク作成 |
| GET | `/api/tasks` | タスク一覧取得 |
| GET | `/api/tasks/{taskId}` | タスク詳細取得 |
| PUT | `/api/tasks/{taskId}` | タスク更新 |
| DELETE | `/api/tasks/{taskId}` | タスク削除 |

### チームAPI

| メソッド | パス | 用途 |
|---|---|---|
| POST | `/api/teams` | チーム作成 |
| GET | `/api/teams` | 所属チーム一覧取得 |
| GET | `/api/teams/{teamId}` | チーム詳細取得 |
| GET | `/api/teams/{teamId}/members` | チーム所属メンバー一覧取得 |
| GET | `/api/teams/{teamId}/available-users` | チーム追加候補ユーザー一覧取得 |
| POST | `/api/teams/{teamId}/members` | チームメンバー追加 |
| PATCH | `/api/teams/{teamId}/members/{memberId}` | チーム内ロール変更 |
| DELETE | `/api/teams/{teamId}/members/{memberId}` | チームメンバー削除 |

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
- `/api/teams/**`
- `/api/auth-test/**`
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

- 対象タスクの所属チームにログインユーザーが所属していること
- 作成者であっても、現在対象タスクの所属チームに所属していない場合は参照不可

#### タスク更新

以下の条件で更新可能。

- 対象タスクの所属チームにログインユーザーが所属していること
- 作成者、担当者、チーム管理者（`OWNER` / `ADMIN`）のいずれかであること
- タスク所属チームの変更は不可

#### タスク削除

以下の条件で削除可能。

- 対象タスクの所属チームにログインユーザーが所属していること
- 作成者またはチーム管理者（`OWNER` / `ADMIN`）であること

#### チーム管理

| API | OWNER | ADMIN | MEMBER | 非所属 |
|---|---|---|---|---|
| チーム一覧取得 | ○ | ○ | ○ | - |
| チーム詳細取得 | ○ | ○ | ○ | × |
| チームメンバー一覧取得 | ○ | ○ | ○ | × |
| チーム追加候補ユーザー一覧取得 | ○ | ○ | × | × |
| チーム作成 | ○ | ○ | ○ | - |
| チームメンバー追加 | ○ | ○ | × | × |
| チームメンバー削除 | ○ | ○ | × | × |
| チーム内ロール変更 | ○ | × | × | × |

チーム一覧取得はログインユーザーが所属するチームのみ返却する。`GET /api/teams/{teamId}/available-users` は `OWNER` または `ADMIN` のみ参照可能とする。`OWNER` は自分自身の削除およびロール変更を不可とする。`ADMIN` の自己削除は可とする。

#### コメント参照 / 投稿

以下を満たす場合に利用可能。

- 親タスクの所属チームにログインユーザーが所属していること
- 親タスクが削除済みではないこと

#### コメント更新 / 削除

以下の条件で利用可能。

- コメント投稿者本人またはチーム管理者（`OWNER` / `ADMIN`）であること
- 親タスクの所属チームにログインユーザーが所属していること

#### 添付ファイル一覧 / ダウンロード

以下を満たす場合に利用可能。

- 親タスクの所属チームにログインユーザーが所属していること
- 親タスクが削除済みではないこと

#### 添付ファイルアップロード

以下を満たす場合に利用可能。

- 対象タスクを更新できること

#### 添付ファイル削除

以下の条件で利用可能。

- 添付登録者本人またはチーム管理者（`OWNER` / `ADMIN`）であること
- 親タスクの所属チームにログインユーザーが所属していること

#### 通知参照 / 既読化

以下を満たす場合に利用可能。

- 自分宛の通知であること

通知自体は削除済みタスクに紐づいていても保持する。通知からタスクへ遷移する場合の参照可否は、タスク詳細取得APIの判定に従う。

#### タスク別履歴参照

以下を満たす場合に利用可能。

- 親タスクの所属チームにログインユーザーが所属していること
- 親タスクが削除済みではないこと

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

以下のAPIは、小規模利用を前提としてページングを導入しない。

- `GET /api/teams`
- `GET /api/teams/{teamId}/members`
- `GET /api/teams/{teamId}/available-users`

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
- `PATCH /api/teams/{teamId}/members/{memberId}` は冪等とし、現在ロールと同じ値を指定した場合でも `200 OK` を返す。
- `POST /api/tasks`、`POST /api/tasks/{taskId}/comments`、`POST /api/tasks/{taskId}/attachments`、`POST /api/teams`、`POST /api/teams/{teamId}/members` は本フェーズでは冪等化しない。
- 作成系APIは `Idempotency-Key` を受け付けず、画面側の二重送信防止で重複作成を抑止する。

## 4.7 403 / 404 の返し分け

- リソースが存在しない場合は `404 Not Found` を返却する。
- リソースは存在するが権限不足の場合は `403 Forbidden` を返却する。
- 本システムは認証必須の内部向けアプリケーションを前提とするため、存在有無の秘匿よりも、運用性・デバッグ容易性・テスト観点の明確性を優先する。
- 画面表示メッセージは UX を優先し、必ずしも API と同じ粒度で出し分けない。

## 4.8 削除済みデータの扱い

- `GET /api/tasks` では論理削除済みタスクを返却しない。
- `GET /api/tasks/{taskId}`、`PUT /api/tasks/{taskId}`、`DELETE /api/tasks/{taskId}` で対象タスクが論理削除済みの場合は `404 ERR-TASK-004` を返却する。
- `GET /api/tasks/{taskId}/comments` と `POST /api/tasks/{taskId}/comments` は、親タスクが論理削除済みの場合 `404 ERR-TASK-004` を返却する。
- `PUT /api/comments/{commentId}` と `DELETE /api/comments/{commentId}` は、対象コメントが論理削除済みの場合 `404 ERR-COMMENT-002` を返却する。
- `GET /api/tasks/{taskId}/attachments` と `POST /api/tasks/{taskId}/attachments` は、親タスクが論理削除済みの場合 `404 ERR-TASK-004` を返却する。
- `GET /api/attachments/{attachmentId}/download` と `DELETE /api/attachments/{attachmentId}` は、対象添付が論理削除済みの場合 `404 ERR-FILE-002` を返却する。
- `GET /api/tasks/{taskId}/activities` は、親タスクが論理削除済みの場合 `404 ERR-TASK-004` を返却する。
- `GET /api/notifications` は削除済みタスクに紐づく通知も返却対象とするが、遷移先タスク取得時は通常のタスク参照API結果に従う。

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
| タスク入力 | ERR-TASK-008 | 400 | タスクを作成するチームを指定してください |
| タスク | ERR-TASK-009 | 403 | 指定されたチームの操作権限がありません |
| タスク入力 | ERR-TASK-010 | 400 | 担当者はチームメンバーから選択してください |
| ユーザー | ERR-USR-001 | 409 | メールアドレスは既に登録されています |
| タスク | ERR-TASK-007 | 409 | 他のユーザーによりタスクが更新されました。最新状態を再読み込みしてください。 |
| コメント | ERR-COMMENT-006 | 409 | 他のユーザーによりコメントが更新されました。最新状態を再読み込みしてください。 |
| ユーザー | ERR-USR-002 | 404 | ユーザーが存在しません |
| タスク | ERR-TASK-004 | 404 | タスクが存在しません |
| タスク | ERR-TASK-005 | 403 | タスク更新権限がありません |
| タスク | ERR-TASK-006 | 403 | タスク削除権限がありません |
| チーム入力 | ERR-TEAM-001 | 400 | チーム入力内容に誤りがあります |
| チーム | ERR-TEAM-002 | 409 | 同じ名前のチームが既に存在します |
| チーム | ERR-TEAM-003 | 403 | このチームにアクセスする権限がありません |
| チーム | ERR-TEAM-004 | 404 | 対象のチームが存在しません |
| チームメンバー入力 | ERR-TEAM-MEMBER-001 | 400 | メンバー入力内容に誤りがあります |
| チームメンバー | ERR-TEAM-MEMBER-002 | 403 | メンバー追加権限がありません |
| チームメンバー | ERR-TEAM-MEMBER-003 | 409 | 対象ユーザーは既にチームに所属しています |
| チームメンバー入力 | ERR-TEAM-MEMBER-004 | 400 | 指定されたロールが不正です |
| チームメンバー | ERR-TEAM-MEMBER-005 | 403 | ロール変更権限がありません |
| チームメンバー | ERR-TEAM-MEMBER-006 | 403 | OWNERのロールは変更できません |
| チームメンバー | ERR-TEAM-MEMBER-007 | 404 | 対象のメンバーが存在しません |
| チームメンバー | ERR-TEAM-MEMBER-008 | 403 | メンバー削除権限がありません |
| チームメンバー | ERR-TEAM-MEMBER-009 | 403 | OWNER は削除できません |
| チームメンバー | ERR-TEAM-MEMBER-010 | 409 | 担当中のタスクがあるため、このメンバーを削除できません |
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
| 添付ファイル | ERR-FILE-011 | 500 | ファイル後処理に失敗しました。詳細はログを確認してください。 |
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
| teamId | number | ○ | 所属チームID | ログインユーザーが所属するチーム |
| title | string | ○ | タイトル | 未入力不可、100文字以内 |
| description | string |  | 説明 | 5000文字以内 |
| status | string | ○ | ステータス | `TODO` / `DOING` / `DONE` |
| priority | string | ○ | 優先度 | `LOW` / `MEDIUM` / `HIGH` |
| dueDate | string |  | 期限日 | `yyyy-MM-dd` |
| assignedUserId | number |  | 担当ユーザーID | 同一チーム所属ユーザーのみ |

### リクエスト例

```json
{
  "teamId": 1,
  "teamName": "開発チームA",
  "title": "レビュー対応",
  "description": "API設計書のレビューを行う",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2026-04-20",
  "assignedUserId": 2
}
```

### 補足

- 所属チームがないユーザーは作成不可とする。
- 指定 `teamId` に所属していない場合は作成不可とする。
- `assignedUserId` を設定する場合、同一チーム所属ユーザーのみ指定可能とする。
- タスク作成は `teamId` が確定したチーム文脈から行う。

### 正常レスポンス

- HTTPステータス: `201 Created`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | タスクID |
| teamId | number | 所属チームID |
| teamName | string | 所属チーム名 |
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
  "teamId": 1,
  "teamName": "開発チームA",
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
| 400 | ERR-TASK-008 | teamId未指定 |
| 400 | ERR-TASK-010 | 担当者がチームメンバーではない |
| 400 | ERR-INPUT-001 | priority等その他入力不正 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-TASK-009 | 指定されたチームの操作権限なし |
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
| teamId | number |  | 指定時は当該チームのタスクのみに絞り込む |
| status | string |  | ステータス絞り込み |
| priority | string |  | 優先度絞り込み |
| assignedUserId | number |  | 担当者絞り込み |
| keyword | string |  | タイトルの部分一致検索 |

### 補足

- 取得対象はログインユーザーが所属するチームのタスクのみ
- `teamId` 指定時は、そのチームに所属している場合のみ当該チームのタスクを返却する
- `teamId` 未指定時は、所属全チーム横断でタスクを返却する
- 削除済みタスクは返却しない
- `keyword` はタイトルに対する小文字化した部分一致検索
- ソート順は `createdAt DESC, id DESC`

### リクエスト例

```http
GET /api/tasks?teamId=1&status=TODO&priority=HIGH&assignedUserId=2&keyword=レビュー
Authorization: Bearer <JWT>
```

### 正常レスポンス

- HTTPステータス: `200 OK`

### レスポンス項目

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | タスクID |
| teamId | number | 所属チームID |
| teamName | string | 所属チーム名 |
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
    "teamId": 1,
    "teamName": "開発チームA",
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
| 403 | ERR-TASK-009 | 指定されたチームの操作権限なし |
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

### 補足

- 対象タスクの所属チームにログインユーザーが所属している場合のみ参照可能とする。
- 削除済みタスクは未存在として扱う。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | タスクID |
| teamId | number | 所属チームID |
| teamName | string | 所属チーム名 |
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
  "teamId": 1,
  "teamName": "開発チームA",
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
  "version": 3
}
```

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
| assignedUserId | number |  | 担当ユーザーID | 同一チーム所属ユーザーのみ |
| version | number | ○ | 取得時点のタスクバージョン | 0以上の整数 |

### リクエスト例

```json
{
  "title": "レビュー対応",
  "description": "API設計書のレビューを行う",
  "status": "DOING",
  "priority": "HIGH",
  "dueDate": "2026-04-20",
  "assignedUserId": 2,
  "version": 3
}
```

### 補足

- 対象タスクの所属チームにログインユーザーが所属していることを前提に認可判定する。
- 作成者、担当者、チーム管理者（`OWNER` / `ADMIN`）のいずれかのみ更新可能とする。
- `assignedUserId` を変更する場合、同一チーム所属ユーザーのみ指定可能とする。
- `teamId` は更新対象に含めない。
- タスク所属チーム変更は不可とする。
- サーバー側の `version` と不一致の場合は `409 ERR-TASK-007` を返却する。

### 正常レスポンス

- HTTPステータス: `200 OK`
- レスポンス項目は「タスク詳細取得」と同一

### レスポンス例

```json
{
  "id": 10,
  "teamId": 1,
  "teamName": "開発チームA",
  "title": "レビュー対応",
  "description": "API設計書のレビューを行う",
  "status": "DOING",
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
  "updatedAt": "2026-04-12T22:00:00",
  "version": 4
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-TASK-001 | title不正 |
| 400 | ERR-TASK-002 | status不正 |
| 400 | ERR-TASK-003 | dueDate不正 |
| 400 | ERR-TASK-010 | 担当者がチームメンバーではない |
| 400 | ERR-INPUT-001 | priority等その他入力不正 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-TASK-005 | タスク更新権限がありません |
| 404 | ERR-TASK-004 | タスク未存在 |
| 404 | ERR-USR-002 | assignedUserIdのユーザーが存在しない |
| 409 | ERR-TASK-007 | 更新競合 |
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

### 補足

- 対象タスクの所属チームにログインユーザーが所属していることを前提に認可判定する。
- 作成者またはチーム管理者（`OWNER` / `ADMIN`）のみ削除可能とする。
- タスク本体は物理削除ではなく論理削除とする。
- 親タスク削除時は、紐づくコメントと添付ファイルメタ情報を連鎖論理削除する。
- `activity_logs` と `notifications` は削除しない。
- 削除済みタスクは未存在として扱う。

### 正常レスポンス

- HTTPステータス: `204 No Content`

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-TASK-006 | タスク削除権限がありません |
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

- タスク担当者選択などで利用する。
- チーム文脈で担当者候補を取得する場合は、タスクの所属チームに応じて同一チーム所属ユーザーのみを選択可能とする。
- チームメンバー追加候補は `GET /api/teams/{teamId}/available-users` を利用する。

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

| 項目名 | 型 | 必須 | デフォルト | 説明 |
|---|---|---|---|---|
| page | number |  | 0 | ページ番号 |
| size | number |  | 20 | 1ページあたり件数 |

### 補足

- 親タスクの所属チームにログインユーザーが所属している場合のみ参照可能とする。
- 親タスクが削除済みの場合は `404 ERR-TASK-004` を返却する。
- 論理削除済みコメントは返却しない。
- ソート順は `createdAt ASC, id ASC` とする。

### 正常レスポンス

- HTTPステータス: `200 OK`

### レスポンス項目

| 項目名 | 型 | 説明 |
|---|---|---|
| content[].id | number | コメントID |
| content[].taskId | number | タスクID |
| content[].content | string | コメント本文 |
| content[].createdBy.id | number | 投稿者ID |
| content[].createdBy.name | string | 投稿者名 |
| content[].createdAt | string | 作成日時 |
| content[].updatedAt | string | 更新日時 |
| content[].version | number | 楽観ロック用バージョン |
| page | number | ページ番号 |
| size | number | 1ページあたり件数 |
| totalElements | number | 総件数 |
| totalPages | number | 総ページ数 |

### レスポンス例

```json
{
  "content": [
    {
      "id": 1,
      "taskId": 10,
      "content": "設計内容を確認しました。",
      "createdBy": {
        "id": 1,
        "name": "山田太郎"
      },
      "createdAt": "2026-04-12T21:40:00",
      "updatedAt": "2026-04-12T21:40:00",
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
| 403 | ERR-AUTH-005 | 参照権限なし |
| 404 | ERR-TASK-004 | 親タスク未存在 |
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
  "content": "設計内容を確認しました。"
}
```

### 補足

- 親タスクの所属チームにログインユーザーが所属している場合のみ投稿可能とする。
- 親タスクが削除済みの場合は `404 ERR-TASK-004` を返却する。

### 正常レスポンス

- HTTPステータス: `201 Created`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | コメントID |
| taskId | number | タスクID |
| content | string | コメント本文 |
| createdBy.id | number | 投稿者ID |
| createdBy.name | string | 投稿者名 |
| createdAt | string | 作成日時 |
| updatedAt | string | 更新日時 |
| version | number | 楽観ロック用バージョン |

### レスポンス例

```json
{
  "id": 1,
  "taskId": 10,
  "content": "設計内容を確認しました。",
  "createdBy": {
    "id": 1,
    "name": "山田太郎"
  },
  "createdAt": "2026-04-12T21:40:00",
  "updatedAt": "2026-04-12T21:40:00",
  "version": 0
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-COMMENT-001 / ERR-COMMENT-003 | 入力不正 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-AUTH-005 | 投稿権限なし |
| 404 | ERR-TASK-004 | 親タスク未存在 |
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

### 補足

- コメント投稿者本人またはチーム管理者（`OWNER` / `ADMIN`）のみ更新可能とする。
- 対象コメントまたは親タスクが削除済みの場合は `404` を返却する。
- サーバー側の `version` と不一致の場合は `409 ERR-COMMENT-006` を返却する。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | コメントID |
| taskId | number | タスクID |
| content | string | コメント本文 |
| createdBy.id | number | 投稿者ID |
| createdBy.name | string | 投稿者名 |
| createdAt | string | 作成日時 |
| updatedAt | string | 更新日時 |
| version | number | 楽観ロック用バージョン |

### レスポンス例

```json
{
  "id": 1,
  "taskId": 10,
  "content": "設計内容を再確認しました。",
  "createdBy": {
    "id": 1,
    "name": "山田太郎"
  },
  "createdAt": "2026-04-12T21:40:00",
  "updatedAt": "2026-04-12T22:10:00",
  "version": 1
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-COMMENT-001 / ERR-COMMENT-003 | 入力不正 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-COMMENT-004 | コメント更新権限なし |
| 404 | ERR-COMMENT-002 | コメント未存在 |
| 404 | ERR-TASK-004 | 親タスク未存在 |
| 409 | ERR-COMMENT-006 | 更新競合 |
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

### 補足

- コメント投稿者本人またはチーム管理者（`OWNER` / `ADMIN`）のみ削除可能とする。
- コメントは論理削除する。

### 正常レスポンス

- HTTPステータス: `204 No Content`

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

- 親タスクの所属チームにログインユーザーが所属している場合のみ参照可能とする。
- 親タスクが削除済みの場合は `404 ERR-TASK-004` を返却する。
- 論理削除済み添付ファイルは返却しない。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | 添付ファイルID |
| taskId | number | タスクID |
| originalFileName | string | 元ファイル名 |
| contentType | string | MIMEタイプ |
| fileSize | number | ファイルサイズ |
| storageType | string | 保存方式 |
| uploadedBy.id | number | 登録者ID |
| uploadedBy.name | string | 登録者名 |
| createdAt | string | 登録日時 |

### レスポンス例

```json
[
  {
    "id": 1,
    "taskId": 10,
    "originalFileName": "api-design-review.pdf",
    "contentType": "application/pdf",
    "fileSize": 1048576,
    "storageType": "LOCAL",
    "uploadedBy": {
      "id": 1,
      "name": "山田太郎"
    },
    "createdAt": "2026-04-12T21:45:00"
  }
]
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-AUTH-005 | 参照権限なし |
| 404 | ERR-TASK-004 | 親タスク未存在 |
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

### リクエストボディ

| 項目名 | 型 | 必須 | 説明 | バリデーション |
|---|---|---|---|---|
| file | file | ○ | 添付ファイル | ファイルサイズ・形式・件数上限を満たすこと |

### 補足

- 対象タスクを更新できる場合のみアップロード可能とする。
- 親タスクが削除済みの場合は `404 ERR-TASK-004` を返却する。

### 正常レスポンス

- HTTPステータス: `201 Created`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | 添付ファイルID |
| taskId | number | タスクID |
| originalFileName | string | 元ファイル名 |
| contentType | string | MIMEタイプ |
| fileSize | number | ファイルサイズ |
| storageType | string | 保存方式 |
| uploadedBy.id | number | 登録者ID |
| uploadedBy.name | string | 登録者名 |
| createdAt | string | 登録日時 |

### レスポンス例

```json
{
  "id": 1,
  "taskId": 10,
  "originalFileName": "api-design-review.pdf",
  "contentType": "application/pdf",
  "fileSize": 1048576,
  "storageType": "LOCAL",
  "uploadedBy": {
    "id": 1,
    "name": "山田太郎"
  },
  "createdAt": "2026-04-12T21:45:00"
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-FILE-001 / 005 / 006 / 007 / 008 | 入力不正 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-FILE-003 | 添付アップロード権限なし |
| 404 | ERR-TASK-004 | 親タスク未存在 |
| 500 | ERR-FILE-009 / ERR-SYS-999 / ERR-SYS-001 | ファイル保存失敗 / 想定外エラー / DBエラー |

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
| レスポンス形式 | バイナリ |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| attachmentId | number | ○ | 添付ファイルID |

### 補足

- 親タスクの所属チームにログインユーザーが所属している場合のみダウンロード可能とする。
- 対象添付ファイルが論理削除済みの場合は `404 ERR-FILE-002` を返却する。

### 正常レスポンス

- HTTPステータス: `200 OK`
- `Content-Disposition` にダウンロード用ファイル名を設定する。

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-AUTH-005 | 参照権限なし |
| 404 | ERR-FILE-002 | 添付ファイル未存在 |
| 500 | ERR-FILE-010 / ERR-SYS-999 / ERR-SYS-001 | ファイル取得失敗 / 想定外エラー / DBエラー |

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
| attachmentId | number | ○ | 添付ファイルID |

### 補足

- 添付登録者本人またはチーム管理者（`OWNER` / `ADMIN`）のみ削除可能とする。
- 添付ファイルメタ情報は論理削除する。

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

| 項目名 | 型 | 必須 | デフォルト | 説明 |
|---|---|---|---|---|
| page | number |  | 0 | ページ番号 |
| size | number |  | 20 | 1ページあたり件数 |
| unreadOnly | boolean |  | false | trueの場合は未読通知のみ取得 |

### 補足

- ログインユーザー宛の通知のみ返却する。
- 削除済みタスクに紐づく通知も返却対象とする。
- 通知からタスクへ遷移する場合の参照可否は、タスク詳細取得APIの判定に従う。

### 正常レスポンス

- HTTPステータス: `200 OK`

### レスポンス項目

| 項目名 | 型 | 説明 |
|---|---|---|
| content[].id | number | 通知ID |
| content[].activityLogId | number | アクティビティログID |
| content[].eventType | string | イベント種別 |
| content[].message | string | 通知メッセージ |
| content[].relatedTaskId | number | 関連タスクID |
| content[].relatedTaskTitle | string | 関連タスクタイトル |
| content[].targetType | string | 対象種別 |
| content[].targetId | number | 対象ID |
| content[].detailJson | object | 詳細情報 |
| content[].isRead | boolean | 既読フラグ |
| content[].readAt | string | 既読日時 |
| content[].createdAt | string | 作成日時 |
| page | number | ページ番号 |
| size | number | 1ページあたり件数 |
| totalElements | number | 総件数 |
| totalPages | number | 総ページ数 |

### レスポンス例

```json
{
  "content": [
    {
      "id": 1,
      "activityLogId": 100,
      "eventType": "TASK_ASSIGNED",
      "message": "タスク「レビュー対応」の担当者に設定されました。",
      "relatedTaskId": 10,
      "relatedTaskTitle": "レビュー対応",
      "targetType": "TASK",
      "targetId": 10,
      "detailJson": null,
      "isRead": false,
      "readAt": null,
      "createdAt": "2026-04-12T21:50:00"
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

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| unreadCount | number | 未読通知件数 |

### レスポンス例

```json
{
  "unreadCount": 3
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

- ログインユーザー宛の通知のみ既読化できる。
- 既読済み通知への再実行でも `200 OK` を返却する。

### 正常レスポンス

- HTTPステータス: `200 OK`
- レスポンス項目は「通知一覧取得」の `content[]` と同一

### レスポンス例

```json
{
  "id": 1,
  "activityLogId": 100,
  "eventType": "TASK_ASSIGNED",
  "message": "タスク「レビュー対応」の担当者に設定されました。",
  "relatedTaskId": 10,
  "relatedTaskTitle": "レビュー対応",
  "targetType": "TASK",
  "targetId": 10,
  "detailJson": null,
  "isRead": true,
  "readAt": "2026-04-12T22:00:00",
  "createdAt": "2026-04-12T21:50:00"
}
```

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

- ログインユーザー宛の未読通知をすべて既読化する。
- 未読通知が0件でも `204 No Content` を返却する。

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

| 項目名 | 型 | 必須 | デフォルト | 説明 |
|---|---|---|---|---|
| page | number |  | 0 | ページ番号 |
| size | number |  | 20 | 1ページあたり件数 |
| eventType | string |  |  | イベント種別絞り込み |

### 補足

- 親タスクの所属チームにログインユーザーが所属している場合のみ参照可能とする。
- 親タスクが削除済みの場合は `404 ERR-TASK-004` を返却する。
- ソート順は `createdAt DESC, id DESC` とする。

### 正常レスポンス

- HTTPステータス: `200 OK`

### レスポンス項目

| 項目名 | 型 | 説明 |
|---|---|---|
| content[].id | number | 履歴ID |
| content[].eventType | string | イベント種別 |
| content[].actor.id | number | 実行者ID |
| content[].actor.name | string | 実行者名 |
| content[].targetType | string | 対象種別 |
| content[].targetId | number | 対象ID |
| content[].taskId | number | タスクID |
| content[].summary | string | 履歴概要 |
| content[].detailJson | object | 詳細情報 |
| content[].createdAt | string | 作成日時 |
| page | number | ページ番号 |
| size | number | 1ページあたり件数 |
| totalElements | number | 総件数 |
| totalPages | number | 総ページ数 |

### レスポンス例

```json
{
  "content": [
    {
      "id": 1,
      "eventType": "TASK_UPDATED",
      "actor": {
        "id": 1,
        "name": "山田太郎"
      },
      "targetType": "TASK",
      "targetId": 10,
      "taskId": 10,
      "summary": "タスクを更新しました",
      "detailJson": {
        "changes": [
          {
            "field": "status",
            "oldValue": "TODO",
            "newValue": "DOING"
          }
        ]
      },
      "createdAt": "2026-04-12T22:00:00"
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
| 404 | ERR-TASK-004 | 親タスク未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

## 6.8 チームAPI

<details>
<summary>6.8.1 チーム作成</summary>

## 6.8.1 チーム作成

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | チーム作成 |
| メソッド | POST |
| パス | `/api/teams` |
| 認証 | 必要 |
| リクエスト形式 | JSON |
| レスポンス形式 | JSON |

### リクエストボディ

| 項目名 | 型 | 必須 | 説明 | バリデーション |
|---|---|---|---|---|
| name | string | ○ | チーム名 | 未入力不可、100文字以内 |
| description | string |  | チーム説明 | 1000文字以内 |

### リクエスト例

```json
{
  "name": "開発チームA",
  "description": "アプリ開発用チーム"
}
```

### 補足

- チーム名は前後の空白を除去して登録する。
- チーム説明は、指定された場合に前後の空白を除去して登録する。
- 作成者は `OWNER` としてチームメンバーに登録する。
- 同一作成者内で同じチーム名は登録不可とする。

### 正常レスポンス

- HTTPステータス: `201 Created`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | チームID |
| name | string | チーム名 |
| description | string | チーム説明 |
| myRole | string | 作成者のチーム内ロール。`OWNER` |
| memberCount | number | メンバー数 |
| createdAt | string | 作成日時 |
| updatedAt | string | 更新日時 |

### レスポンス例

```json
{
  "id": 1,
  "name": "開発チームA",
  "description": "アプリ開発用チーム",
  "myRole": "OWNER",
  "memberCount": 1,
  "createdAt": "2026-04-20T10:00:00",
  "updatedAt": "2026-04-20T10:00:00"
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-TEAM-001 | 入力不正 |
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 409 | ERR-TEAM-002 | 同一作成者内でチーム名重複 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.8.2 所属チーム一覧取得</summary>

## 6.8.2 所属チーム一覧取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | 所属チーム一覧取得 |
| メソッド | GET |
| パス | `/api/teams` |
| 認証 | 必要 |
| リクエスト形式 | Query Parameter |
| レスポンス形式 | JSON配列 |

### クエリパラメータ

なし

### 補足

- 自分が所属するチームのみ返却する。
- ソート順は `name ASC, id ASC` とする。
- 初回導線の判定は本APIの返却件数で行う。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | チームID |
| name | string | チーム名 |
| description | string | チーム説明 |
| myRole | string | ログインユーザーのチーム内ロール |
| memberCount | number | メンバー数 |
| updatedAt | string | 更新日時 |

### レスポンス例

```json
[
  {
    "id": 1,
    "name": "開発チームA",
    "description": "アプリ開発用チーム",
    "myRole": "OWNER",
    "memberCount": 3,
    "updatedAt": "2026-04-20T10:00:00"
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
<summary>6.8.3 チーム詳細取得</summary>

## 6.8.3 チーム詳細取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | チーム詳細取得 |
| メソッド | GET |
| パス | `/api/teams/{teamId}` |
| 認証 | 必要 |
| レスポンス形式 | JSON |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| teamId | number | ○ | チームID |

### 補足

- 所属ユーザーのみ参照可能とする。
- チーム基本情報のみ返却する。
- メンバー一覧は `GET /api/teams/{teamId}/members` で別取得する。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| id | number | チームID |
| name | string | チーム名 |
| description | string | チーム説明 |
| myRole | string | ログインユーザーのチーム内ロール |
| memberCount | number | メンバー数 |
| createdAt | string | 作成日時 |
| updatedAt | string | 更新日時 |

### レスポンス例

```json
{
  "id": 1,
  "name": "開発チームA",
  "description": "アプリ開発用チーム",
  "myRole": "OWNER",
  "memberCount": 3,
  "createdAt": "2026-04-20T10:00:00",
  "updatedAt": "2026-04-20T10:00:00"
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-TEAM-003 | チーム参照権限なし |
| 404 | ERR-TEAM-004 | チーム未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

## 6.9 チームメンバー参照API

<details>
<summary>6.9.1 チーム所属メンバー一覧取得</summary>

## 6.9.1 チーム所属メンバー一覧取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | チーム所属メンバー一覧取得 |
| メソッド | GET |
| パス | `/api/teams/{teamId}/members` |
| 認証 | 必要 |
| レスポンス形式 | JSON配列 |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| teamId | number | ○ | チームID |

### クエリパラメータ

なし

### 補足

- 所属ユーザーのみ参照可能とする。
- チーム詳細画面のメンバー一覧表示に利用する。
- 返却対象は当該チームに所属するユーザーのみとする。
- 表示順は `OWNER` → `ADMIN` → `MEMBER`、同一ロール内は `joinedAt ASC, userId ASC` とする。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| memberId | number | チームメンバーID |
| userId | number | ユーザーID |
| name | string | ユーザー名 |
| email | string | メールアドレス |
| role | string | チーム内ロール |
| joinedAt | string | チーム参加日時 |

### レスポンス例

```json
[
  {
    "memberId": 1,
    "userId": 1,
    "name": "山田太郎",
    "email": "yamada@example.com",
    "role": "OWNER",
    "joinedAt": "2026-04-20T10:00:00"
  },
  {
    "memberId": 2,
    "userId": 2,
    "name": "佐藤花子",
    "email": "sato@example.com",
    "role": "ADMIN",
    "joinedAt": "2026-04-20T10:30:00"
  },
  {
    "memberId": 3,
    "userId": 3,
    "name": "田中一郎",
    "email": "tanaka@example.com",
    "role": "MEMBER",
    "joinedAt": "2026-04-20T11:00:00"
  }
]
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-TEAM-003 | チーム参照権限なし |
| 404 | ERR-TEAM-004 | チーム未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.9.2 チーム追加候補ユーザー一覧取得</summary>

## 6.9.2 チーム追加候補ユーザー一覧取得

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | チーム追加候補ユーザー一覧取得 |
| メソッド | GET |
| パス | `/api/teams/{teamId}/available-users` |
| 認証 | 必要 |
| レスポンス形式 | JSON配列 |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| teamId | number | ○ | チームID |

### クエリパラメータ

なし

### 補足

- `OWNER` または `ADMIN` のみ参照可能とする。
- メンバー追加モーダルの候補取得に利用する。
- 返却対象は当該チームにまだ所属していない既存ユーザーとする。
- 検索条件なし・ページングなしで全件返却する。
- ソート順は `name ASC, userId ASC` とする。API項目上の `name` を表示名として扱う。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| userId | number | ユーザーID |
| name | string | ユーザー名 |
| email | string | メールアドレス |

### レスポンス例

```json
[
  {
    "userId": 4,
    "name": "鈴木次郎",
    "email": "suzuki@example.com"
  },
  {
    "userId": 5,
    "name": "高橋美咲",
    "email": "takahashi@example.com"
  }
]
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-TEAM-MEMBER-002 | メンバー追加権限なし |
| 404 | ERR-TEAM-004 | チーム未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

## 6.10 チームメンバーAPI

<details>
<summary>6.10.1 チームメンバー追加</summary>

## 6.10.1 チームメンバー追加

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | チームメンバー追加 |
| メソッド | POST |
| パス | `/api/teams/{teamId}/members` |
| 認証 | 必要 |
| リクエスト形式 | JSON |
| レスポンス形式 | JSON |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| teamId | number | ○ | チームID |

### リクエストボディ

| 項目名 | 型 | 必須 | 説明 | バリデーション |
|---|---|---|---|---|
| userId | number | ○ | 追加対象ユーザーID | 存在するユーザー |
| role | string | ○ | 追加時ロール | `ADMIN` / `MEMBER` |

### リクエスト例

```json
{
  "userId": 2,
  "role": "MEMBER"
}
```

### 補足

- `OWNER` または `ADMIN` のみ実行可能とする。
- `OWNER` は追加APIでは指定不可とする。
- 重複所属は不可とする。

### 正常レスポンス

- HTTPステータス: `201 Created`

| 項目名 | 型 | 説明 |
|---|---|---|
| memberId | number | チームメンバーID |
| userId | number | 追加対象ユーザーID |
| name | string | ユーザー名 |
| email | string | メールアドレス |
| role | string | チーム内ロール |
| joinedAt | string | 参加日時 |

### レスポンス例

```json
{
  "memberId": 2,
  "userId": 2,
  "name": "佐藤花子",
  "email": "sato@example.com",
  "role": "MEMBER",
  "joinedAt": "2026-04-20T10:30:00"
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-TEAM-MEMBER-001 | 入力不正 |
| 403 | ERR-TEAM-MEMBER-002 | メンバー追加権限なし |
| 404 | ERR-TEAM-004 | チーム未存在 |
| 404 | ERR-USR-002 | 対象ユーザー未存在 |
| 409 | ERR-TEAM-MEMBER-003 | 既に所属済み |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.10.2 チーム内ロール変更</summary>

## 6.10.2 チーム内ロール変更

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | チーム内ロール変更 |
| メソッド | PATCH |
| パス | `/api/teams/{teamId}/members/{memberId}` |
| 認証 | 必要 |
| リクエスト形式 | JSON |
| レスポンス形式 | JSON |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| teamId | number | ○ | チームID |
| memberId | number | ○ | 対象チームメンバーID |

### リクエストボディ

| 項目名 | 型 | 必須 | 説明 | バリデーション |
|---|---|---|---|---|
| role | string | ○ | 変更後ロール | `ADMIN` / `MEMBER` |

### リクエスト例

```json
{
  "role": "ADMIN"
}
```

### 補足

- `OWNER` のみ実行可能とする。
- `OWNER` 自身のロール変更は不可とする。
- `OWNER` を指定した場合は入力不正として扱う。
- 現在ロールと同じ値を指定した場合は冪等に `200 OK` を返却する。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| memberId | number | チームメンバーID |
| userId | number | 対象ユーザーID |
| name | string | ユーザー名 |
| email | string | メールアドレス |
| role | string | 更新後のチーム内ロール |
| joinedAt | string | 参加日時 |

### レスポンス例

```json
{
  "memberId": 2,
  "userId": 2,
  "name": "佐藤花子",
  "email": "sato@example.com",
  "role": "ADMIN",
  "joinedAt": "2026-04-20T10:30:00"
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 400 | ERR-TEAM-MEMBER-004 | 不正なロール指定 |
| 403 | ERR-TEAM-MEMBER-005 | ロール変更権限なし |
| 403 | ERR-TEAM-MEMBER-006 | OWNERのロールは変更不可 |
| 404 | ERR-TEAM-004 | チーム未存在 |
| 404 | ERR-TEAM-MEMBER-007 | 対象メンバー未存在 |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>

<details>
<summary>6.10.3 チームメンバー削除</summary>

## 6.10.3 チームメンバー削除

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | チームメンバー削除 |
| メソッド | DELETE |
| パス | `/api/teams/{teamId}/members/{memberId}` |
| 認証 | 必要 |
| レスポンス形式 | ボディなし |

### パスパラメータ

| 項目名 | 型 | 必須 | 説明 |
|---|---|---|---|
| teamId | number | ○ | チームID |
| memberId | number | ○ | 対象チームメンバーID |

### 補足

- `OWNER` または `ADMIN` のみ実行可能とする。
- `OWNER` 自身の削除は不可とする。
- `OWNER` の削除は不可とする。
- 対象ユーザーが当該チームの担当中タスクを持つ場合は削除不可とする。
- 担当中タスクがある場合は、先に担当変更または担当解除を行う。

### 正常レスポンス

- HTTPステータス: `204 No Content`

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 403 | ERR-TEAM-MEMBER-008 | メンバー削除権限なし |
| 403 | ERR-TEAM-MEMBER-009 | OWNERは削除できません |
| 404 | ERR-TEAM-004 | チーム未存在 |
| 404 | ERR-TEAM-MEMBER-007 | 対象メンバー未存在 |
| 409 | ERR-TEAM-MEMBER-010 | 担当中タスクがあるため削除できません |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>


## 6.11 認証テストAPI

<details>
<summary>6.11.1 認証コンテキスト確認</summary>

## 6.11.1 認証コンテキスト確認

### 基本情報

| 項目 | 内容 |
|---|---|
| API名 | 認証コンテキスト確認 |
| メソッド | GET |
| パス | `/api/auth-test/me` |
| 認証 | 必要 |
| レスポンス形式 | JSON |

### 補足

- 認証フィルタの動作確認に利用する簡易テスト用API。
- 現在の認証コンテキストから取得したユーザーIDとメールアドレスを返却する。

### 正常レスポンス

- HTTPステータス: `200 OK`

| 項目名 | 型 | 説明 |
|---|---|---|
| userId | number | ログインユーザーID |
| email | string | ログインユーザーのメールアドレス |

### レスポンス例

```json
{
  "userId": 1,
  "email": "yamada@example.com"
}
```

### 主な異常レスポンス

| HTTP | errorCode | 説明 |
|---:|---|---|
| 401 | ERR-AUTH-001 / 003 / 004 | 未認証 / 不正トークン / 期限切れ |
| 500 | ERR-SYS-999 / ERR-SYS-001 | 想定外エラー / DBエラー |

---


</details>


---


</details>

<details>
<summary>7. 補足事項</summary>

## 7. 補足事項

- API仕様は、実装時のController / DTO / Service構成と整合を取る。
- 日時項目は、原則としてISO 8601形式の文字列で返却する。
- APIで扱うIDは、原則として数値型で扱う。
- 入力値エラーの詳細メッセージは、エラー設計書および実装時のバリデーション定義に従う。
- 認証、認可、エラー制御は共通仕様に従う。
- タスク、コメント、添付ファイル、通知、履歴のチーム所属判定は、対象タスクの `teamId` に基づいて行う。
- チーム管理APIでは、チーム内ロール `OWNER` / `ADMIN` / `MEMBER` に基づいて操作可否を判定する。
- チーム削除、招待、招待リンク、メール通知、チームダッシュボード、チーム単位通知設定は、本API設計書の対象外とする。
- 認証テストAPIは、認証フィルタの動作確認用として扱う。

---


</details>

<details>
<summary>8. 備考</summary>

## 8. 備考

- 本設計書は、詳細設計、実装、APIテスト作成時の基準資料として利用する。
- API追加または変更時は、画面設計書、DB設計書、エラー設計書、テスト設計書との整合を確認する。
- 実装差異が発生した場合は、設計書側または実装側のどちらを正とするかを確認し、必要に応じて本設計書を更新する。
- API仕様を変更する場合は、改訂履歴に変更内容を記載する。

---


</details>
