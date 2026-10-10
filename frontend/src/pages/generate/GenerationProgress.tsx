import { Check, Loader2, OctagonX } from 'lucide-react'
import type { GenerationRun } from '@/api/endpoints'
import { Progress } from '@/components/ui/progress'
import { failureMessageFor } from '@/lib/llm-errors'
import { cn } from '@/lib/utils'

const STAGES = [
  { key: 'IDEATING', label: 'Generating ideas', description: 'Drafting five candidate products for your constraints' },
  { key: 'RESEARCHING', label: 'Researching market', description: 'Competitor and demand signals, snippets only' },
  { key: 'SCORING', label: 'Scoring', description: 'Demand, profit, competition, and novelty estimates' },
] as const

export function GenerationProgress({ run }: { run: GenerationRun | undefined }) {
  const status = run?.status ?? 'PENDING'
  const stage = run?.progressStage ?? 'IDEATING'
  const activeIndex = STAGES.findIndex((item) => item.key === stage)
  const failed = status === 'FAILED'
  const finished = status === 'SUCCEEDED'

  const percent = finished ? 100 : Math.max(6, (Math.max(0, activeIndex) / STAGES.length) * 100)

  return (
    <div className="space-y-5">
      <div>
        <div className="mb-2 flex items-baseline justify-between">
          <p className="font-heading text-lg font-semibold">
            {failed ? 'Generation failed' : finished ? 'Generation complete' : 'Generating…'}
          </p>
          <p className="font-mono text-xs text-muted-foreground">
            {failed ? (run?.errorCode ?? 'ERROR') : finished ? 'done' : `${Math.round(percent)}%`}
          </p>
        </div>
        <Progress value={percent} aria-label="Generation progress" />
      </div>

      <ol className="space-y-3">
        {STAGES.map((item, index) => {
          const done = finished || index < activeIndex
          const active = !failed && !finished && index === activeIndex
          return (
            <li key={item.key} className="flex items-start gap-3">
              <span
                className={cn(
                  'mt-0.5 flex size-6 shrink-0 items-center justify-center rounded-full border text-xs',
                  done && 'border-success/40 bg-success/10 text-success',
                  active && 'border-signal/50 bg-signal/15 text-signal-ink',
                  !done && !active && 'border-border text-muted-foreground',
                )}
              >
                {finished || index < activeIndex ? (
                  <Check className="size-3.5" />
                ) : active ? (
                  <Loader2 className="size-3.5 animate-spin" />
                ) : (
                  <span className="font-mono text-[10px]">{index + 1}</span>
                )}
              </span>
              <div>
                <p className={cn('text-sm font-medium', !done && !active && 'text-muted-foreground')}>
                  {item.label}
                </p>
                <p className="text-xs text-muted-foreground">{item.description}</p>
              </div>
            </li>
          )
        })}
      </ol>

      {failed && (
        <div className="flex items-start gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-3 text-sm">
          <OctagonX className="mt-0.5 size-4 text-destructive" />
          <div>
            <p className="font-medium">{failureMessageFor(run?.errorCode)}</p>
            <p className="mt-0.5 text-xs text-muted-foreground">
              Your credits for this run were refunded automatically.
            </p>
          </div>
        </div>
      )}
    </div>
  )
}
