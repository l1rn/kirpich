import type { Application } from '@/types/application'

export const rub = (n: number) => new Intl.NumberFormat('ru-RU').format(n) + ' ₽'

export const date = (iso: string) =>
  new Date(iso).toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })

export const statusMeta = (s: Application['status']) =>
  ({
    APPROVED: { label: 'Проверка пройдена', tone: 'ok' },
    REJECTED: { label: 'Проверка не пройдена', tone: 'bad' },
    SUBMITTED: { label: 'На рассмотрении', tone: 'neutral' },
  } as const)[s]

export const checkTone = (code: string) =>
  code.endsWith('_PASSED') ? 'ok' : code.endsWith('_FAILED') ? 'bad' : 'neutral'

export const checkLabel = (code: string) =>
  ({ ok: 'Пройдена', bad: 'Не пройдена', neutral: 'Не проверялась' })[checkTone(code)]