import { ArrowLeft, CalendarClock, Loader2, SendHorizonal, Sparkles } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { toast } from 'sonner'
import { useIdea, useRecentMessages, useSendIdeaMessage } from '@/auth/use-auth'
import { ApiError } from '@/api/client'
import type { ChatMessage } from '@/api/endpoints'
import { Markdown } from '@/components/Markdown'
import { ScoreChips } from '@/components/ideas/ScoreChips'
import { VersionsDialog } from '@/components/ideas/VersionsDialog'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { formatUnits } from '@/lib/format'
import { cn } from '@/lib/utils'

const SUGGESTIONS = [
  'Narrow the niche',
  'Make it more AI-centric',
  'Explore pricing',
  'Find competitors',
]

export default function WorkPage() {
  const { id = '' } = useParams()
  const { data: idea, isPending: ideaPending } = useIdea(id)
  const { data: messages, isPending: messagesPending } = useRecentMessages(id)
  const send = useSendIdeaMessage(id)
  const [input, setInput] = useState('')
  const [error, setError] = useState<string | null>(null)
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const node = scrollRef.current
    if (node && typeof node.scrollTo === 'function') {
      node.scrollTo({ top: node.scrollHeight, behavior: 'smooth' })
    }
  }, [messages, send.isPending])

  const submit = () => {
    const content = input.trim()
    if (!content || send.isPending) {
      return
    }
    setError(null)
    send.mutate(
      { content, idempotencyKey: crypto.randomUUID() },
      {
        onSuccess: (turn) => {
          setInput('')
          if (turn.newVersion) {
            toast.success(`Version ${turn.newVersion.versionNumber} created`)
          }
        },
        onError: (mutationError) => {
          setError(
            mutationError instanceof ApiError
              ? mutationError.message
              : 'The message could not be sent. Try again.',
          )
        },
      },
    )
  }

  const ideaPanel = ideaPending || !idea ? (
    <div className="space-y-3">
      <Skeleton className="h-6 w-32" />
      <Skeleton className="h-24 w-full" />
    </div>
  ) : (
    <div className="space-y-5">
      <div>
        <Link
          to={`/ideas/${id}`}
          className="inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="size-3.5" /> Full details
        </Link>
        <div className="mt-3 flex flex-wrap items-center gap-1.5">
          <Badge variant="secondary" className="font-mono text-[10px]">
            {idea.status === 'IN_PROGRESS' ? 'In progress' : idea.status === 'FINALIZED' ? 'Finalized' : 'Generated'}
          </Badge>
          <Badge variant="outline" className="font-mono text-[10px]">
            v{idea.versionNumber}
          </Badge>
          <Badge variant="outline" className="font-mono text-[10px]">
            {idea.fieldName}
          </Badge>
        </div>
        <h1 className="mt-3 font-heading text-2xl leading-snug">{idea.title}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{idea.tagline}</p>
      </div>

      {idea.validation && (
        <div className="rounded-lg border bg-card p-4">
          <p className="mb-2 text-xs font-medium text-muted-foreground">
            Scores (version {idea.validation.validatedVersionNumber})
            {idea.validation.stale ? ' · stale, re-validate' : ''}
          </p>
          <ScoreChips scores={idea.validation.scores} compact />
        </div>
      )}

      <VersionsDialog ideaId={id} disabled={idea.status === 'FINALIZED'} />

      {idea.status === 'FINALIZED' && (
        <Alert>
          <AlertDescription>This idea is finalized and read-only.</AlertDescription>
        </Alert>
      )}
    </div>
  )

  const chatPanel = (
    <div className="flex h-[calc(100svh-13rem)] min-h-80 flex-col rounded-xl border bg-card">
      <div className="flex items-center justify-between border-b px-4 py-2.5">
        <p className="flex items-center gap-2 text-sm font-medium">
          <Sparkles className="size-4 text-signal" /> Product agent
        </p>
        <p className="font-mono text-xs text-muted-foreground">
          {formatUnits(2)} credits per message
        </p>
      </div>

      <div ref={scrollRef} className="flex-1 space-y-4 overflow-y-auto p-4">
        {messagesPending ? (
          <div className="space-y-3">
            <Skeleton className="h-16 w-3/4" />
            <Skeleton className="ml-auto h-10 w-1/2" />
          </div>
        ) : (messages ?? []).length === 0 ? (
          <div className="space-y-3">
            <p className="text-sm text-muted-foreground">
              Ask about demand, competitors, pricing, or tell the agent to revise the idea. Try one of
              these:
            </p>
            <div className="flex flex-wrap gap-1.5">
              {SUGGESTIONS.map((suggestion) => (
                <button
                  key={suggestion}
                  type="button"
                  className="rounded-full border px-3 py-1 text-xs text-muted-foreground transition-colors hover:text-foreground"
                  onClick={() => setInput(suggestion)}
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        ) : (
          <>
            {(messages ?? []).map((message) => (
              <MessageBubble key={message.id} message={message} ideaId={id} />
            ))}
            {send.isPending && (
              <div className="flex items-center gap-2 text-sm text-muted-foreground" aria-live="polite">
                <Loader2 className="size-3.5 animate-spin" /> Thinking…
              </div>
            )}
          </>
        )}
      </div>

      <div className="space-y-2 border-t p-3">
        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        <div className="flex items-end gap-2">
          <Textarea
            aria-label="Message the agent"
            placeholder="Ask a question or request a revision…"
            value={input}
            maxLength={2000}
            rows={2}
            disabled={send.isPending || idea?.status === 'FINALIZED'}
            onChange={(event) => setInput(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault()
                submit()
              }
            }}
          />
          <Button
            size="lg"
            onClick={submit}
            disabled={!input.trim() || send.isPending || idea?.status === 'FINALIZED'}
            aria-label="Send message"
          >
            {send.isPending ? <Loader2 className="animate-spin" /> : <SendHorizonal />}
            Send
          </Button>
        </div>
        <p className="text-[11px] text-muted-foreground">
          Enter sends · Shift+Enter adds a line. The agent can search the web (read-only).
        </p>
      </div>
    </div>
  )

  return (
    <div className="space-y-4">
      <p className="flex items-center gap-2 text-xs text-muted-foreground">
        <CalendarClock className="size-3.5" /> Work on it
      </p>

      <div className="hidden gap-6 lg:grid lg:grid-cols-[5fr_7fr]">
        <div>{ideaPanel}</div>
        <div>{chatPanel}</div>
      </div>

      <div className="lg:hidden">
        <Tabs defaultValue="chat">
          <TabsList className="w-full">
            <TabsTrigger value="idea" className="flex-1">
              Idea
            </TabsTrigger>
            <TabsTrigger value="chat" className="flex-1">
              Chat
            </TabsTrigger>
          </TabsList>
          <TabsContent value="idea" className="pt-4">
            {ideaPanel}
          </TabsContent>
          <TabsContent value="chat" className="pt-4">
            {chatPanel}
          </TabsContent>
        </Tabs>
      </div>
    </div>
  )
}

function MessageBubble({ message, ideaId }: { message: ChatMessage; ideaId: string }) {
  const isUser = message.role === 'USER'
  const failed = message.status === 'FAILED'
  return (
    <div className={cn('flex flex-col gap-1', isUser ? 'items-end' : 'items-start')}>
      <div
        className={cn(
          'max-w-[92%] rounded-xl px-3.5 py-2.5',
          isUser
            ? 'bg-secondary text-secondary-foreground'
            : failed
              ? 'border border-destructive/40 bg-destructive/5'
              : 'border bg-background',
        )}
      >
        {isUser ? (
          <p className="whitespace-pre-wrap text-sm">{message.content}</p>
        ) : (
          <Markdown>{message.content}</Markdown>
        )}
      </div>
      <div className="flex items-center gap-2 px-1 text-[11px] text-muted-foreground">
        <span className="font-mono">{isUser ? 'you' : failed ? 'failed · refunded' : 'agent'}</span>
        {message.resultingVersionNumber != null && (
          <Link to={`/ideas/${ideaId}`} className="underline underline-offset-2 hover:text-foreground">
            created version {message.resultingVersionNumber}
          </Link>
        )}
      </div>
    </div>
  )
}
