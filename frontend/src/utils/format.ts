import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export function formatMoney(value: number | string | undefined | null): string {
  const num = Number(value ?? 0)
  return `¥${num.toFixed(2)}`
}

export function formatTime(value?: string): string {
  if (!value) return '—'
  return dayjs(value).format('YYYY-MM-DD HH:mm')
}

export function formatDate(value?: string): string {
  if (!value) return '—'
  return dayjs(value).format('YYYY-MM-DD')
}

export function fromNow(value?: string): string {
  if (!value) return '—'
  return dayjs(value).fromNow()
}

export function remainingSeconds(deadline?: string): number {
  if (!deadline) return 0
  return Math.max(0, dayjs(deadline).diff(dayjs(), 'second'))
}

export function formatCountdown(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

export function maskName(name: string): string {
  if (name.length <= 1) return name
  return `${name.slice(0, 1)}${'*'.repeat(Math.max(0, name.length - 2))}${name.slice(-1)}`
}
