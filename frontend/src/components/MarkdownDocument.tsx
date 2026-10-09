import { Check, Copy, Download } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { Markdown } from '@/components/Markdown'
import { Button } from '@/components/ui/button'

/** Renders a markdown document with copy and download-as-.md actions. */
export function MarkdownDocument({
  title,
  markdown,
  filename,
}: {
  title: string
  markdown: string
  filename: string
}) {
  const [copied, setCopied] = useState(false)

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(markdown)
      setCopied(true)
      toast.success(`${title} copied.`)
      window.setTimeout(() => setCopied(false), 2000)
    } catch {
      toast.error('Could not copy to the clipboard.')
    }
  }

  const download = () => {
    const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = filename
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm text-muted-foreground">
          {markdown.length.toLocaleString()} characters · markdown
        </p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={copy}>
            {copied ? <Check /> : <Copy />} Copy
          </Button>
          <Button variant="outline" size="sm" onClick={download}>
            <Download /> Download .md
          </Button>
        </div>
      </div>
      <div className="max-h-[32rem] overflow-y-auto rounded-lg border bg-card p-4">
        <Markdown>{markdown}</Markdown>
      </div>
    </div>
  )
}
