/**
 * Thin typed fetch wrapper. CSRF token is read from the XSRF-TOKEN cookie and
 * echoed in X-XSRF-TOKEN; all requests are same-origin with credentials.
 */

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: Array<{ field: string; message: string }>

  constructor(
    status: number,
    code: string,
    message: string,
    fieldErrors: Array<{ field: string; message: string }> = [],
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }

  isUnauthorized(): boolean {
    return this.status === 401
  }
}

interface ProblemBody {
  code?: string
  detail?: string
  errors?: Array<{ field: string; message: string }>
}

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : null
}

/** Fetches a fresh CSRF cookie/token, even when a stale cookie is present. */
async function refreshCsrfToken(): Promise<void> {
  await fetch('/api/auth/csrf', { credentials: 'same-origin' })
}

/** Generates the CSRF cookie if this browser does not have one yet. */
async function ensureCsrfCookie(): Promise<void> {
  if (readCookie('XSRF-TOKEN')) {
    return
  }
  await refreshCsrfToken()
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const unsafe = method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS'
  const baseHeaders = new Headers(init.headers)
  if (init.body !== undefined && !baseHeaders.has('Content-Type')) {
    baseHeaders.set('Content-Type', 'application/json')
  }
  if (unsafe && path !== '/api/auth/csrf') {
    await ensureCsrfCookie()
  }

  const send = () => {
    const headers = new Headers(baseHeaders)
    if (unsafe) {
      const token = readCookie('XSRF-TOKEN')
      if (token) {
        headers.set('X-XSRF-TOKEN', token)
      }
    }
    return fetch(path, {
      ...init,
      method,
      headers,
      credentials: 'same-origin',
    })
  }

  let response = await send()
  if (unsafe && path !== '/api/auth/csrf' && response.status === 403) {
    // The token can go stale (expired cookie, server-side rotation). Refresh it
    // once and retry; rejected requests were never executed, and paid endpoints
    // are additionally guarded by Idempotency-Key.
    await refreshCsrfToken()
    response = await send()
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  let body: ProblemBody | undefined
  try {
    body = text ? (JSON.parse(text) as ProblemBody) : undefined
  } catch {
    body = undefined
  }

  if (!response.ok) {
    throw new ApiError(
      response.status,
      body?.code ?? 'UNKNOWN_ERROR',
      body?.detail ?? response.statusText,
      body?.errors ?? [],
    )
  }
  return body as T
}

/** Fetches the CSRF cookie/token before any unsafe request. */
export async function bootstrapCsrf(): Promise<void> {
  await ensureCsrfCookie()
}
