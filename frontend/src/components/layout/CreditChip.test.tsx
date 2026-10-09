import { screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CreditChip } from './CreditChip'
import { renderWithProviders } from '@/test/test-utils'

vi.mock('@/api/endpoints', () => ({
  getCredits: vi.fn(),
  getMe: vi.fn(),
  getLedger: vi.fn(),
  getPackages: vi.fn(),
  getFields: vi.fn(),
  getGenerationOptions: vi.fn(),
  requestOtp: vi.fn(),
  verifyOtp: vi.fn(),
  googleLogin: vi.fn(),
  logout: vi.fn(),
}))

import { getCredits } from '@/api/endpoints'

const mockedGetCredits = vi.mocked(getCredits)

describe('CreditChip', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows the balance from the API in credits with one decimal', async () => {
    mockedGetCredits.mockResolvedValue({
      balanceUnits: 142,
      costs: [],
    })

    renderWithProviders(<CreditChip />)

    await waitFor(() => expect(screen.getByText('14.2 cr')).toBeInTheDocument())
    expect(screen.getByRole('link', { name: /credit balance: 14\.2 cr/i })).toHaveAttribute('href', '/credits')
  })

  it('shows a placeholder while loading', () => {
    mockedGetCredits.mockReturnValue(new Promise(() => undefined))
    renderWithProviders(<CreditChip />)
    expect(screen.getByText('—')).toBeInTheDocument()
  })

  it('degrades gracefully when the API fails', async () => {
    mockedGetCredits.mockRejectedValue(new Error('offline'))
    const { queryClient } = renderWithProviders(<CreditChip />)
    await waitFor(() => expect(screen.getByText('n/a')).toBeInTheDocument())
    queryClient.clear()
  })
})
