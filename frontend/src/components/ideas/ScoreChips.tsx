import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import type { IdeaScores } from '@/api/endpoints'
import { cn } from '@/lib/utils'

interface ScoreChipsProps {
  scores: IdeaScores
  compact?: boolean
}

/**
 * Four score chips. Competition is inverted for display: a low competition
 * score means a less crowded market, labelled accordingly.
 */
export function ScoreChips({ scores, compact }: ScoreChipsProps) {
  const items = [
    {
      label: 'Demand',
      value: scores.demand,
      tone: tone(scores.demand),
      detail: 'AI-estimated buyer pull, 1-10',
    },
    {
      label: 'Profit',
      value: scores.profitability,
      tone: tone(scores.profitability),
      detail: 'AI-estimated willingness to pay, 1-10',
    },
    {
      label: 'Competition',
      value: scores.competition,
      tone: tone(11 - scores.competition),
      detail: `Market saturation, ${scores.competition}/10 (10 = saturated) — lower is better`,
    },
    {
      label: 'Novelty',
      value: scores.novelty,
      tone: tone(scores.novelty),
      detail: 'How new the angle is, 1-10',
    },
  ]

  return (
    <dl className={cn('flex flex-wrap items-center gap-x-3 gap-y-1.5', compact && 'gap-x-2')}>
      {items.map((item) => (
        <div key={item.label} className="flex items-center gap-1.5">
          <dt className="text-[10px] text-muted-foreground">{item.label}</dt>
          <Tooltip>
            <TooltipTrigger asChild>
              <dd
                className={cn(
                  'rounded px-1 font-mono text-[11px] font-medium',
                  item.tone === 'good' && 'bg-success/10 text-success',
                  item.tone === 'mid' && 'bg-warning/10 text-warning',
                  item.tone === 'bad' && 'bg-destructive/10 text-destructive',
                )}
              >
                {item.value}/10
              </dd>
            </TooltipTrigger>
            <TooltipContent className="max-w-56">{item.detail}</TooltipContent>
          </Tooltip>
        </div>
      ))}
    </dl>
  )
}

function tone(value: number): 'good' | 'mid' | 'bad' {
  if (value >= 7) {
    return 'good'
  }
  if (value >= 4) {
    return 'mid'
  }
  return 'bad'
}
