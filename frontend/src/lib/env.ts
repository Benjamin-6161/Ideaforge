/** Build-time configuration from Vite env vars (see frontend/.env.example). */

export const googleClientId: string | undefined = import.meta.env.VITE_GOOGLE_CLIENT_ID
export const turnstileSiteKey: string | undefined = import.meta.env.VITE_TURNSTILE_SITE_KEY

export function isGoogleConfigured(): boolean {
  return Boolean(googleClientId)
}

export function isTurnstileConfigured(): boolean {
  return Boolean(turnstileSiteKey)
}
