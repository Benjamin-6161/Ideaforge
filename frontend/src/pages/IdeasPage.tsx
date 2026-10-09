import { Archive, ArchiveRestore, ArrowLeft, ArrowRight, Lightbulb, MoreHorizontal, Search, Star, Trash2 } from 'lucide-react'
import { useDeferredValue, useState } from 'react'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { useDeleteIdea, useFields, useIdeas, usePatchIdea } from '@/auth/use-auth'
import type { IdeaListItem, IdeaListParams } from '@/api/endpoints'
import { EmptyState } from '@/components/EmptyState'
import { ScoreChips } from '@/components/ideas/ScoreChips'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import { cn } from '@/lib/utils'

const statusLabels: Record<string, string> = {
  GENERATED: 'Generated',
  IN_PROGRESS: 'In progress',
  FINALIZED: 'Finalized',
}

export default function IdeasPage() {
  const { data: fields } = useFields()
  const [filters, setFilters] = useState<Omit<IdeaListParams, 'q'>>({
    sort: 'newest',
    page: 0,
    size: 12,
    archived: false,
  })
  const [searchInput, setSearchInput] = useState('')
  const deferredSearch = useDeferredValue(searchInput)

  const params: IdeaListParams = { ...filters, q: deferredSearch.trim() || undefined }
  const { data, isPending, isError, isPlaceholderData } = useIdeas(params)

  const patchIdea = usePatchIdea()
  const deleteIdea = useDeleteIdea()

  const updateFilters = (next: Partial<IdeaListParams>) =>
    setFilters((current) => ({ ...current, ...next, page: next.page ?? 0 }))

  const hasFilters =
    Boolean(filters.fieldId) ||
    Boolean(filters.status) ||
    filters.favorite === true ||
    filters.archived === true ||
    deferredSearch.trim().length > 0

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="font-mono text-xs text-muted-foreground">My ideas</p>
          <h1 className="mt-1 text-3xl">Your workspace</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Everything you generate lives here. Favorite the promising ones, open one to see the full
            breakdown, or archive what you are done with.
          </p>
        </div>
        <Link to="/generate" className={buttonVariants({ size: 'lg' })}>
          New generation
        </Link>
      </header>

      <div className="flex flex-wrap items-end gap-3 rounded-xl border bg-card p-4">
        <div className="min-w-48 flex-1 space-y-1.5">
          <Label htmlFor="idea-search" className="text-xs text-muted-foreground">
            Search
          </Label>
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
            <Input
              id="idea-search"
              className="pl-8"
              placeholder="Search titles…"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <Label className="text-xs text-muted-foreground">Field</Label>
          <select
            aria-label="Filter by field"
            className="h-9 rounded-md border border-input bg-background px-3 text-sm"
            value={filters.fieldId ?? ''}
            onChange={(event) => updateFilters({ fieldId: event.target.value || undefined })}
          >
            <option value="">All fields</option>
            {(fields ?? []).map((field) => (
              <option key={field.id} value={field.id}>
                {field.name}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-1.5">
          <Label className="text-xs text-muted-foreground">Status</Label>
          <select
            aria-label="Filter by status"
            className="h-9 rounded-md border border-input bg-background px-3 text-sm"
            value={filters.status ?? ''}
            onChange={(event) => updateFilters({ status: event.target.value || undefined })}
          >
            <option value="">Any status</option>
            <option value="GENERATED">Generated</option>
            <option value="IN_PROGRESS">In progress</option>
            <option value="FINALIZED">Finalized</option>
          </select>
        </div>

        <div className="space-y-1.5">
          <Label className="text-xs text-muted-foreground">Sort</Label>
          <select
            aria-label="Sort ideas"
            className="h-9 rounded-md border border-input bg-background px-3 text-sm"
            value={filters.sort}
            onChange={(event) =>
              updateFilters({ sort: event.target.value as IdeaListParams['sort'] })
            }
          >
            <option value="newest">Newest</option>
            <option value="demand">Demand</option>
            <option value="profitability">Profitability</option>
          </select>
        </div>

        <Button
          type="button"
          variant={filters.favorite ? 'secondary' : 'outline'}
          size="sm"
          aria-pressed={filters.favorite === true}
          onClick={() => updateFilters({ favorite: filters.favorite ? undefined : true })}
        >
          <Star className={cn('size-3.5', filters.favorite && 'fill-current')} />
          Favorites
        </Button>

        <label className="flex items-center gap-2 pb-1 text-sm">
          <Switch
            aria-label="Show archived ideas"
            checked={filters.archived === true}
            onCheckedChange={(checked) => updateFilters({ archived: checked })}
          />
          Archived
        </label>
      </div>

      {isError && (
        <Alert variant="destructive">
          <AlertDescription>Couldn't load your ideas. Refresh to try again.</AlertDescription>
        </Alert>
      )}

      {isPending ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {[0, 1, 2, 3].map((index) => (
            <Skeleton key={index} className="h-44" />
          ))}
        </div>
      ) : data && data.items.length === 0 ? (
        hasFilters ? (
          <EmptyState
            icon={Search}
            title="No ideas match these filters"
            description="Try a different field, status, or search term."
            action={
              <Button
                variant="outline"
                onClick={() => {
                  setFilters({ sort: 'newest', page: 0, size: 12, archived: false })
                  setSearchInput('')
                }}
              >
                Clear filters
              </Button>
            }
          />
        ) : (
          <EmptyState
            icon={Lightbulb}
            title="No ideas yet"
            description="Generate your first set of five researched ideas, then open one to see the full breakdown."
            action={
              <Link to="/generate" className={buttonVariants({ size: 'lg' })}>
                Start a generation
              </Link>
            }
          />
        )
      ) : (
        <>
          <div className={cn('grid gap-4 md:grid-cols-2 xl:grid-cols-3', isPlaceholderData && 'opacity-70')}>
            {(data?.items ?? []).map((idea) => (
              <IdeaListCard
                key={idea.id}
                idea={idea}
                onToggleFavorite={() =>
                  patchIdea.mutate({ id: idea.id, body: { isFavorite: !idea.favorite } })
                }
                onToggleArchive={() =>
                  patchIdea.mutate(
                    { id: idea.id, body: { isArchived: !idea.archived } },
                    {
                      onSuccess: () =>
                        toast.success(idea.archived ? 'Idea restored.' : 'Idea archived.'),
                    },
                  )
                }
                onDelete={() =>
                  deleteIdea.mutate(idea.id, {
                    onSuccess: () => toast.success('Idea deleted.'),
                    onError: () => toast.error('Could not delete the idea.'),
                  })
                }
              />
            ))}
          </div>

          <div className="flex items-center justify-between text-sm">
            <Button
              variant="outline"
              size="sm"
              disabled={(filters.page ?? 0) === 0}
              onClick={() => setFilters((current) => ({ ...current, page: (current.page ?? 0) - 1 }))}
            >
              <ArrowLeft /> Newer
            </Button>
            <span className="font-mono text-xs text-muted-foreground">
              page {(data?.page ?? 1) + 1} of {Math.max(1, data?.totalPages ?? 1)} · {data?.totalItems ?? 0}{' '}
              ideas
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={!data || data.page + 1 >= data.totalPages}
              onClick={() => setFilters((current) => ({ ...current, page: (current.page ?? 0) + 1 }))}
            >
              Older <ArrowRight />
            </Button>
          </div>
        </>
      )}
    </div>
  )
}

function IdeaListCard({
  idea,
  onToggleFavorite,
  onToggleArchive,
  onDelete,
}: {
  idea: IdeaListItem
  onToggleFavorite: () => void
  onToggleArchive: () => void
  onDelete: () => void
}) {
  return (
    <Card className={cn('flex flex-col', idea.archived && 'opacity-70')}>
      <CardContent className="flex flex-1 flex-col gap-3 pt-5">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-1.5">
              <Badge variant="secondary" className="font-mono text-[10px]">
                {statusLabels[idea.status] ?? idea.status}
              </Badge>
              <Badge variant="outline" className="font-mono text-[10px]">
                v{idea.versionNumber}
              </Badge>
            </div>
            <Link to={`/ideas/${idea.id}`} className="mt-2 block">
              <h2 className="font-heading text-lg leading-snug hover:underline">{idea.title}</h2>
            </Link>
            <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{idea.tagline}</p>
          </div>
          <Button
            variant="ghost"
            size="icon-sm"
            aria-label={idea.favorite ? 'Remove from favorites' : 'Add to favorites'}
            aria-pressed={idea.favorite}
            onClick={onToggleFavorite}
          >
            <Star className={cn('size-4', idea.favorite && 'fill-signal text-signal')} />
          </Button>
        </div>

        {idea.scores ? <ScoreChips scores={idea.scores} compact /> : null}

        <div className="mt-auto flex items-center justify-between text-xs text-muted-foreground">
          <span className="truncate">{idea.fieldName}</span>
          <div className="flex items-center gap-1">
            <Link
              to={`/ideas/${idea.id}`}
              className="text-foreground underline-offset-2 hover:underline"
            >
              Open
            </Link>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon-sm" aria-label="Idea actions">
                  <MoreHorizontal className="size-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onSelect={onToggleArchive}>
                  {idea.archived ? <ArchiveRestore /> : <Archive />}
                  {idea.archived ? 'Unarchive' : 'Archive'}
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DeleteIdeaDialog onConfirm={onDelete} />
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

function DeleteIdeaDialog({ onConfirm }: { onConfirm: () => void }) {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <DropdownMenuItem
          className="text-destructive focus:text-destructive"
          onSelect={(event) => event.preventDefault()}
        >
          <Trash2 />
          Delete…
        </DropdownMenuItem>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Delete this idea?</DialogTitle>
          <DialogDescription>
            The idea disappears from your workspace. This can't be undone from the UI.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">Cancel</Button>
          </DialogClose>
          <DialogClose asChild>
            <Button variant="destructive" onClick={onConfirm}>
              Delete idea
            </Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
