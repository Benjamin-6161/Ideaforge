import { useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import {
  AlertTriangle,
  Archive,
  ArchiveRestore,
  ArrowLeft,
  Check,
  FileText,
  Link2,
  Loader2,
  Lock,
  MessageSquare,
  RefreshCw,
  Star,
  Trash2,
  Trophy,
} from 'lucide-react'

import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import {
  useDeleteIdea,
  useFinalization,
  useFinalizeIdea,
  useIdea,
  usePatchIdea,
  useRevalidateIdea,
} from '@/auth/use-auth'
import { ApiError } from '@/api/client'
import type { IdeaDetail, ValidationDetail } from '@/api/endpoints'
import { FinalizedView } from '@/components/ideas/FinalizedView'
import { ScoreMeter } from '@/components/ideas/ScoreMeter'
import { VersionsDialog } from '@/components/ideas/VersionsDialog'
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
import { failureMessageFor } from '@/lib/llm-errors'
import { formatDateTime } from '@/lib/format'
import { cn } from '@/lib/utils'

export default function IdeaDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { data: idea, isPending, isError } = useIdea(id)
  const patchIdea = usePatchIdea()
  const deleteIdea = useDeleteIdea()
  const finalize = useFinalizeIdea(id)
  const { data: finalization } = useFinalization(
    id,
    idea?.status === 'FINALIZED' || finalize.isSuccess,
  )
  const [finalizeOpen, setFinalizeOpen] = useState(false)
  const queryClient = useQueryClient()

  useEffect(() => {
    if (finalization?.status === 'SUCCEEDED') {
      void queryClient.invalidateQueries({ queryKey: ['idea', id] })
    }
  }, [finalization?.status, id, queryClient])

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
          <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
            <span>
              The idea content changed since this check. Re-validation runs a deep check against the
              current version for 0.5 credits.
            </span>
            <RevalidateButton idea={idea} />
          </AlertDescription>
        </Alert>
      )}

      {idea.revalidation?.status === 'RUNNING' && (
        <Alert>
          <AlertDescription className="flex items-center gap-2">
            <Loader2 className="size-4 animate-spin" /> Deep validation is running — this can take a
            minute.
          </AlertDescription>
        </Alert>
      )}

      {idea.revalidation?.status === 'FAILED' && (
        <Alert variant="destructive">
          <AlertDescription>
            The last deep validation failed: {failureMessageFor(idea.revalidation.errorCode)} Credits
            were refunded.
          </AlertDescription>
        </Alert>
      )}

      {finalization?.status === 'RUNNING' && (
        <Alert>
          <AlertDescription className="flex items-center gap-2">
            <Loader2 className="size-4 animate-spin" /> Finalizing: deep validation, build prompt, and
            AGENTS.md. This can take a few minutes.
          </AlertDescription>
        </Alert>
      )}

      {finalization?.status === 'FAILED' && (
        <Alert variant="destructive">
          <AlertDescription>
            The finalization failed: {failureMessageFor(finalization.errorCode)} Credits were refunded.
          </AlertDescription>
        </Alert>
      )}

      {finalization?.status === 'SUCCEEDED' && <FinalizedView finalization={finalization} />}

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
        <FinalizeButton
          idea={idea}
          running={finalization?.status === 'RUNNING' || finalize.isPending}
          onRequest={() => setFinalizeOpen(true)}
        />
        <VersionsDialog ideaId={idea.id} disabled={finalized} />
        <FinalizeDialog
          open={finalizeOpen}
          onOpenChange={setFinalizeOpen}
          pending={finalize.isPending}
          onConfirm={() =>
            finalize.mutate(crypto.randomUUID(), {
              onSuccess: () => {
                setFinalizeOpen(false)
                toast.success('Finalization started.')
              },
              onError: (error: unknown) =>
                toast.error(
                  error instanceof ApiError ? error.message : 'Could not start the finalization.',
                ),
            })
          }
        />
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

function FinalizeButton({
  idea,
  running,
  onRequest,
}: {
  idea: IdeaDetail
  running: boolean
  onRequest: () => void
}) {
  const finalized = idea.status === 'FINALIZED'
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <span>
          <Button variant="secondary" size="lg" disabled={finalized || running} onClick={onRequest}>
            {running ? <Loader2 className="animate-spin" /> : <Check />}
            {finalized ? 'Finalized' : running ? 'Finalizing…' : 'Finalize · 1 credit'}
          </Button>
        </span>
      </TooltipTrigger>
      <TooltipContent>
        {finalized
          ? 'Already finalized and read-only.'
          : 'Deep validation plus tech stack, roadmap, build prompt, and AGENTS.md.'}
      </TooltipContent>
    </Tooltip>
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

function RevalidateButton({ idea }: { idea: IdeaDetail }) {
  const revalidate = useRevalidateIdea(idea.id)
  const running = idea.revalidation?.status === 'RUNNING' || revalidate.isPending

  return (
    <Button
      variant="outline"
      disabled={running || idea.status === 'FINALIZED'}
      onClick={() =>
        revalidate.mutate(crypto.randomUUID(), {
          onSuccess: () => toast.success('Deep validation started.'),
          onError: (error) =>
            toast.error(error instanceof ApiError ? error.message : 'Could not start validation.'),
        })
      }
    >
      {running ? <Loader2 className="animate-spin" /> : <RefreshCw />}
      {running ? 'Validating…' : 'Re-validate · 0.5 credits'}
    </Button>
  )
}

function FinalizeDialog({
  open,
  onOpenChange,
  pending,
  onConfirm,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  pending: boolean
  onConfirm: () => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Finalize this idea?</DialogTitle>
          <DialogDescription>
            Finalizing costs <span className="font-mono">1 credit</span> and runs a deep validation
            plus the launch package: tech stack, MVP roadmap, pricing, launch checklist, branding,
            risks, a detailed coding-agent build prompt, and a draft AGENTS.md. The idea becomes
            read-only afterwards — only favorites and archiving stay available.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">Cancel</Button>
          </DialogClose>
          <DialogClose asChild>
            <Button disabled={pending} onClick={onConfirm}>
              {pending ? <Loader2 className="animate-spin" /> : <Check />} Finalize · 1 credit
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
