import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { FinalizedView } from './FinalizedView'
import { renderWithProviders } from '@/test/test-utils'
import type { Finalization } from '@/api/endpoints'

const finalization: Finalization = {
  id: 'final-1',
  ideaId: 'idea-1',
  versionId: 'version-2',
  status: 'SUCCEEDED',
  errorCode: null,
  output: {
    techStack: {
      frontend: 'React + Vite',
      backend: 'Spring Boot',
      database: 'PostgreSQL',
      auth: 'Email OTP',
      hosting: 'Single VPS',
      aiLayer: 'LangChain4j',
      thirdParty: ['Sentry free tier'],
      estMonthlyCostAtMvp: '$10-25/month',
      rationale: 'Free tiers until traction.',
    },
    mvpRoadmap: [
      { phase: 'Foundation', goals: ['Auth', 'Schema'], estWeeks: '1-2' },
      { phase: 'Core loop', goals: ['Reminders'], estWeeks: '2-3' },
      { phase: 'Launch', goals: ['Billing'], estWeeks: '2' },
    ],
    pricingSuggestion: '$99/month per location',
    launchChecklist: ['Landing page', 'Waitlist form', 'Pricing page', 'Legal pages', 'Support email', 'Analytics'],
    validationNextSteps: ['Interview 10 managers', 'Fake-door test', 'Competitor pricing'],
    branding: { nameIdeas: ['NoShow Shield', 'ChairFill', 'DentalNudge'], domainTips: 'Prefer .com.' },
    risks: ['Data quality', 'API coverage', 'Reminder fatigue'],
  },
  buildPromptMd: '# Dental reminders — Build Prompt\n\n## Product\nBuild it.',
  agentsMd: '# AGENTS.md\n\n## Project\nRules here.',
  promptVersion: 'finalize-system-v1',
  createdAt: new Date().toISOString(),
  completedAt: new Date().toISOString(),
}

describe('FinalizedView', () => {
  it('shows the tech stack tab by default', () => {
    renderWithProviders(<FinalizedView finalization={finalization} />)
    expect(screen.getByText('React + Vite')).toBeInTheDocument()
    expect(screen.getByText('$10-25/month')).toBeInTheDocument()
    expect(screen.getByText('Sentry free tier')).toBeInTheDocument()
  })

  it('switches to the roadmap tab', async () => {
    const user = userEvent.setup()
    renderWithProviders(<FinalizedView finalization={finalization} />)

    await user.click(screen.getByRole('tab', { name: 'Roadmap' }))
    expect(screen.getByText('1. Foundation')).toBeInTheDocument()
    expect(screen.getByText('2-3 weeks')).toBeInTheDocument()
  })

  it('renders copyable build prompt and AGENTS.md tabs', async () => {
    const user = userEvent.setup()
    renderWithProviders(<FinalizedView finalization={finalization} />)

    await user.click(screen.getByRole('tab', { name: 'Build prompt' }))
    expect(screen.getByText('Dental reminders — Build Prompt')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Copy/ })).toBeInTheDocument()

    await user.click(screen.getByRole('tab', { name: 'AGENTS.md' }))
    expect(screen.getAllByText('AGENTS.md').length).toBeGreaterThan(1)
  })

  it('shows checklist, branding, and risks content', async () => {
    const user = userEvent.setup()
    renderWithProviders(<FinalizedView finalization={finalization} />)

    await user.click(screen.getByRole('tab', { name: 'Launch checklist' }))
    expect(screen.getByText('Landing page')).toBeInTheDocument()
    expect(screen.getByText('NoShow Shield')).toBeInTheDocument()

    await user.click(screen.getByRole('tab', { name: 'Risks' }))
    expect(screen.getByText('Reminder fatigue')).toBeInTheDocument()
    expect(screen.getByText('Interview 10 managers')).toBeInTheDocument()
  })
})
