import { Sparkles } from 'lucide-react'
import { Link } from 'react-router'
import { EmptyState } from '@/components/EmptyState'
import { buttonVariants } from '@/components/ui/button'

export default function GeneratePage() {
  return (
    <div className="space-y-6">
      <header>
        <p className="font-mono text-xs text-muted-foreground">Generate</p>
        <h1 className="mt-1 text-3xl">New generation</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Pick a field and optional parameters. Each run returns five ideas with evidence-backed scores.
        </p>
      </header>
      <EmptyState
        icon={Sparkles}
        title="The generation form arrives with the pipeline"
        description="The API, credit metering, and research layer are in place. This page will host the field picker, parameters, and progress view."
        action={
          <Link to="/credits" className={buttonVariants({ variant: 'outline', size: 'lg' })}>
            Check generation costs
          </Link>
        }
      />
    </div>
  )
}
