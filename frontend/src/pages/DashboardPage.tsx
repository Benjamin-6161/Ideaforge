import { ArrowRight, Coins, Lightbulb, Sparkles } from 'lucide-react'
import { Link } from 'react-router'
import { useCredits } from '@/auth/use-auth'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useCurrentUser } from '@/auth/use-auth'
import { formatCredits, formatDateTime, formatSignedUnits } from '@/lib/format'
import { useLedger } from '@/auth/use-auth'

export default function DashboardPage() {
  const { data: user } = useCurrentUser()
  const { data: credits, isPending: creditsPending } = useCredits()
  const { data: ledger, isPending: ledgerPending } = useLedger(0, 6)

  const firstName = user?.displayName?.split(' ')[0]

  return (
    <div className="space-y-8">
      <header>
        <p className="font-mono text-xs text-muted-foreground">Dashboard</p>
        <h1 className="mt-1 text-3xl">
          {firstName ? `Welcome back, ${firstName}.` : 'Welcome back.'}
        </h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Start with a run: pick a field and get five researched ideas. Refine one in chat, then finalize
          it into a build-ready plan.
        </p>
      </header>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardDescription className="flex items-center gap-2">
              <Coins className="size-4" /> Credit balance
            </CardDescription>
            <CardTitle className="font-mono text-3xl">
              {creditsPending || !credits ? <Skeleton className="h-8 w-24" /> : formatCredits(credits.balanceUnits)}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Link to="/credits" className="inline-flex items-center gap-1 text-sm underline-offset-2 hover:underline">
              View costs and ledger <ArrowRight className="size-3.5" />
            </Link>
          </CardContent>
        </Card>

        <Card className="md:col-span-2">
          <CardHeader>
            <CardDescription className="flex items-center gap-2">
              <Sparkles className="size-4" /> Next step
            </CardDescription>
            <CardTitle className="text-xl">Generate five ideas from a field</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-wrap items-center gap-3">
            <p className="text-sm text-muted-foreground">
              A run takes a couple of minutes: ideation, market research, then scoring.
            </p>
            <Link
              to="/generate"
              className="inline-flex h-9 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-primary-foreground hover:bg-primary/80"
            >
              Start a generation
            </Link>
          </CardContent>
        </Card>
      </div>

      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-xl">Recent credit activity</h2>
          <Link to="/credits" className="text-sm text-muted-foreground underline-offset-2 hover:text-foreground hover:underline">
            All activity
          </Link>
        </div>
        <Card>
          <CardContent className="p-0">
            {ledgerPending ? (
              <div className="space-y-2 p-4">
                <Skeleton className="h-5 w-full" />
                <Skeleton className="h-5 w-full" />
                <Skeleton className="h-5 w-2/3" />
              </div>
            ) : !ledger || ledger.items.length === 0 ? (
              <EmptyState />
            ) : (
              <ul className="divide-y">
                {ledger.items.map((entry) => (
                  <li key={entry.id} className="flex items-center justify-between px-4 py-3 text-sm">
                    <div>
                      <p className="font-medium">{describeEntry(entry.entryType, entry.action)}</p>
                      <p className="text-xs text-muted-foreground">{formatDateTime(entry.createdAt)}</p>
                    </div>
                    <span
                      className={`font-mono text-sm ${
                        entry.amountUnits > 0 ? 'text-success' : 'text-foreground'
                      }`}
                    >
                      {formatSignedUnits(entry.amountUnits)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </section>
    </div>
  )
}

function describeEntry(entryType: string, action?: string | null): string {
  const actions: Record<string, string> = {
    GENERATE_IDEAS: 'Generated ideas',
    WORK_MESSAGE: 'Chat message',
    FINALIZE: 'Finalized an idea',
    REVALIDATE: 'Re-validated an idea',
  }
  switch (entryType) {
    case 'GRANT_SIGNUP':
      return 'Welcome credits'
    case 'GRANT_ADMIN':
      return 'Credits grant'
    case 'SPEND':
      return action ? (actions[action] ?? 'Spend') : 'Spend'
    case 'REFUND':
      return 'Refund'
    case 'PURCHASE':
      return 'Credit purchase'
    default:
      return 'Adjustment'
  }
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center gap-2 px-4 py-10 text-center">
      <Lightbulb className="size-5 text-muted-foreground" />
      <p className="text-sm font-medium">No credit activity yet</p>
      <p className="max-w-sm text-xs text-muted-foreground">
        Your signup grant and every spend will appear here.
      </p>
    </div>
  )
}
