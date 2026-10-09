import { describe, expect, it } from 'vitest'
import { cn } from '@/lib/utils'

describe('cn', () => {
  it('merges class names and resolves conflicts', () => {
    const hidden = false
    expect(cn('p-2', 'p-4')).toBe('p-4')
    expect(cn('text-sm', hidden && 'hidden', 'font-bold')).toBe('text-sm font-bold')
  })
})
