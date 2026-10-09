import { apiFetch } from './client'
import type { components } from './schema'

type Generated = components['schemas']

/**
 * Response types derive from the generated OpenAPI schema so client and server
 * cannot drift silently; fields the API always returns are marked required.
 */
export type CurrentUser = Required<Pick<Generated['MeResponse'], 'id' | 'email' | 'role'>> & {
  displayName: string | null
  avatarUrl: string | null
}

export type CostItem = Required<Pick<Generated['CostItem'], 'action' | 'costUnits'>>

export type Credits = {
  balanceUnits: number
  costs: CostItem[]
}

export type LedgerEntry = Required<
  Pick<
    Generated['LedgerEntryResponse'],
    'id' | 'entryType' | 'amountUnits' | 'balanceAfterUnits' | 'createdAt'
  >
> & {
  action: string | null
  refType: string | null
  refId: string | null
}

export type LedgerPage = {
  items: LedgerEntry[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type CreditPackage = Required<
  Pick<
    Generated['CreditPackageResponse'],
    'id' | 'code' | 'name' | 'creditsUnits' | 'priceCents' | 'currency'
  >
>

export type Field = Required<
  Pick<Generated['FieldResponse'], 'id' | 'slug' | 'name' | 'description' | 'icon'>
> & {
  niches: Field[]
}

export type OptionValue = Required<Pick<Generated['OptionValue'], 'value' | 'label'>>

export type GenerationOptions = {
  targetCustomer: OptionValue[]
  customerSize: OptionValue[]
  builderProfile: OptionValue[]
  timeToMvp: OptionValue[]
  startingBudget: OptionValue[]
  aiDepth: OptionValue[]
  aiCapabilities: OptionValue[]
  monetization: OptionValue[]
  region: OptionValue[]
  noveltyPreference: OptionValue[]
}

export const getMe = () => apiFetch<CurrentUser>('/api/me')

export const requestOtp = (email: string, turnstileToken: string | null) =>
  apiFetch<void>('/api/auth/otp/request', {
    method: 'POST',
    body: JSON.stringify({ email, turnstileToken }),
  })

export const verifyOtp = (email: string, code: string) =>
  apiFetch<CurrentUser>('/api/auth/otp/verify', {
    method: 'POST',
    body: JSON.stringify({ email, code }),
  })

export const googleLogin = (idToken: string, turnstileToken: string | null) =>
  apiFetch<CurrentUser>('/api/auth/google', {
    method: 'POST',
    body: JSON.stringify({ idToken, turnstileToken }),
  })

export const logout = () => apiFetch<void>('/api/auth/logout', { method: 'POST' })

export const getCredits = () => apiFetch<Credits>('/api/credits')

export const getLedger = (page = 0, size = 20) =>
  apiFetch<LedgerPage>(`/api/credits/ledger?page=${page}&size=${size}`)

export const getPackages = () => apiFetch<CreditPackage[]>('/api/credits/packages')

export const getFields = () => apiFetch<Field[]>('/api/fields')

export const getGenerationOptions = () => apiFetch<GenerationOptions>('/api/generation-options')
