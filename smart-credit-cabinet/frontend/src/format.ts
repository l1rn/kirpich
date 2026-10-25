const rub = new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 0 });
const dateTime = new Intl.DateTimeFormat('ru-RU', { dateStyle: 'short', timeStyle: 'medium' });
const date = new Intl.DateTimeFormat('ru-RU', { dateStyle: 'long' });

export const formatRub = (value: number) => rub.format(value);
export const formatDateTime = (iso: string) => dateTime.format(new Date(iso));
export const formatDate = (iso: string) => date.format(new Date(iso));
export const formatPercent = (value: number) => `${value.toLocaleString('ru-RU')}%`;

/** Аннуитетный платёж — та же формула, что в бэкенде (Finance.annuityPayment). */
export function annuityPayment(amount: number, annualRate: number, months: number): number {
  const rate = annualRate / 12 / 100;
  if (rate === 0) return Math.round(amount / months);
  return Math.round((amount * rate) / (1 - Math.pow(1 + rate, -months)));
}

export const rubParser = (value: string | undefined) => Number((value ?? '').replace(/[^\d]/g, ''));
export const rubFormatter = (value: number | string | undefined) =>
  `${value ?? ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
