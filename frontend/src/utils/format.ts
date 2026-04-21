/**
 * 日付文字列を日本語ロケールの年月日表示へ変換する。
 */
export function formatDate(value?: string | null) {
  if (!value) return '-'
  const normalized = value.length >= 10 ? value.slice(0, 10) : value
  // yyyy-MM-ddをローカル日付として扱い、タイムゾーン差で日付がずれないようにする。
  const date = new Date(`${normalized}T00:00:00`)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(date)
}

/**
 * 日時文字列を日本語ロケールの年月日時分表示へ変換する。
 */
export function formatDateTime(value?: string | null) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

/**
 * APIの日付/日時文字列をdate inputへ渡せるyyyy-MM-dd形式へ変換する。
 */
export function toDateInputValue(value?: string | null) {
  if (!value) return ''
  return value.slice(0, 10)
}
