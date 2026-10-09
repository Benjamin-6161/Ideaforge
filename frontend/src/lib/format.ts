/** Formats integer credit units (1 credit = 10 units) for display. */

export function formatUnits(units: number): string {
  const sign = units < 0 ? '-' : ''
  const abs = Math.abs(units)
  const whole = Math.floor(abs / 10)
  const tenth = abs % 10
  return `${sign}${whole}.${tenth}`
}

export function formatCredits(units: number): string {
  return `${formatUnits(units)} cr`
}

export function formatSignedUnits(units: number): string {
  if (units === 0) {
    return '0.0'
  }
  return `${units > 0 ? '+' : ''}${formatUnits(units)}`
}

export function formatMoney(cents: number, currency: string): string {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency,
  }).format(cents / 100)
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(iso))
}

export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(iso))
}

/** "demand 8/10" style labels for scores. */
export function formatScore(score: number): string {
  return `${score}/10`
}
