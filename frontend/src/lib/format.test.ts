import { describe, expect, it } from 'vitest'
import {
  formatCredits,
  formatMoney,
  formatScore,
  formatSignedUnits,
  formatUnits,
} from '@/lib/format'

describe('formatUnits', () => {
  it('shows one decimal from integer units', () => {
    expect(formatUnits(50)).toBe('5.0')
    expect(formatUnits(2)).toBe('0.2')
    expect(formatUnits(1000)).toBe('100.0')
    expect(formatUnits(142)).toBe('14.2')
  })

  it('keeps the sign for negative units', () => {
    expect(formatUnits(-20)).toBe('-2.0')
    expect(formatUnits(-3)).toBe('-0.3')
  })

  it('formats signed deltas', () => {
    expect(formatSignedUnits(50)).toBe('+5.0')
    expect(formatSignedUnits(-20)).toBe('-2.0')
    expect(formatSignedUnits(0)).toBe('0.0')
  })

  it('formats credits and scores', () => {
    expect(formatCredits(50)).toBe('5.0 cr')
    expect(formatScore(8)).toBe('8/10')
  })
})

describe('formatMoney', () => {
  it('formats cents as currency', () => {
    expect(formatMoney(500, 'USD')).toMatch(/\$5\.00/)
  })
})
