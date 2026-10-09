import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { MarkdownDocument } from '@/components/MarkdownDocument'
import type { Finalization } from '@/api/endpoints'

/** The finalized build pack: tabbed, copyable, downloadable. */
export function FinalizedView({ finalization }: { finalization: Finalization }) {
  const plan = finalization.output
  if (finalization.status !== 'SUCCEEDED' || !plan) {
    return null
  }

  return (
    <section aria-label="Finalization package" className="space-y-4">
      <Tabs defaultValue="stack">
        <TabsList className="flex h-auto flex-wrap justify-start">
          <TabsTrigger value="stack">Tech stack</TabsTrigger>
          <TabsTrigger value="roadmap">Roadmap</TabsTrigger>
          <TabsTrigger value="build-prompt">Build prompt</TabsTrigger>
          <TabsTrigger value="agents">AGENTS.md</TabsTrigger>
          <TabsTrigger value="checklist">Launch checklist</TabsTrigger>
          <TabsTrigger value="risks">Risks</TabsTrigger>
        </TabsList>

        <TabsContent value="stack" className="mt-4 space-y-4">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {[
              ['Frontend', plan.techStack.frontend],
              ['Backend', plan.techStack.backend],
              ['Database', plan.techStack.database],
              ['Auth', plan.techStack.auth],
              ['Hosting', plan.techStack.hosting],
              ['AI layer', plan.techStack.aiLayer],
            ].map(([label, value]) => (
              <div key={label} className="rounded-lg border bg-card p-3">
                <p className="text-xs font-medium text-muted-foreground">{label}</p>
                <p className="mt-0.5 text-sm">{value}</p>
              </div>
            ))}
          </div>
          <div className="rounded-lg border bg-card p-4">
            <p className="text-xs font-medium text-muted-foreground">Estimated MVP cost</p>
            <p className="mt-1 font-mono text-sm">{plan.techStack.estMonthlyCostAtMvp}</p>
            <p className="mt-2 text-sm text-muted-foreground">{plan.techStack.rationale}</p>
            {plan.techStack.thirdParty.length > 0 && (
              <div className="mt-3 flex flex-wrap gap-1.5">
                {plan.techStack.thirdParty.map((item) => (
                  <Badge key={item} variant="secondary" className="text-[10px]">
                    {item}
                  </Badge>
                ))}
              </div>
            )}
          </div>
        </TabsContent>

        <TabsContent value="roadmap" className="mt-4">
          <ol className="space-y-3">
            {plan.mvpRoadmap.map((phase, index) => (
              <li key={phase.phase} className="rounded-lg border bg-card p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="font-heading text-base font-semibold">
                    {index + 1}. {phase.phase}
                  </p>
                  <Badge variant="outline" className="font-mono text-[10px]">
                    {phase.estWeeks} weeks
                  </Badge>
                </div>
                <ul className="mt-2 list-inside list-disc space-y-1 text-sm text-muted-foreground">
                  {phase.goals.map((goal) => (
                    <li key={goal}>{goal}</li>
                  ))}
                </ul>
              </li>
            ))}
          </ol>
        </TabsContent>

        <TabsContent value="build-prompt" className="mt-4">
          <MarkdownDocument
            title="Build prompt"
            markdown={finalization.buildPromptMd ?? ''}
            filename="build-prompt.md"
          />
        </TabsContent>

        <TabsContent value="agents" className="mt-4">
          <MarkdownDocument
            title="AGENTS.md"
            markdown={finalization.agentsMd ?? ''}
            filename="AGENTS.md"
          />
        </TabsContent>

        <TabsContent value="checklist" className="mt-4 space-y-4">
          <div className="rounded-lg border bg-card p-4">
            <p className="text-xs font-medium text-muted-foreground">Pricing suggestion</p>
            <p className="mt-1 text-sm">{plan.pricingSuggestion}</p>
          </div>
          <div className="rounded-lg border bg-card p-4">
            <p className="font-heading text-base font-semibold">Launch checklist</p>
            <ul className="mt-2 space-y-2">
              {plan.launchChecklist.map((item) => (
                <li key={item} className="flex items-start gap-2 text-sm">
                  <input type="checkbox" className="mt-0.5 size-4 accent-[var(--signal)]" aria-label={item} />
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          </div>
          <div className="rounded-lg border bg-card p-4">
            <p className="font-heading text-base font-semibold">Branding</p>
            <div className="mt-2 flex flex-wrap gap-1.5">
              {plan.branding.nameIdeas.map((name) => (
                <Badge key={name} variant="secondary">
                  {name}
                </Badge>
              ))}
            </div>
            <p className="mt-2 text-sm text-muted-foreground">{plan.branding.domainTips}</p>
          </div>
        </TabsContent>

        <TabsContent value="risks" className="mt-4 space-y-4">
          <div className="rounded-lg border bg-card p-4">
            <p className="font-heading text-base font-semibold">Risks</p>
            <ul className="mt-2 list-inside list-disc space-y-1 text-sm">
              {plan.risks.map((risk) => (
                <li key={risk}>{risk}</li>
              ))}
            </ul>
          </div>
          <div className="rounded-lg border bg-card p-4">
            <p className="font-heading text-base font-semibold">Next validation steps</p>
            <ul className="mt-2 list-inside list-disc space-y-1 text-sm text-muted-foreground">
              {plan.validationNextSteps.map((step) => (
                <li key={step}>{step}</li>
              ))}
            </ul>
          </div>
        </TabsContent>
      </Tabs>
    </section>
  )
}
