export function formatBusinessDate(value) {
  return value ? value.replaceAll('-', '/') : '-'
}

export function formatDateTime(value) {
  if (!value) return '-'

  return new Intl.DateTimeFormat('ja-JP', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'Asia/Tokyo'
  }).format(new Date(value))
}
