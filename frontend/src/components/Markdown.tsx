import ReactMarkdown from 'react-markdown'
import rehypeSanitize from 'rehype-sanitize'
import { cn } from '@/lib/utils'

/**
 * The only way assistant markdown is rendered: sanitized, no raw HTML, and
 * external links get safe rel attributes.
 */
export function Markdown({ children, className }: { children: string; className?: string }) {
  return (
    <div className={cn('text-sm leading-relaxed [&>*+*]:mt-3', className)}>
      <ReactMarkdown
        rehypePlugins={[rehypeSanitize]}
        components={{
          a: ({ node: _node, ...props }) => (
            <a {...props} target="_blank" rel="noopener noreferrer nofollow" className="underline underline-offset-2" />
          ),
          ul: ({ node: _node, ...props }) => <ul {...props} className="list-inside list-disc space-y-1" />,
          ol: ({ node: _node, ...props }) => <ol {...props} className="list-inside list-decimal space-y-1" />,
          code: ({ node: _node, ...props }) => (
            <code {...props} className="rounded bg-muted px-1 py-0.5 font-mono text-xs" />
          ),
          pre: ({ node: _node, ...props }) => (
            <pre {...props} className="overflow-x-auto rounded-lg bg-muted p-3 font-mono text-xs" />
          ),
          h1: ({ node: _node, ...props }) => <h3 {...props} className="font-heading text-base font-semibold" />,
          h2: ({ node: _node, ...props }) => <h3 {...props} className="font-heading text-base font-semibold" />,
          h3: ({ node: _node, ...props }) => <h4 {...props} className="font-heading text-sm font-semibold" />,
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  )
}
