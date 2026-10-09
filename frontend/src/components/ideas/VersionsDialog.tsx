import { History } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { useIdeaVersions, useRestoreIdeaVersion } from '@/auth/use-auth'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDateTime } from '@/lib/format'

/** Version history with restore; used on the idea detail and work pages. */
export function VersionsDialog({ ideaId, disabled }: { ideaId: string; disabled?: boolean }) {
  const { data: versions, isPending } = useIdeaVersions(ideaId)
  const restore = useRestoreIdeaVersion(ideaId)
  const [open, setOpen] = useState(false)

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="lg" disabled={disabled}>
          <History /> Version history
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Version history</DialogTitle>
          <DialogDescription>
            Every revision is kept. Restoring creates a new version from an old one.
          </DialogDescription>
        </DialogHeader>
        {isPending ? (
          <Skeleton className="h-24" />
        ) : (
          <ul className="max-h-80 space-y-2 overflow-y-auto">
            {(versions ?? []).map((version) => (
              <li key={version.id} className="flex items-center justify-between gap-3 rounded-lg border p-3">
                <div>
                  <p className="font-mono text-xs">
                    v{version.versionNumber} · {version.source.toLowerCase()}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {version.changeSummary ?? 'No summary'} · {formatDateTime(version.createdAt)}
                  </p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={restore.isPending}
                  onClick={() =>
                    restore.mutate(version.versionNumber, {
                      onSuccess: () => {
                        toast.success(`Restored version ${version.versionNumber}.`)
                        setOpen(false)
                      },
                      onError: (error) =>
                        toast.error(
                          error instanceof ApiError ? error.message : 'Could not restore that version.',
                        ),
                    })
                  }
                >
                  Restore
                </Button>
              </li>
            ))}
          </ul>
        )}
      </DialogContent>
    </Dialog>
  )
}
