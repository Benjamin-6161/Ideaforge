import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router'
import type { GeneratedIdeaSummary } from '@/api/endpoints'
import { ScoreChips } from '@/components/ideas/ScoreChips'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export function IdeaCard({ idea }: { idea: GeneratedIdeaSummary }) {
  return (
    <Card className="flex flex-col">
      <CardHeader>
        <CardTitle className="font-heading text-lg leading-snug">{idea.title}</CardTitle>
        <CardDescription className="line-clamp-2">{idea.tagline}</CardDescription>
      </CardHeader>
      <CardContent className="mt-auto space-y-3">
        {idea.scores ? <ScoreChips scores={idea.scores} compact /> : null}
        <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1 text-xs text-muted-foreground">
          <span className="whitespace-nowrap font-mono">
            v{idea.versionNumber} · {idea.status.toLowerCase()}
            {idea.scores && !idea.scores.evidenceAvailable ? ' · no evidence' : ''}
          </span>
          <Link
            to={`/ideas/${idea.id}`}
            className="inline-flex items-center gap-1 whitespace-nowrap text-foreground underline-offset-2 hover:underline"
          >
            Open idea <ArrowRight className="size-3.5" />
          </Link>
        </div>
      </CardContent>
    </Card>
  )
}
