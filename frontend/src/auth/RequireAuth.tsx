import { Navigate, useLocation } from 'react-router'
import type { ReactNode } from 'react'
import { useCurrentUser } from './use-auth'
import { Skeleton } from '@/components/ui/skeleton'

function FullPageSkeleton() {
  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-4 px-4 py-10" aria-busy="true" aria-label="Loading">
      <Skeleton className="h-6 w-40" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Skeleton className="h-36" />
        <Skeleton className="h-36" />
        <Skeleton className="h-36" />
      </div>
    </div>
  )
}

export function RequireAuth({ children }: { children: ReactNode }) {
  const { isPending, isError } = useCurrentUser()
  const location = useLocation()

  if (isPending) {
    return <FullPageSkeleton />
  }
  if (isError) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }
  return children
}
