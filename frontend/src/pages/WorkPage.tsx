import { MessagesSquare } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { useIdea } from '@/auth/use-auth'
import { EmptyState } from '@/components/EmptyState'
import { buttonVariants } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'

/**
 * Placeholder for the chat workspace. The full split view lands with the
 * "work on it" phase; the idea remains open from here.
 */
export default function WorkPage() {
  const { id = '' } = useParams()
  const { data: idea, isPending } = useIdea(id)

  if (isPending) {
    return <Skeleton className="h-64 w-full" />
  }

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      {idea && (
        <p className="font-mono text-xs text-muted-foreground">
          Working on: <span className="text-foreground">{idea.title}</span>
        </p>
      )}
      <EmptyState
        icon={MessagesSquare}
        title="The chat workspace is the next milestone"
        description="You will be able to ask questions, run research tools, and revise the idea here. Every revision is versioned."
        action={
          <Link to={`/ideas/${id}`} className={buttonVariants({ variant: 'outline' })}>
            Back to the idea
          </Link>
        }
      />
    </div>
  )
}
