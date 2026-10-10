import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch } from './client'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('apiFetch', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    // Reset cookies between tests.
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('echoes the CSRF cookie on unsafe requests', async () => {
    document.cookie = 'XSRF-TOKEN=token-abc; path=/'
    fetchMock.mockResolvedValue(jsonResponse({ ok: true }, 202))

    await apiFetch('/api/generations', { method: 'POST', body: '{}' })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [, init] = fetchMock.mock.calls[0]
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('token-abc')
  })

  it('does not send the header on GET requests', async () => {
    document.cookie = 'XSRF-TOKEN=token-abc; path=/'
    fetchMock.mockResolvedValue(jsonResponse({ ok: true }))

    await apiFetch('/api/me')

    const [, init] = fetchMock.mock.calls[0]
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBeNull()
  })

  it('bootstraps the CSRF cookie when it is missing', async () => {
    fetchMock
      .mockImplementationOnce(() => {
        document.cookie = 'XSRF-TOKEN=fresh-token; path=/'
        return Promise.resolve(jsonResponse({ token: 'fresh-token' }))
      })
      .mockResolvedValue(jsonResponse({ ok: true }, 202))

    await apiFetch('/api/auth/otp/request', { method: 'POST', body: '{}' })

    expect(fetchMock.mock.calls[0][0]).toBe('/api/auth/csrf')
    const [, init] = fetchMock.mock.calls[1]
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('fresh-token')
  })

  it('refreshes a stale token and retries once on 403', async () => {
    document.cookie = 'XSRF-TOKEN=stale; path=/'
    let postCalls = 0
    fetchMock.mockImplementation((url: string) => {
      if (url === '/api/auth/csrf') {
        document.cookie = 'XSRF-TOKEN=refreshed; path=/'
        return Promise.resolve(jsonResponse({ token: 'refreshed' }))
      }
      postCalls += 1
      return Promise.resolve(
        postCalls === 1
          ? jsonResponse({ code: 'FORBIDDEN' }, 403)
          : jsonResponse({ ok: true }, 200),
      )
    })

    const result = await apiFetch<{ ok: boolean }>('/api/ideas/1/messages', {
      method: 'POST',
      body: '{}',
    })

    expect(result.ok).toBe(true)
    const retryInit = fetchMock.mock.calls[2][1]
    expect(new Headers(retryInit.headers).get('X-XSRF-TOKEN')).toBe('refreshed')
  })

  it('gives up after a single retry', async () => {
    document.cookie = 'XSRF-TOKEN=stale; path=/'
    fetchMock.mockResolvedValue(jsonResponse({ code: 'FORBIDDEN', detail: 'Access denied.' }, 403))

    await expect(apiFetch('/api/ideas/1/messages', { method: 'POST', body: '{}' })).rejects.toThrow(
      'Access denied.',
    )
    // First POST + CSRF refresh + one retry, nothing more.
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('parses problem+json errors into ApiError', async () => {
    document.cookie = 'XSRF-TOKEN=token; path=/'
    fetchMock.mockResolvedValue(
      jsonResponse({ code: 'INSUFFICIENT_CREDITS', detail: 'Not enough credits.' }, 402),
    )

    try {
      await apiFetch('/api/generations', { method: 'POST', body: '{}' })
      throw new Error('should have thrown')
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError)
      expect((error as ApiError).code).toBe('INSUFFICIENT_CREDITS')
      expect((error as ApiError).status).toBe(402)
    }
  })
})
