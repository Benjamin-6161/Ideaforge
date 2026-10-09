import { useEffect, useRef } from 'react'
import { googleClientId } from '@/lib/env'
import { loadScript } from '@/lib/load-script'

interface GoogleIdentityApi {
  accounts: {
    id: {
      initialize(options: { client_id: string; callback: (response: { credential: string }) => void }): void
      renderButton(
        element: HTMLElement,
        options: { theme?: string; size?: string; width?: number; text?: string; shape?: string },
      ): void
    }
  }
}

declare global {
  interface Window {
    google?: GoogleIdentityApi
  }
}

const SCRIPT_URL = 'https://accounts.google.com/gsi/client'

/** Google Identity Services button; hidden when no client id is configured. */
export function GoogleSignInButton({ onCredential }: { onCredential: (idToken: string) => void }) {
  const clientId = googleClientId
  const containerRef = useRef<HTMLDivElement>(null)
  const onCredentialRef = useRef(onCredential)

  useEffect(() => {
    onCredentialRef.current = onCredential
  }, [onCredential])

  useEffect(() => {
    if (!clientId) {
      return
    }
    let cancelled = false
    loadScript(SCRIPT_URL)
      .then(() => {
        if (cancelled || !window.google || !containerRef.current) {
          return
        }
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: (response) => onCredentialRef.current(response.credential),
        })
        window.google.accounts.id.renderButton(containerRef.current, {
          theme: 'outline',
          size: 'large',
          width: 320,
          text: 'continue_with',
          shape: 'rectangular',
        })
      })
      .catch(() => undefined)
    return () => {
      cancelled = true
    }
  }, [clientId])

  if (!clientId) {
    return null
  }
  return <div ref={containerRef} data-testid="google-signin" />
}
