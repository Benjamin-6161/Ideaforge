import { ArrowLeft, ArrowRight, Coins } from 'lucide-react'
import { useState } from 'react'
import { useCredits, useLedger, usePackages } from '@/auth/use-auth'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { formatCredits, formatDateTime, formatMoney, formatSignedUnits, formatUnits } from '@/lib/format'

const costLabels: Record<string, { label: string; note: string }> = {
  GENERATE_IDEAS: { label: 'Generate ideas', note: 'Five researched ideas per run' },
  WORK_MESSAGE: { label: 'Chat message', note: 'Per message while working on an idea' },
  FINALIZE: { label: 'Finalize', note: 'Deep validation and the full build kickoff pack' },
  REVALIDATE: { label: 'Re-validate', note: 'Deep check against the current version' },
}

const entryLabels: Record<string, string> = {
  GRANT_SIGNUP: 'Welcome credits',
  GRANT_ADMIN: 'Credits grant',
  PURCHASE: 'Credit purchase',
  SPEND: 'Spend',
  REFUND: 'Refund',
  ADJUSTMENT: 'Adjustment',
}

export default function CreditsPage() {
  const [page, setPage] = useState(0)
  const { data: credits, isPending: creditsPending, isError: creditsError } = useCredits()
  const { data: packages, isPending: packagesPending } = usePackages()
  const { data: ledger, isPending: ledgerPending } = useLedger(page)

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="font-mono text-xs text-muted-foreground">Credits</p>
          <h1 className="mt-1 text-3xl">Balance and usage</h1>
        </div>
        <div className="rounded-lg border bg-card px-5 py-3 text-right">
          <p className="text-xs text-muted-foreground">Available</p>
          {creditsPending ? (
            <Skeleton className="mt-1 h-8 w-28" />
          ) : (
            <p className="font-mono text-3xl font-medium">
              {credits ? formatCredits(credits.balanceUnits) : '—'}
            </p>
          )}
          <p className="text-[11px] text-muted-foreground">1 credit = 10 units</p>
        </div>
      </header>

      {creditsError && (
        <Alert variant="destructive">
          <AlertDescription>Couldn't load your balance. Refresh to try again.</AlertDescription>
        </Alert>
      )}

      <section className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardDescription className="flex items-center gap-2">
              <Coins className="size-4" /> Cost table
            </CardDescription>
            <CardTitle className="text-xl">What actions cost</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <ul className="divide-y">
              {(credits?.costs ?? []).map((cost) => (
                <li key={cost.action} className="flex items-center justify-between px-5 py-3">
                  <div>
                    <p className="text-sm font-medium">{costLabels[cost.action]?.label ?? cost.action}</p>
                    <p className="text-xs text-muted-foreground">
                      {costLabels[cost.action]?.note ?? 'Metered action'}
                    </p>
                  </div>
                  <span className="font-mono text-sm">{formatUnits(cost.costUnits)} cr</span>
                </li>
              ))}
              {creditsPending && (
                <li className="px-5 py-3">
                  <Skeleton className="h-5 w-full" />
                </li>
              )}
            </ul>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Packages</CardDescription>
            <CardTitle className="text-xl">Buy more credits</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Alert>
              <AlertDescription>Payments aren't enabled yet. Packages are shown for reference.</AlertDescription>
            </Alert>
            <div className="grid gap-2 sm:grid-cols-2">
              {packagesPending &&
                [0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-20" />)}
              {(packages ?? []).map((creditPackage) => (
                <div key={creditPackage.id} className="rounded-lg border p-3">
                  <div className="flex items-baseline justify-between">
                    <p className="text-sm font-medium">{creditPackage.name}</p>
                    <p className="font-mono text-sm">{formatMoney(creditPackage.priceCents, creditPackage.currency)}</p>
                  </div>
                  <p className="mt-0.5 text-xs text-muted-foreground">
                    {formatUnits(creditPackage.creditsUnits)} credits
                  </p>
                  <Button variant="outline" size="sm" className="mt-2 w-full" disabled>
                    Buy credits (coming soon)
                  </Button>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </section>

      <section>
        <h2 className="mb-3 text-xl">Ledger</h2>
        <Card>
          <CardContent className="p-0">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-xs text-muted-foreground">
                  <th scope="col" className="px-5 py-2 font-medium">When</th>
                  <th scope="col" className="px-5 py-2 font-medium">Entry</th>
                  <th scope="col" className="px-5 py-2 text-right font-medium">Change</th>
                  <th scope="col" className="hidden px-5 py-2 text-right font-medium sm:table-cell">Balance</th>
                </tr>
              </thead>
              <tbody>
                {ledgerPending && (
                  <tr>
                    <td colSpan={4} className="px-5 py-4">
                      <Skeleton className="h-5 w-full" />
                    </td>
                  </tr>
                )}
                {!ledgerPending && ledger?.items.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-5 py-10 text-center text-muted-foreground">
                      No ledger entries yet. They appear after your first grant or spend.
                    </td>
                  </tr>
                )}
                {(ledger?.items ?? []).map((entry) => (
                  <tr key={entry.id} className="border-b last:border-0">
                    <td className="px-5 py-3 text-muted-foreground">{formatDateTime(entry.createdAt)}</td>
                    <td className="px-5 py-3">
                      {entryLabels[entry.entryType] ?? entry.entryType}
                      {entry.action && entry.entryType === 'SPEND' && (
                        <span className="text-muted-foreground"> — {costLabels[entry.action]?.label ?? entry.action}</span>
                      )}
                    </td>
                    <td
                      className={`px-5 py-3 text-right font-mono ${
                        entry.amountUnits > 0 ? 'text-success' : ''
                      }`}
                    >
                      {formatSignedUnits(entry.amountUnits)}
                    </td>
                    <td className="hidden px-5 py-3 text-right font-mono text-muted-foreground sm:table-cell">
                      {formatUnits(entry.balanceAfterUnits)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
        <div className="mt-3 flex items-center justify-between text-sm">
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((value) => Math.max(0, value - 1))}
          >
            <ArrowLeft /> Newer
          </Button>
          <span className="font-mono text-xs text-muted-foreground">
            page {(ledger?.page ?? page) + 1} of {Math.max(1, ledger?.totalPages ?? 1)}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={!ledger || page + 1 >= (ledger.totalPages ?? 1)}
            onClick={() => setPage((value) => value + 1)}
          >
            Older <ArrowRight />
          </Button>
        </div>
      </section>
    </div>
  )
}
