import { Lightbulb } from 'lucide-react'
import { Link } from 'react-router'
import { EmptyState } from '@/components/EmptyState'
import { buttonVariants } from '@/components/ui/button'

export default function IdeasPage() {
  return (
    <div className="space-y-6">
      <header>
        <p className="font-mono text-xs text-muted-foreground">My ideas</p>
        <h1 className="mt-1 text-3xl">Your workspace</h1>
        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Every idea you generate lives here: filter by field and status, favourite the promising ones,
          and open one to work on it.
        </p>
      </header>
      <EmptyState
        icon={Lightbulb}
        title="No ideas yet"
        description="Generate your first set of five. Once the ideas pipeline lands, they will appear here with scores and actions."
        action={
          <Link to="/generate" className={buttonVariants({ size: 'lg' })}>
            Start a generation
          </Link>
        }
      />
    </div>
  )
}
