import {
  AlertTriangle,
  Archive,
  ArchiveRestore,
  ArrowLeft,
  Check,
  FileText,
  History,
  Link2,
  Lock,
  MessageSquare,
  Star,
  Trash2,
  Trophy,
} from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import {
  useDeleteIdea,
  useIdea,
  useIdeaVersions,
  usePatchIdea,
  useRestoreIdeaVersion,
} from '@/auth/use-auth'
import type { IdeaDetail, ValidationDetail } from '@/api/endpoints'
import { ScoreMeter } from '@/components/ideas/ScoreMeter'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import { formatDateTime } from '@/lib/format'
import { cn } from '@/lib/utils'

export default function IdeaDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { data: idea, isPending, isError } = useIdea(id)
  const patchIdea = usePatchIdea()
  const deleteIdea = useDeleteIdea()

  if (isPending) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-6 w-40" />
        <Skeleton className="h-10 w-2/3" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }
  if (isError || !idea) {
    return (
      <Alert variant="destructive">
        <AlertDescription>
          This idea could not be loaded. It may have been deleted.{' '}
          <Link to="/ideas" className="underline">
            Back to my ideas
          </Link>
          .
        </AlertDescription>
      </Alert>
    )
  }

  const content = idea.content
  const finalized = idea.status === 'FINALIZED'

  return (
    <div className="space-y-8">
      <div>
        <Link
          to="/ideas"
          className="inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="size-3.5" /> My ideas
        </Link>
      </div>

      <header className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0 max-w-3xl">
          <div className="flex flex-wrap items-center gap-1.5">
            <Badge variant={finalized ? 'default' : 'secondary'} className="font-mono text-[10px]">
              {finalized ? 'Finalized' : idea.status === 'IN_PROGRESS' ? 'In progress' : 'Generated'}
            </Badge>
            <Badge variant="outline" className="font-mono text-[10px]">
              v{idea.versionNumber}
            </Badge>
            {idea.validation && (
              <Badge variant="outline" className="font-mono text-[10px]">
                {idea.validation.scores.depth === 'DEEP' ? 'Deep check' : 'Light check'}
              </Badge>
            )}
            {idea.archived && (
              <Badge variant="outline" className="font-mono text-[10px]">
                Archived
              </Badge>
            )}
          </div>
          <h1 className="mt-3 text-3xl leading-tight">{idea.title}</h1>
          <p className="mt-2 text-muted-foreground">{idea.tagline}</p>
          <p className="mt-1 text-xs text-muted-foreground">
            {idea.fieldName}
            {idea.niche ? ` · ${idea.niche}` : ''} · updated {formatDateTime(idea.updatedAt)}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant="outline"
            size="icon"
            aria-label={idea.favorite ? 'Remove from favorites' : 'Add to favorites'}
            aria-pressed={idea.favorite}
            onClick={() => patchIdea.mutate({ id: idea.id, body: { isFavorite: !idea.favorite } })}
          >
            <Star className={cn('size-4', idea.favorite && 'fill-signal text-signal')} />
          </Button>
          <Button
            variant="outline"
            onClick={() =>
              patchIdea.mutate(
                { id: idea.id, body: { isArchived: !idea.archived } },
                { onSuccess: () => toast.success(idea.archived ? 'Idea restored.' : 'Idea archived.') },
              )
            }
          >
            {idea.archived ? <ArchiveRestore /> : <Archive />}
            {idea.archived ? 'Unarchive' : 'Archive'}
          </Button>
          <DeleteButton
            onConfirm={() =>
              deleteIdea.mutate(idea.id, {
                onSuccess: () => {
                  toast.success('Idea deleted.')
                  navigate('/ideas')
                },
              })
            }
          />
        </div>
      </header>

      {finalized && (
        <Alert>
          <Lock />
          <AlertTitle>This idea is finalized and read-only</AlertTitle>
          <AlertDescription>
            The version history stays available below. Favorites and archive still work.
          </AlertDescription>
        </Alert>
      )}

      {idea.validation?.stale && (
        <Alert>
          <AlertTriangle />
          <AlertTitle>
            Scores were computed for version {idea.validation.validatedVersionNumber}
          </AlertTitle>
          <AlertDescription>
            The idea content changed since this check. Re-validation will run a deep check against the
            current version.
          </AlertDescription>
        </Alert>
      )}

      {content ? (
        <>
          <section className="grid gap-6 rounded-xl border bg-card p-5 sm:grid-cols-2 lg:grid-cols-4">
            <ValidationBlock validation={idea.validation} />
          </section>

          {idea.validation && <EvidenceSection validation={idea.validation} />}

          <div className="grid gap-6 lg:grid-cols-2">
            <Section title="Problem">
              <Field label="Statement">{content.problem.statement}</Field>
              <Field label="Who feels it">{content.problem.whoFeelsIt}</Field>
              <Field label="Today's workaround">{content.problem.currentWorkaround}</Field>
              <Field label="Pain severity">{content.problem.painSeverity}</Field>
              <Field label="Frequency">{content.problem.frequency}</Field>
            </Section>

            <Section title="Target customer">
              <Field label="Persona">{content.targetCustomer.persona}</Field>
              <Field label="Segment">{content.targetCustomer.segment}</Field>
              <Field label="Size">{content.targetCustomer.size}</Field>
              <Field label="Buyer">{content.targetCustomer.buyer}</Field>
            </Section>

            <Section title="Solution">
              <p className="text-sm">{content.solution.overview}</p>
              <div className="space-y-2">
                {content.solution.keyFeatures.map((feature) => (
                  <div key={feature.name} className="rounded-lg border p-3">
                    <p className="text-sm font-medium">{feature.name}</p>
                    <p className="text-xs text-muted-foreground">{feature.description}</p>
                  </div>
                ))}
              </div>
              <Field label="MVP scope">
                <ul className="list-inside list-disc space-y-1">
                  {content.solution.mvpScope.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              </Field>
            </Section>

            <Section title="AI role">
              <p className="text-sm">{content.aiRole.summary}</p>
              <div className="flex flex-wrap gap-1.5">
                {content.aiRole.capabilities.map((capability) => (
                  <Badge key={capability} variant="secondary" className="text-[10px]">
                    {capability}
                  </Badge>
                ))}
              </div>
              <Field label="Why AI is essential">{content.aiRole.whyAiIsEssential}</Field>
              <Field label="Data needed">{content.aiRole.dataNeeded}</Field>
              <Field label="Accuracy risks">{content.aiRole.accuracyRisks}</Field>
            </Section>

            <Section title="Differentiation">
              <Field label="Wedge">{content.differentiation.wedge}</Field>
              <Field label="Moat">{content.differentiation.moat}</Field>
              <Field label="Why now">{content.differentiation.whyNow}</Field>
            </Section>

            <Section title="Monetization">
              <Field label="Model">{content.monetization.model}</Field>
              <Field label="Pricing example">{content.monetization.pricingExample}</Field>
              <Field label="Willingness to pay">{content.monetization.willingnessToPayNotes}</Field>
            </Section>

            <Section title="Go to market">
              <Field label="First customers">{content.goToMarket.firstCustomers}</Field>
              <Field label="Channels">
                <ul className="list-inside list-disc space-y-1">
                  {content.goToMarket.channels.map((channel) => (
                    <li key={channel}>{channel}</li>
                  ))}
                </ul>
              </Field>
            </Section>

            <Section title="Build estimate">
              <Field label="Complexity">{content.buildEstimate.complexity}</Field>
              <Field label="MVP time">{content.buildEstimate.mvpTimeEstimate}</Field>
              <Field label="Suggested team">{content.buildEstimate.suggestedTeam}</Field>
            </Section>
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <ListSection title="Caveats" items={content.caveats} />
            <ListSection title="Regulatory notes" items={content.regulatoryNotes} empty="None flagged." />
            <ListSection title="Open questions" items={content.openQuestions} />
          </div>
        </>
      ) : (
        <Alert variant="destructive">
          <AlertDescription>The idea content could not be read.</AlertDescription>
        </Alert>
      )}

      <Separator />

      <section className="flex flex-wrap items-center gap-3">
        <WorkButton idea={idea} />
        <FinalizeButton idea={idea} />
        <VersionsDialog ideaId={idea.id} />
      </section>
    </div>
  )
}

function ValidationBlock({ validation }: { validation: ValidationDetail | null }) {
  if (!validation) {
    return (
      <div className="sm:col-span-2 lg:col-span-4">
        <p className="text-sm font-medium">No validation yet</p>
        <p className="text-xs text-muted-foreground">Scores appear after a check completes.</p>
      </div>
    )
  }
  return (
    <>
      <ScoreMeter
        label="Demand"
        score={validation.scores.demand}
        rationale={validation.rationale.demand}
      />
      <ScoreMeter
        label="Profitability"
        score={validation.scores.profitability}
        rationale={validation.rationale.profitability}
      />
      <ScoreMeter
        label="Competition"
        score={validation.scores.competition}
        rationale={validation.rationale.competition}
        inverted
      />
      <ScoreMeter
        label="Novelty"
        score={validation.scores.novelty}
        rationale={validation.rationale.novelty}
      />
    </>
  )
}

function EvidenceSection({ validation }: { validation: ValidationDetail }) {
  return (
    <section className="space-y-3">
      {!validation.scores.evidenceAvailable && (
        <Alert>
          <AlertTriangle />
          <AlertTitle>No usable evidence was found</AlertTitle>
          <AlertDescription>
            Scores are based on general market knowledge only. Treat them as directional.
          </AlertDescription>
        </Alert>
      )}
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="rounded-xl border bg-card p-5">
          <h2 className="flex items-center gap-2 font-heading text-lg font-semibold">
            <Link2 className="size-4" /> Sources
          </h2>
          {validation.evidence.length === 0 ? (
            <p className="mt-2 text-sm text-muted-foreground">
              No sources were captured for this check.
            </p>
          ) : (
            <ol className="mt-3 space-y-3">
              {validation.evidence.map((item, index) => (
                <li key={`${item.url}-${index}`} className="flex gap-3 text-sm">
                  <span className="font-mono text-xs text-muted-foreground">[{index}]</span>
                  <div className="min-w-0">
                    <a
                      href={item.url}
                      target="_blank"
                      rel="noopener noreferrer nofollow"
                      className="font-medium underline-offset-2 hover:underline"
                    >
                      {item.title || item.url}
                    </a>
                    <p className="text-xs text-muted-foreground">
                      {item.kind} · {item.url}
                    </p>
                    {item.snippet && (
                      <p className="mt-0.5 line-clamp-2 text-xs text-muted-foreground">{item.snippet}</p>
                    )}
                  </div>
                </li>
              ))}
            </ol>
          )}
        </div>

        <div className="rounded-xl border bg-card p-5">
          <h2 className="flex items-center gap-2 font-heading text-lg font-semibold">
            <Trophy className="size-4" /> Competitors
          </h2>
          {validation.competitors.length === 0 ? (
            <p className="mt-2 text-sm text-muted-foreground">No direct competitors were found.</p>
          ) : (
            <ul className="mt-3 space-y-3">
              {validation.competitors.map((competitor) => (
                <li key={competitor.name} className="text-sm">
                  <p className="font-medium">{competitor.name}</p>
                  {competitor.url ? (
                    <a
                      href={competitor.url}
                      target="_blank"
                      rel="noopener noreferrer nofollow"
                      className="text-xs text-muted-foreground underline-offset-2 hover:underline"
                    >
                      {competitor.url}
                    </a>
                  ) : null}
                  {competitor.note && (
                    <p className="text-xs text-muted-foreground">{competitor.note}</p>
                  )}
                </li>
              ))}
            </ul>
          )}
          {validation.queries.length > 0 && (
            <p className="mt-4 font-mono text-[11px] text-muted-foreground">
              queries: {validation.queries.join(' · ')}
            </p>
          )}
        </div>
      </div>
    </section>
  )
}

function WorkButton({ idea }: { idea: IdeaDetail }) {
  const finalized = idea.status === 'FINALIZED'
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <span>
          <Button asChild={!finalized} disabled={finalized} size="lg">
            {finalized ? (
              <>
                <MessageSquare /> Work on it
              </>
            ) : (
              <Link to={`/ideas/${idea.id}/work`}>
                <MessageSquare /> Work on it · 0.2 credits/message
              </Link>
            )}
          </Button>
        </span>
      </TooltipTrigger>
      <TooltipContent>
        {finalized ? 'Finalized ideas are read-only.' : 'Chat with the agent; each message costs 0.2 credits.'}
      </TooltipContent>
    </Tooltip>
  )
}

function FinalizeButton({ idea }: { idea: IdeaDetail }) {
  const finalized = idea.status === 'FINALIZED'
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <span>
          <Button variant="secondary" size="lg" disabled>
            <Check /> {finalized ? 'Finalized' : 'Finalize · 1 credit'}
          </Button>
        </span>
      </TooltipTrigger>
      <TooltipContent>
        {finalized
          ? 'Already finalized.'
          : 'Finalization (deep validation and build pack) arrives with the finalize phase.'}
      </TooltipContent>
    </Tooltip>
  )
}

function VersionsDialog({ ideaId }: { ideaId: string }) {
  const { data: versions, isPending } = useIdeaVersions(ideaId)
  const restore = useRestoreIdeaVersion(ideaId)
  const [open, setOpen] = useState(false)

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="lg">
          <History /> Version history
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Version history</DialogTitle>
          <DialogDescription>
            Every revision is kept. Restoring creates a new version from an old one.
          </DialogDescription>
        </DialogHeader>
        {isPending ? (
          <Skeleton className="h-24" />
        ) : (
          <ul className="max-h-80 space-y-2 overflow-y-auto">
            {(versions ?? []).map((version) => (
              <li key={version.id} className="flex items-center justify-between gap-3 rounded-lg border p-3">
                <div>
                  <p className="font-mono text-xs">
                    v{version.versionNumber} · {version.source.toLowerCase()}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {version.changeSummary ?? 'No summary'} · {formatDateTime(version.createdAt)}
                  </p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={restore.isPending}
                  onClick={() =>
                    restore.mutate(version.versionNumber, {
                      onSuccess: () => {
                        toast.success(`Restored version ${version.versionNumber}.`)
                        setOpen(false)
                      },
                      onError: (error) =>
                        toast.error(
                          error instanceof Error && 'message' in error
                            ? error.message
                            : 'Could not restore that version.',
                        ),
                    })
                  }
                >
                  Restore
                </Button>
              </li>
            ))}
          </ul>
        )}
      </DialogContent>
    </Dialog>
  )
}

function DeleteButton({ onConfirm }: { onConfirm: () => void }) {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline" size="icon" aria-label="Delete idea">
          <Trash2 className="text-destructive" />
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Delete this idea?</DialogTitle>
          <DialogDescription>
            It disappears from your workspace. This can't be undone from the UI.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">Cancel</Button>
          </DialogClose>
          <DialogClose asChild>
            <Button variant="destructive" onClick={onConfirm}>
              Delete idea
            </Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="space-y-3 rounded-xl border bg-card p-5">
      <h2 className="flex items-center gap-2 font-heading text-lg font-semibold">
        <FileText className="size-4" /> {title}
      </h2>
      <div className="space-y-3 text-sm">{children}</div>
    </section>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      <div className="mt-0.5">{children}</div>
    </div>
  )
}

function ListSection({ title, items, empty }: { title: string; items: string[]; empty?: string }) {
  return (
    <section className="space-y-2 rounded-xl border bg-card p-5">
      <h2 className="font-heading text-lg font-semibold">{title}</h2>
      {items.length === 0 ? (
        <p className="text-sm text-muted-foreground">{empty ?? 'None.'}</p>
      ) : (
        <ul className="list-inside list-disc space-y-1 text-sm">
          {items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      )}
    </section>
  )
}
