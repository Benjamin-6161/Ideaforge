import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import { cn } from '@/lib/utils'

interface ScoreMeterProps {
  label: string
  score: number
  rationale?: string | null
  /** Competition: a higher raw score means a more crowded market (worse). */
  inverted?: boolean
  className?: string
}

/** Segmented 1-10 meter with a qualitative read and rationale tooltip. */
export function ScoreMeter({ label, score, rationale, inverted, className }: ScoreMeterProps) {
  const effective = inverted ? 11 - score : score
  const tone = effective >= 7 ? 'good' : effective >= 4 ? 'mid' : 'bad'
  const qualifier = qualifierFor(score, inverted)

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <div
          className={cn('space-y-1.5', className)}
          role="meter"
          aria-label={`${label}: ${score} out of 10. ${qualifier}.`}
          aria-valuenow={score}
          aria-valuemin={1}
          aria-valuemax={10}
        >
          <div className="flex items-baseline justify-between gap-2">
            <span className="text-xs font-medium text-muted-foreground">{label}</span>
            <span className="font-mono text-xs">{score}/10</span>
          </div>
          <div className="flex gap-0.5" aria-hidden="true">
            {Array.from({ length: 10 }, (_, index) => (
              <span
                key={index}
                className={cn(
                  'h-1.5 flex-1 rounded-full',
                  index < score
                    ? tone === 'good'
                      ? 'bg-success'
                      : tone === 'mid'
                        ? 'bg-warning'
                        : 'bg-destructive'
                    : 'bg-muted',
                )}
              />
            ))}
          </div>
          <p className="text-[11px] text-muted-foreground">{qualifier}</p>
        </div>
      </TooltipTrigger>
      <TooltipContent className="max-w-72">
        {rationale && rationale.trim().length > 0 ? rationale : defaultHint(label, inverted)}
      </TooltipContent>
    </Tooltip>
  )
}

function qualifierFor(score: number, inverted?: boolean): string {
  if (inverted) {
    if (score <= 3) return 'Open field'
    if (score <= 6) return 'Some rivalry'
    if (score <= 8) return 'Crowded'
    return 'Saturated'
  }
  if (score >= 9) return 'Excellent'
  if (score >= 7) return 'Strong'
  if (score >= 4) return 'Mixed'
  return 'Weak'
}

function defaultHint(label: string, inverted?: boolean): string {
  if (inverted) {
    return `${label} is shown as market saturation: 10 means saturated. Lower is better. This is an AI estimate.`
  }
  return `${label} is an AI-estimated score from 1 to 10.`
}
