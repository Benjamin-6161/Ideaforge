import { Compass } from 'lucide-react'
import { Link } from 'react-router'
import { EmptyState } from '@/components/EmptyState'
import { buttonVariants } from '@/components/ui/button'

export default function NotFoundPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-20">
      <EmptyState
        icon={Compass}
        title="Page not found"
        description="That link doesn't lead anywhere. Head back to your workspace."
        action={
          <Link to="/" className={buttonVariants({ variant: 'outline', size: 'lg' })}>
            Go to dashboard
          </Link>
        }
      />
    </div>
  )
}
