export function formatDate(value?: string | null) {
  if (!value) return '-'
  const normalized = value.length >= 10 ? value.slice(0, 10) : value
  const date = new Date(`${normalized}T00:00:00`)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric', month: '2-digit', day: '2-digit',
  }).format(date)
}

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

export function toDateInputValue(value?: string | null) {
  if (!value) return ''
  return value.slice(0, 10)
}
