import { cn } from '@/lib/utils'

/** The forge mark: a rising signal line inside an ink tile. */
export function Logo({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        'inline-flex size-7 items-center justify-center rounded-md bg-secondary text-secondary-foreground',
        className,
      )}
      aria-hidden="true"
    >
      <svg viewBox="0 0 24 24" className="size-4" fill="none" stroke="currentColor" strokeWidth="2.2">
        <path d="M3.5 18.5 9 11.5l4.2 3.1L20.5 5" strokeLinecap="round" strokeLinejoin="round" className="text-signal" />
        <circle cx="20.5" cy="5" r="1.6" className="fill-signal stroke-none" />
      </svg>
    </span>
  )
}
