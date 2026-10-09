import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import IdeasPage from './IdeasPage'
import { renderWithProviders } from '@/test/test-utils'
import type { IdeaListPage } from '@/api/endpoints'

vi.mock('@/api/endpoints', () => ({
  getCredits: vi.fn(),
  getMe: vi.fn(),
  getLedger: vi.fn(),
  getPackages: vi.fn(),
  getFields: vi.fn(),
  getGenerationOptions: vi.fn(),
  getGeneration: vi.fn(),
  createGeneration: vi.fn(),
  getIdeas: vi.fn(),
  getIdea: vi.fn(),
  patchIdea: vi.fn(),
  deleteIdea: vi.fn(),
  getIdeaVersions: vi.fn(),
  getIdeaVersion: vi.fn(),
  restoreIdeaVersion: vi.fn(),
  requestOtp: vi.fn(),
  verifyOtp: vi.fn(),
  googleLogin: vi.fn(),
  logout: vi.fn(),
}))

import { getFields, getIdeas } from '@/api/endpoints'

const mockedGetIdeas = vi.mocked(getIdeas)
const mockedGetFields = vi.mocked(getFields)

function page(items: IdeaListPage['items']): IdeaListPage {
  return { items, page: 0, size: 12, totalItems: items.length, totalPages: 1 }
}

const idea: IdeaListPage['items'][number] = {
  id: 'idea-1',
  fieldId: 'field-1',
  fieldName: 'Healthcare & Clinics',
  niche: null,
  title: 'Claim denial autopilot',
  tagline: 'Appeals drafted automatically',
  status: 'GENERATED',
  favorite: false,
  archived: false,
  currentVersionId: 'version-1',
  versionNumber: 1,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  scores: {
    demand: 8,
    profitability: 7,
    competition: 4,
    novelty: 6,
    evidenceAvailable: true,
    depth: 'LIGHT',
    versionId: 'version-1',
  },
}

describe('IdeasPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetFields.mockResolvedValue([])
  })

  it('renders idea cards with scores', async () => {
    mockedGetIdeas.mockResolvedValue(page([idea]))
    renderWithProviders(<IdeasPage />, { route: '/ideas' })

    expect(await screen.findByText('Claim denial autopilot')).toBeInTheDocument()
    expect(screen.getByText('8/10')).toBeInTheDocument()
    expect(screen.getByText('page 1 of 1 · 1 ideas')).toBeInTheDocument()
  })

  it('shows the empty state when there are no ideas', async () => {
    mockedGetIdeas.mockResolvedValue(page([]))
    renderWithProviders(<IdeasPage />, { route: '/ideas' })

    expect(await screen.findByText('No ideas yet')).toBeInTheDocument()
  })

  it('passes filters to the API', async () => {
    mockedGetIdeas.mockResolvedValue(page([]))
    const user = userEvent.setup()
    renderWithProviders(<IdeasPage />, { route: '/ideas' })

    await user.selectOptions(screen.getByLabelText('Filter by status'), 'FINALIZED')
    await waitFor(() =>
      expect(mockedGetIdeas).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'FINALIZED' })),
    )

    await user.selectOptions(screen.getByLabelText('Sort ideas'), 'demand')
    await waitFor(() =>
      expect(mockedGetIdeas).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'demand' })),
    )
  })
})
