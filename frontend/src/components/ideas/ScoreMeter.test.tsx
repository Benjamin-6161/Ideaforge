import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ScoreMeter } from './ScoreMeter'
import { TooltipProvider } from '@/components/ui/tooltip'

function renderMeter(props: Parameters<typeof ScoreMeter>[0]) {
  return render(
    <TooltipProvider>
      <ScoreMeter {...props} />
    </TooltipProvider>,
  )
}

describe('ScoreMeter', () => {
  it('shows the value and a qualitative read', () => {
    renderMeter({ label: 'Demand', score: 9, rationale: 'Repeated complaints in forums' })
    expect(screen.getByRole('meter', { name: /Demand: 9 out of 10\. Excellent/ })).toBeInTheDocument()
    expect(screen.getByText('9/10')).toBeInTheDocument()
    expect(screen.getByText('Excellent')).toBeInTheDocument()
  })

  it('labels competition as inverted saturation', () => {
    renderMeter({ label: 'Competition', score: 9, inverted: true })
    expect(screen.getByText('Saturated')).toBeInTheDocument()
    expect(screen.queryByText('Excellent')).not.toBeInTheDocument()
  })

  it('shows open field for low competition', () => {
    renderMeter({ label: 'Competition', score: 2, inverted: true })
    expect(screen.getByText('Open field')).toBeInTheDocument()
  })

  it('marks weak demand', () => {
    renderMeter({ label: 'Demand', score: 2 })
    expect(screen.getByText('Weak')).toBeInTheDocument()
  })
})
