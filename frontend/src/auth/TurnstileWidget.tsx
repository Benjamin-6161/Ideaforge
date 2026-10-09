import { useEffect, useRef } from 'react'
import { turnstileSiteKey } from '@/lib/env'
import { loadScript } from '@/lib/load-script'

interface TurnstileApi {
  render(
    element: HTMLElement,
    options: {
      sitekey: string
      callback: (token: string) => void
      'expired-callback'?: () => void
      'error-callback'?: () => void
      theme?: 'auto' | 'light' | 'dark'
    },
  ): string
  remove(widgetId: string): void
  reset(widgetId: string): void
}

declare global {
  interface Window {
    turnstile?: TurnstileApi
  }
}

const SCRIPT_URL = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'

/**
 * Cloudflare Turnstile widget. Renders nothing when no site key is configured
 * (dev/test), in which case the backend must have verification disabled.
 */
export function TurnstileWidget({ onToken }: { onToken: (token: string | null) => void }) {
  const siteKey = turnstileSiteKey
  const containerRef = useRef<HTMLDivElement>(null)
  const widgetIdRef = useRef<string | null>(null)
  const onTokenRef = useRef(onToken)

  useEffect(() => {
    onTokenRef.current = onToken
  }, [onToken])

  useEffect(() => {
    if (!siteKey) {
      return
    }
    let cancelled = false
    loadScript(SCRIPT_URL)
      .then(() => {
        if (cancelled || !window.turnstile || !containerRef.current) {
          return
        }
        widgetIdRef.current = window.turnstile.render(containerRef.current, {
          sitekey: siteKey,
          callback: (token) => onTokenRef.current(token),
          'expired-callback': () => onTokenRef.current(null),
          'error-callback': () => onTokenRef.current(null),
        })
      })
      .catch(() => onTokenRef.current(null))

    return () => {
      cancelled = true
      if (widgetIdRef.current && window.turnstile) {
        window.turnstile.remove(widgetIdRef.current)
        widgetIdRef.current = null
      }
    }
  }, [siteKey])

  if (!siteKey) {
    return null
  }
  return <div ref={containerRef} className="min-h-[65px]" data-testid="turnstile" />
}
