import { FormEvent, useMemo, useState } from 'react';
import './App.css';
import { clearToken, getToken, saveToken } from './lib/auth';

type LoginResponse = {
  success?: boolean;
  data?: {
    token?: string;
    tokenType?: string;
    expiresIn?: number;
  };
  message?: string;
  token?: string;
  tokenType?: string;
  expiresIn?: number;
};

type ErrorResponse = {
  message?: string;
  error?: string;
};

const API_BASE_URL = 'http://localhost:8080';

function App() {
  const [email, setEmail] = useState('tasktester@example.com');
  const [password, setPassword] = useState('password123');
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [token, setToken] = useState<string | null>(() => getToken());

  const isLoggedIn = useMemo(() => Boolean(token), [token]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      });

      const rawText = await response.text();
      const json = rawText ? (JSON.parse(rawText) as LoginResponse & ErrorResponse) : {};

      if (!response.ok) {
        const message = json.message || json.error || 'ログインに失敗しました。';
        throw new Error(message);
      }

      const resolvedToken = json.data?.token ?? json.token;
      if (!resolvedToken) {
        throw new Error('レスポンスにtokenが含まれていません。');
      }

      saveToken(resolvedToken);
      setToken(resolvedToken);
      setSuccessMessage('ログインに成功しました。タスク一覧画面へ遷移します。');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'ログインに失敗しました。';
      setErrorMessage(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleLogout = () => {
    clearToken();
    setToken(null);
    setSuccessMessage('ログアウトしました。');
  };

  if (isLoggedIn) {
    return (
      <main className="page page--tasks">
        <section className="panel">
          <div className="panel__header">
            <div>
              <p className="eyebrow">Task Manager</p>
              <h1>タスク一覧</h1>
              <p className="subtext">WBSID 5.1 のログイン成功後遷移を確認するための仮一覧画面です。</p>
            </div>
            <button className="secondary-button" onClick={handleLogout}>
              ログアウト
            </button>
          </div>

          <div className="status-banner success">
            <strong>ログイン済み</strong>
            <span>JWTをlocalStorageに保存済みです。</span>
          </div>

          <div className="token-box">
            <p className="token-box__label">保存済みトークン（先頭のみ表示）</p>
            <code>{token?.slice(0, 48)}...</code>
          </div>

          <div className="task-placeholder">
            <div className="task-card">
              <h2>サンプルタスク 1</h2>
              <p>一覧画面の本実装は WBSID 5.4 で対応予定です。</p>
            </div>
            <div className="task-card">
              <h2>サンプルタスク 2</h2>
              <p>この画面はログイン成功後の遷移確認を目的に配置しています。</p>
            </div>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="page">
      <section className="panel panel--login">
        <div className="brand-block">
          <p className="eyebrow">Task Manager</p>
          <h1>ログイン</h1>
          <p className="subtext">
            JWT認証API（<code>/api/auth/login</code>）に接続し、成功時はトークンを保存して一覧画面へ遷移します。
          </p>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <label className="field">
            <span>メールアドレス</span>
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="tasktester@example.com"
              autoComplete="email"
              required
            />
          </label>

          <label className="field">
            <span>パスワード</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="password123"
              autoComplete="current-password"
              required
            />
          </label>

          {errorMessage && (
            <div className="status-banner error" role="alert">
              {errorMessage}
            </div>
          )}

          {successMessage && <div className="status-banner success">{successMessage}</div>}

          <button className="primary-button" type="submit" disabled={submitting}>
            {submitting ? 'ログイン中...' : 'ログイン'}
          </button>
        </form>

        <div className="hint-box">
          <p className="hint-box__title">確認ポイント</p>
          <ul>
            <li>成功時に JWT を localStorage に保存</li>
            <li>ログイン後に一覧画面へ遷移</li>
            <li>APIエラーメッセージを画面表示</li>
          </ul>
        </div>
      </section>
    </main>
  );
}

export default App;
