import { Link } from 'react-router'
import { useCredits } from '@/auth/use-auth'
import { Button } from '@/components/ui/button'
import { formatCredits } from '@/lib/format'

/** Balance chip in the top bar; the UI only displays what the API returns. */
export function CreditChip() {
  const { data, isPending, isError } = useCredits()
  const label = isPending ? '—' : isError ? 'n/a' : formatCredits(data.balanceUnits)

  return (
    <Button asChild variant="outline" size="sm" className="font-mono">
      <Link to="/credits" aria-label={`Credit balance: ${label}. Open credits page.`}>
        <span className="size-1.5 rounded-full bg-signal" aria-hidden="true" />
        {label}
      </Link>
    </Button>
  )
}
