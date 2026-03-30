export function formatDateTime(value: string | null) {
  if (!value) {
    return '未设置'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ')
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

export function toDateTimeLocalInput(value: string | null) {
  if (!value) {
    return ''
  }

  return value.slice(0, 16)
}
