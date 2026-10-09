import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import WorkPage from './WorkPage'
import { createTestQueryClient } from '@/test/test-utils'
import { TooltipProvider } from '@/components/ui/tooltip'
import type { ChatMessage, IdeaDetail } from '@/api/endpoints'

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
  getRecentMessages: vi.fn(),
  sendIdeaMessage: vi.fn(),
  revalidateIdea: vi.fn(),
  requestOtp: vi.fn(),
  verifyOtp: vi.fn(),
  googleLogin: vi.fn(),
  logout: vi.fn(),
}))

import { getIdea, getRecentMessages, sendIdeaMessage } from '@/api/endpoints'

const mockedGetIdea = vi.mocked(getIdea)
const mockedGetRecentMessages = vi.mocked(getRecentMessages)
const mockedSendIdeaMessage = vi.mocked(sendIdeaMessage)

function idea(status = 'IN_PROGRESS'): IdeaDetail {
  return {
    id: 'idea-1',
    fieldId: 'field-1',
    fieldName: 'Healthcare & Clinics',
    niche: null,
    title: 'Dental reminders',
    tagline: 'Fewer no-shows',
    status,
    favorite: false,
    archived: false,
    currentVersionId: 'version-1',
    versionNumber: 1,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    finalizedAt: null,
    revalidation: null,
    content: null,
    validation: null,
  } as unknown as IdeaDetail
}

const messages: ChatMessage[] = [
  {
    id: 'm1',
    role: 'USER',
    content: 'Is the demand real?',
    status: 'COMPLETED',
    resultingVersionId: null,
    resultingVersionNumber: null,
    createdAt: new Date().toISOString(),
  },
  {
    id: 'm2',
    role: 'ASSISTANT',
    content: 'Yes — scheduling pain is frequent.',
    status: 'COMPLETED',
    resultingVersionId: null,
    resultingVersionNumber: null,
    createdAt: new Date().toISOString(),
  },
]

function renderWorkPage() {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <MemoryRouter initialEntries={['/ideas/idea-1/work']}>
          <Routes>
            <Route path="/ideas/:id/work" element={<WorkPage />} />
          </Routes>
        </MemoryRouter>
      </TooltipProvider>
    </QueryClientProvider>,
  )
}

describe('WorkPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetIdea.mockResolvedValue(idea())
    mockedGetRecentMessages.mockResolvedValue(messages)
  })

  it('shows the idea and the conversation', async () => {
    renderWorkPage()

    expect(await screen.findByText('Dental reminders')).toBeInTheDocument()
    expect(screen.getAllByText('Is the demand real?').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Yes — scheduling pain is frequent.').length).toBeGreaterThan(0)
    expect(screen.getAllByText(/0.2 credits per message/).length).toBeGreaterThan(0)
  })

  it('sends a message with an idempotency key', async () => {
    mockedSendIdeaMessage.mockResolvedValue({
      userMessage: messages[0],
      assistantMessage: messages[1],
      newVersion: null,
    })
    const user = userEvent.setup()
    renderWorkPage()

    await screen.findByText('Dental reminders')
    await user.type(screen.getAllByLabelText('Message the agent')[0], 'Narrow the niche')
    await user.click(screen.getAllByRole('button', { name: 'Send message' })[0])

    await waitFor(() => expect(mockedSendIdeaMessage).toHaveBeenCalledTimes(1))
    const [ideaId, content, key] = mockedSendIdeaMessage.mock.calls[0]
    expect(ideaId).toBe('idea-1')
    expect(content).toBe('Narrow the niche')
    expect(typeof key).toBe('string')
    expect(key.length).toBeGreaterThan(10)
  })

  it('locks the composer for finalized ideas', async () => {
    mockedGetIdea.mockResolvedValue(idea('FINALIZED'))
    renderWorkPage()

    await screen.findByText('Dental reminders')
    expect(screen.getAllByLabelText('Message the agent')[0]).toBeDisabled()
    expect(screen.getByText('This idea is finalized and read-only.')).toBeInTheDocument()
  })
})
