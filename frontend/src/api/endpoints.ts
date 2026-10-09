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

export type GenerationParameters = Generated['GenerationParameters']

export type IdeaScores = Required<
  Pick<Generated['Scores'], 'demand' | 'profitability' | 'competition' | 'novelty' | 'evidenceAvailable' | 'depth' | 'versionId'>
>

export type GeneratedIdeaSummary = Required<
  Pick<Generated['GeneratedIdeaSummary'], 'id' | 'title' | 'tagline' | 'fieldId' | 'status' | 'versionNumber'>
> & {
  currentVersionId: string | null
  scores: IdeaScores | null
}

export type GenerationRun = Required<Pick<Generated['GenerationRunResponse'], 'id' | 'status' | 'fieldId' | 'createdAt'>> & {
  progressStage: string | null
  errorCode: string | null
  niche: string | null
  completedAt: string | null
  ideas: GeneratedIdeaSummary[]
}

export type CreateGenerationBody = Generated['CreateGenerationRequest'] & { fieldId: string }

type Deep<T> = T extends (infer U)[] ? Deep<U>[] : T extends object ? { [K in keyof T]-?: Deep<T[K]> } : T

export type IdeaContent = Deep<Generated['IdeaContent']>

export type IdeaListItem = Omit<Deep<Generated['IdeaListItem']>, 'niche' | 'currentVersionId' | 'scores'> & {
  niche: string | null
  currentVersionId: string | null
  scores: Deep<Generated['ScoreSummary']> | null
}

export type IdeaListPage = {
  items: IdeaListItem[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type ValidationDetail = Deep<Generated['ValidationDetail']>

export type IdeaDetail = Omit<
  Deep<Generated['IdeaDetail']>,
  'niche' | 'currentVersionId' | 'finalizedAt' | 'validation'
> & {
  niche: string | null
  currentVersionId: string | null
  finalizedAt: string | null
  validation: ValidationDetail | null
}

export type VersionSummary = Omit<Deep<Generated['VersionSummary']>, 'changeSummary' | 'content'> & {
  changeSummary: string | null
  content: IdeaContent | null
}

export type RevalidationInfo = Generated['RevalidationInfo'] & {
  status: string
  errorCode: string | null
  startedAt: string
  completedAt: string | null
}

export type ChatMessage = Required<
  Pick<Generated['ChatMessageResponse'], 'id' | 'role' | 'content' | 'status' | 'createdAt'>
> & {
  resultingVersionId: string | null
  resultingVersionNumber: number | null
}

export type ChatTurn = {
  userMessage: ChatMessage
  assistantMessage: ChatMessage | null
  newVersion: { id: string; versionNumber: number; changeSummary: string } | null
}

export type ChatMessagePage = {
  items: ChatMessage[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type FinalizePlan = Deep<Generated['FinalizePlan']>

export type Finalization = Omit<
  Deep<Generated['FinalizationResponse']>,
  'errorCode' | 'output' | 'buildPromptMd' | 'agentsMd' | 'completedAt'
> & {
  errorCode: string | null
  output: FinalizePlan | null
  buildPromptMd: string | null
  agentsMd: string | null
  completedAt: string | null
}

export interface IdeaListParams {
  fieldId?: string
  status?: string
  favorite?: boolean
  archived?: boolean
  q?: string
  sort?: 'newest' | 'demand' | 'profitability'
  page?: number
  size?: number
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

export const createGeneration = (body: CreateGenerationBody, idempotencyKey: string) =>
  apiFetch<GenerationRun>('/api/generations', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(body),
  })

export const getGeneration = (id: string) => apiFetch<GenerationRun>(`/api/generations/${id}`)

export const getIdeas = (params: IdeaListParams = {}) => {
  const search = new URLSearchParams()
  if (params.fieldId) search.set('fieldId', params.fieldId)
  if (params.status) search.set('status', params.status)
  if (params.favorite !== undefined) search.set('favorite', String(params.favorite))
  if (params.archived !== undefined) search.set('archived', String(params.archived))
  if (params.q) search.set('q', params.q)
  if (params.sort) search.set('sort', params.sort)
  search.set('page', String(params.page ?? 0))
  search.set('size', String(params.size ?? 20))
  return apiFetch<IdeaListPage>(`/api/ideas?${search.toString()}`)
}

export const getIdea = (id: string) => apiFetch<IdeaDetail>(`/api/ideas/${id}`)

export const patchIdea = (id: string, body: { isFavorite?: boolean; isArchived?: boolean }) =>
  apiFetch<IdeaDetail>(`/api/ideas/${id}`, { method: 'PATCH', body: JSON.stringify(body) })

export const deleteIdea = (id: string) => apiFetch<void>(`/api/ideas/${id}`, { method: 'DELETE' })

export const getIdeaVersions = (id: string) => apiFetch<VersionSummary[]>(`/api/ideas/${id}/versions`)

export const getIdeaVersion = (id: string, number: number) =>
  apiFetch<VersionSummary>(`/api/ideas/${id}/versions/${number}`)

export const restoreIdeaVersion = (id: string, number: number) =>
  apiFetch<VersionSummary>(`/api/ideas/${id}/versions/${number}/restore`, { method: 'POST' })

export const getRecentMessages = (ideaId: string, limit = 50) =>
  apiFetch<ChatMessage[]>(`/api/ideas/${ideaId}/messages/recent?limit=${limit}`)

export const sendIdeaMessage = (ideaId: string, content: string, idempotencyKey: string) =>
  apiFetch<ChatTurn>(`/api/ideas/${ideaId}/messages`, {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify({ content }),
  })

export const revalidateIdea = (ideaId: string, idempotencyKey: string) =>
  apiFetch<{ id: string; status: string }>(`/api/ideas/${ideaId}/revalidate`, {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
  })

export const finalizeIdea = (ideaId: string, idempotencyKey: string) =>
  apiFetch<Finalization>(`/api/ideas/${ideaId}/finalize`, {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
  })

export const getFinalization = (ideaId: string) =>
  apiFetch<Finalization>(`/api/ideas/${ideaId}/finalization`)
