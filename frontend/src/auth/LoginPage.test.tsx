import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginPage from './LoginPage'
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

import { getMe, requestOtp, verifyOtp } from '@/api/endpoints'

const mockedGetMe = vi.mocked(getMe)
const mockedRequestOtp = vi.mocked(requestOtp)
const mockedVerifyOtp = vi.mocked(verifyOtp)

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetMe.mockRejectedValue(Object.assign(new Error('unauthorized'), { status: 401 }))
  })

  it('validates the email before sending', async () => {
    const user = userEvent.setup()
    renderWithProviders(<LoginPage />, { route: '/login' })

    await user.click(screen.getByRole('button', { name: 'Email me a code' }))

    expect(await screen.findByText('Enter a valid email address')).toBeInTheDocument()
    expect(mockedRequestOtp).not.toHaveBeenCalled()
  })

  it('moves to the code step after requesting a code', async () => {
    mockedRequestOtp.mockResolvedValue(undefined)
    const user = userEvent.setup()
    renderWithProviders(<LoginPage />, { route: '/login' })

    await user.type(screen.getByLabelText('Email'), 'founder@example.com')
    await user.click(screen.getByRole('button', { name: 'Email me a code' }))

    expect(mockedRequestOtp).toHaveBeenCalledWith('founder@example.com', null)
    expect(await screen.findByText('Check your email')).toBeInTheDocument()
    expect(screen.getByText('founder@example.com')).toBeInTheDocument()
  })

  it('verifies the six-digit code', async () => {
    mockedRequestOtp.mockResolvedValue(undefined)
    mockedVerifyOtp.mockResolvedValue({
      id: 'user-1',
      email: 'founder@example.com',
      displayName: null,
      avatarUrl: null,
      role: 'USER',
    })
    const user = userEvent.setup()
    renderWithProviders(<LoginPage />, { route: '/login' })

    await user.type(screen.getByLabelText('Email'), 'founder@example.com')
    await user.click(screen.getByRole('button', { name: 'Email me a code' }))
    await screen.findByText('Check your email')

    await user.type(screen.getByLabelText('Digit 1'), '000000')
    await user.click(screen.getByRole('button', { name: 'Verify code' }))

    await waitFor(() =>
      expect(mockedVerifyOtp).toHaveBeenCalledWith('founder@example.com', '000000'),
    )
  })
})
