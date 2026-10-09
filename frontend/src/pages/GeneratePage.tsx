import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { IdeaCard } from '@/components/ideas/IdeaCard'
import { useCreateGeneration, useCredits, useFields, useGeneration, useGenerationOptions } from '@/auth/use-auth'
import type { CreateGenerationBody, GenerationParameters, OptionValue } from '@/api/endpoints'
import { ApiError } from '@/api/client'
import { formatCredits, formatUnits } from '@/lib/format'
import { cn } from '@/lib/utils'
import { useState } from 'react'
import { Sparkles } from 'lucide-react'
import { FieldCombobox } from './generate/FieldCombobox'
import { GenerationProgress } from './generate/GenerationProgress'
import { ParameterSelect } from './generate/ParameterSelect'

export default function GeneratePage() {
  const { data: fields, isPending: fieldsPending } = useFields()
  const { data: options } = useGenerationOptions()
  const { data: credits } = useCredits()
  const createGeneration = useCreateGeneration()

  const [fieldId, setFieldId] = useState<string | null>(null)
  const [nicheSelect, setNicheSelect] = useState('')
  const [customNiche, setCustomNiche] = useState('')
  const [customNicheMode, setCustomNicheMode] = useState(false)
  const [params, setParams] = useState<Partial<GenerationParameters>>({})
  const [runId, setRunId] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const { data: run } = useGeneration(runId)

  const costUnits = credits?.costs.find((item) => item.action === 'GENERATE_IDEAS')?.costUnits ?? 20
  const balanceUnits = credits?.balanceUnits ?? 0
  const affordable = balanceUnits >= costUnits
  const selectedField = fields?.find((field) => field.id === fieldId)
  const niche = customNicheMode ? customNiche.trim() : nicheSelect

  const update = (key: keyof GenerationParameters, value: unknown) =>
    setParams((current) => ({ ...current, [key]: value }))

  const optionValue = (current: string | undefined, list: OptionValue[] | undefined, fallback: string) => {
    if (current) {
      return current
    }
    const preferred = list?.find((option) => option.value === fallback)
    return preferred?.value ?? list?.[0]?.value ?? ''
  }

  const submit = () => {
    if (!fieldId) {
      setFormError('Pick a field first.')
      return
    }
    setFormError(null)
    const body: CreateGenerationBody = {
      fieldId,
      niche: niche || undefined,
      parameters: {
        ...params,
        problemFocus: params.problemFocus?.trim() || undefined,
        exclude: params.exclude?.trim() || undefined,
      },
    }
    createGeneration.mutate(
      { body, idempotencyKey: crypto.randomUUID() },
      {
        onSuccess: (created) => setRunId(created.id),
        onError: (error) =>
          setFormError(
            error instanceof ApiError
              ? error.message
              : 'Could not start the generation. Please try again.',
          ),
      },
    )
  }

  const reset = () => {
    setRunId(null)
    setParams({})
    setNicheSelect('')
    setCustomNiche('')
    setCustomNicheMode(false)
  }

  if (runId) {
    const succeeded = run?.status === 'SUCCEEDED'
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <header className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p className="font-mono text-xs text-muted-foreground">Generate</p>
            <h1 className="mt-1 text-3xl">Your run</h1>
          </div>
          <Button variant="outline" onClick={reset}>
            New generation
          </Button>
        </header>

        {!succeeded && <GenerationProgress run={run} />}

        {succeeded && run && (
          <>
            <GenerationProgress run={run} />
            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {run.ideas.map((idea) => (
                <IdeaCard key={idea.id} idea={idea} />
              ))}
            </section>
            <p className="text-xs text-muted-foreground">
              Scores are AI estimates with cited evidence — open an idea to see the details, or work on
              it in chat.
            </p>
          </>
        )}
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <header>
        <p className="font-mono text-xs text-muted-foreground">Generate</p>
        <h1 className="mt-1 text-3xl">New generation</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Pick an industry, tune the constraints if you want, and get five researched ideas.
        </p>
      </header>

      <div className="space-y-5 rounded-xl border bg-card p-5">
        <div className="space-y-1.5">
          <Label className="text-xs text-muted-foreground">Field</Label>
          {fieldsPending ? (
            <Skeleton className="h-10 w-full" />
          ) : (
            <FieldCombobox fields={fields ?? []} value={fieldId} onChange={setFieldId} />
          )}
        </div>

        {selectedField && selectedField.niches.length > 0 && (
          <div className="space-y-1.5">
            <Label className="text-xs text-muted-foreground">Niche (optional)</Label>
            {!customNicheMode ? (
              <div className="flex gap-2">
                <select
                  aria-label="Niche"
                  className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                  value={nicheSelect}
                  onChange={(event) => {
                    if (event.target.value === '__custom__') {
                      setCustomNicheMode(true)
                      setNicheSelect('')
                    } else {
                      setNicheSelect(event.target.value)
                    }
                  }}
                >
                  <option value="">Any sub-niche</option>
                  {selectedField.niches.map((child) => (
                    <option key={child.id} value={child.name}>
                      {child.name}
                    </option>
                  ))}
                  <option value="__custom__">Custom niche…</option>
                </select>
              </div>
            ) : (
              <div className="flex gap-2">
                <Input
                  aria-label="Custom niche"
                  placeholder="e.g. medical billing for small clinics"
                  maxLength={100}
                  value={customNiche}
                  onChange={(event) => setCustomNiche(event.target.value)}
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setCustomNicheMode(false)
                    setCustomNiche('')
                  }}
                >
                  Cancel
                </Button>
              </div>
            )}
          </div>
        )}

        <Accordion type="single" collapsible>
          <AccordionItem value="more" className="border-b-0">
            <AccordionTrigger className="text-sm">More options</AccordionTrigger>
            <AccordionContent>
              <div className="grid gap-4 pt-1 sm:grid-cols-2">
                <ParameterSelect
                  label="Target customer"
                  value={optionValue(params.targetCustomer, options?.targetCustomer, 'ANY')}
                  options={options?.targetCustomer ?? []}
                  onChange={(value) => update('targetCustomer', value)}
                />
                <ParameterSelect
                  label="Customer size (B2B)"
                  value={optionValue(params.customerSize, options?.customerSize, 'ANY')}
                  options={options?.customerSize ?? []}
                  onChange={(value) => update('customerSize', value)}
                />
                <ParameterSelect
                  label="Builder profile"
                  value={optionValue(params.builderProfile, options?.builderProfile, 'SOLO_FOUNDER')}
                  options={options?.builderProfile ?? []}
                  onChange={(value) => update('builderProfile', value)}
                />
                <ParameterSelect
                  label="Time to MVP"
                  value={optionValue(params.timeToMvp, options?.timeToMvp, 'NO_LIMIT')}
                  options={options?.timeToMvp ?? []}
                  onChange={(value) => update('timeToMvp', value)}
                />
                <ParameterSelect
                  label="Starting budget"
                  value={optionValue(params.startingBudget, options?.startingBudget, 'MODERATE')}
                  options={options?.startingBudget ?? []}
                  onChange={(value) => update('startingBudget', value)}
                />
                <ParameterSelect
                  label="AI depth"
                  value={optionValue(params.aiDepth, options?.aiDepth, 'ANY')}
                  options={options?.aiDepth ?? []}
                  onChange={(value) => update('aiDepth', value)}
                />
                <ParameterSelect
                  label="Monetization"
                  value={optionValue(params.monetization, options?.monetization, 'ANY')}
                  options={options?.monetization ?? []}
                  onChange={(value) => update('monetization', value)}
                />
                <ParameterSelect
                  label="Region"
                  value={optionValue(params.region, options?.region, 'GLOBAL')}
                  options={options?.region ?? []}
                  onChange={(value) => update('region', value)}
                />
                <ParameterSelect
                  label="Novelty preference"
                  value={optionValue(params.noveltyPreference, options?.noveltyPreference, 'BALANCED')}
                  options={options?.noveltyPreference ?? []}
                  onChange={(value) => update('noveltyPreference', value)}
                />
              </div>

              <div className="mt-4 space-y-2">
                <Label className="text-xs text-muted-foreground">AI capabilities (any)</Label>
                <div className="flex flex-wrap gap-1.5">
                  {(options?.aiCapabilities ?? []).map((capability) => {
                    const selected = params.aiCapabilities?.includes(
                      capability.value as NonNullable<GenerationParameters['aiCapabilities']>[number],
                    )
                    return (
                      <button
                        key={capability.value}
                        type="button"
                        aria-pressed={selected}
                        className={cn(
                          'rounded-full border px-3 py-1 text-xs transition-colors',
                          selected
                            ? 'border-signal/60 bg-signal/15 text-signal-ink'
                            : 'border-border text-muted-foreground hover:text-foreground',
                        )}
                        onClick={() => {
                          const current = params.aiCapabilities ?? []
                          const next = selected
                            ? current.filter((value) => value !== capability.value)
                            : [...current, capability.value as (typeof current)[number]]
                          update('aiCapabilities', next)
                        }}
                      >
                        {capability.label}
                      </button>
                    )
                  })}
                </div>
              </div>

              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <div className="space-y-1.5">
                  <Label htmlFor="problem-focus" className="text-xs text-muted-foreground">
                    Problem focus (optional)
                  </Label>
                  <Input
                    id="problem-focus"
                    maxLength={200}
                    placeholder="e.g. reducing no-shows"
                    value={params.problemFocus ?? ''}
                    onChange={(event) => update('problemFocus', event.target.value)}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="exclude" className="text-xs text-muted-foreground">
                    Exclude (optional)
                  </Label>
                  <Input
                    id="exclude"
                    maxLength={200}
                    placeholder="e.g. anything needing hardware"
                    value={params.exclude ?? ''}
                    onChange={(event) => update('exclude', event.target.value)}
                  />
                </div>
              </div>
            </AccordionContent>
          </AccordionItem>
        </Accordion>
      </div>

      {formError && (
        <Alert variant="destructive">
          <AlertDescription>{formError}</AlertDescription>
        </Alert>
      )}

      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border bg-card p-4">
        <div className="text-sm">
          <p>
            Costs <span className="font-mono">{formatUnits(costUnits)}</span> credits — you have{' '}
            <span className="font-mono">{formatCredits(balanceUnits)}</span>
          </p>
          {!affordable && (
            <p className="text-xs text-destructive">
              Not enough credits. Add more from the credits page once payments are enabled.
            </p>
          )}
        </div>
        <Button
          size="lg"
          onClick={submit}
          disabled={!fieldId || !affordable || createGeneration.isPending}
        >
          <Sparkles />
          {createGeneration.isPending ? 'Starting…' : 'Generate ideas'}
        </Button>
      </div>
    </div>
  )
}
