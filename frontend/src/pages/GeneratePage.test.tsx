import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import GeneratePage from './GeneratePage'
import { renderWithProviders } from '@/test/test-utils'
import type { GenerationRun } from '@/api/endpoints'

vi.mock('@/api/endpoints', () => ({
  getCredits: vi.fn(),
  getMe: vi.fn(),
  getLedger: vi.fn(),
  getPackages: vi.fn(),
  getFields: vi.fn(),
  getGenerationOptions: vi.fn(),
  getGeneration: vi.fn(),
  createGeneration: vi.fn(),
  requestOtp: vi.fn(),
  verifyOtp: vi.fn(),
  googleLogin: vi.fn(),
  logout: vi.fn(),
}))

import { createGeneration, getCredits, getFields, getGeneration, getGenerationOptions } from '@/api/endpoints'

const mockedGetFields = vi.mocked(getFields)
const mockedGetCredits = vi.mocked(getCredits)
const mockedCreateGeneration = vi.mocked(createGeneration)
const mockedGetGeneration = vi.mocked(getGeneration)
const mockedGetGenerationOptions = vi.mocked(getGenerationOptions)

const field = {
  id: 'field-1',
  slug: 'healthcare-clinics',
  name: 'Healthcare & Clinics',
  description: 'Clinics and practices',
  icon: 'stethoscope',
  niches: [],
}

function credits(balanceUnits: number) {
  return {
    balanceUnits,
    costs: [
      { action: 'GENERATE_IDEAS', costUnits: 20 },
      { action: 'WORK_MESSAGE', costUnits: 2 },
    ],
  }
}

function succeededRun(): GenerationRun {
  return {
    id: 'run-1',
    status: 'SUCCEEDED',
    progressStage: 'DONE',
    errorCode: null,
    fieldId: 'field-1',
    niche: null,
    createdAt: new Date().toISOString(),
    completedAt: new Date().toISOString(),
    ideas: [
      {
        id: 'idea-1',
        title: 'Claim denial autopilot',
        tagline: 'Appeals drafted automatically',
        fieldId: 'field-1',
        status: 'GENERATED',
        currentVersionId: 'version-1',
        versionNumber: 1,
        scores: {
          demand: 8,
          profitability: 7,
          competition: 4,
          novelty: 6,
          evidenceAvailable: true,
          depth: 'LIGHT',
          versionId: 'version-1',
        },
      },
    ],
  }
}

describe('GeneratePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetFields.mockResolvedValue([field])
    mockedGetGenerationOptions.mockResolvedValue({
      targetCustomer: [{ value: 'ANY', label: 'Any' }],
      customerSize: [{ value: 'ANY', label: 'Any' }],
      builderProfile: [{ value: 'SOLO_FOUNDER', label: 'Solo founder' }],
      timeToMvp: [{ value: 'NO_LIMIT', label: 'No limit' }],
      startingBudget: [{ value: 'MODERATE', label: 'Moderate' }],
      aiDepth: [{ value: 'ANY', label: 'Any' }],
      aiCapabilities: [{ value: 'TEXT_LLM', label: 'Text / LLM' }],
      monetization: [{ value: 'ANY', label: 'Any' }],
      region: [{ value: 'GLOBAL', label: 'Global' }],
      noveltyPreference: [{ value: 'BALANCED', label: 'Balanced' }],
    })
  })

  it('shows the cost and balance in credits', async () => {
    mockedGetCredits.mockResolvedValue(credits(50))
    const user = userEvent.setup()
    renderWithProviders(<GeneratePage />, { route: '/generate' })

    // Selecting a field also proves the form is ready; then the cost line
    // must reflect the mocked balance.
    await user.click(await screen.findByRole('combobox', { name: 'Field' }))
    await user.click(await screen.findByText('Healthcare & Clinics'))
    await waitFor(() => expect(screen.getByRole('button', { name: /Generate ideas/ })).toBeEnabled())
    expect(
      screen.getByText((_, element) =>
        element?.tagName === 'P' && element.textContent?.includes('you have') === true,
      ),
    ).toHaveTextContent('Costs 2.0 credits — you have 5.0 cr')
  })

  it('disables generation when credits are insufficient', async () => {
    mockedGetCredits.mockResolvedValue(credits(10))
    renderWithProviders(<GeneratePage />, { route: '/generate' })

    expect(await screen.findByText(/Not enough credits/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Generate ideas/ })).toBeDisabled()
  })

  it('submits with an idempotency key and renders the scored ideas', async () => {
    mockedGetCredits.mockResolvedValue(credits(50))
    mockedCreateGeneration.mockResolvedValue({ ...succeededRun(), status: 'RUNNING', progressStage: 'IDEATING', ideas: [] })
    mockedGetGeneration.mockResolvedValue(succeededRun())

    const user = userEvent.setup()
    renderWithProviders(<GeneratePage />, { route: '/generate' })

    await user.click(await screen.findByRole('combobox', { name: 'Field' }))
    await user.click(await screen.findByText('Healthcare & Clinics'))
    await user.click(screen.getByRole('button', { name: /Generate ideas/ }))

    await waitFor(() => expect(mockedCreateGeneration).toHaveBeenCalledTimes(1))
    const [body, key] = mockedCreateGeneration.mock.calls[0]
    expect(body.fieldId).toBe('field-1')
    expect(typeof key).toBe('string')
    expect(key.length).toBeGreaterThan(10)

    expect(await screen.findByText('Claim denial autopilot')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Open idea/ })).toHaveAttribute('href', '/ideas/idea-1')
    expect(screen.getByText('8/10')).toBeInTheDocument()
  })
})
