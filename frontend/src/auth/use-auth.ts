import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createGeneration,
  deleteIdea,
  getCredits,
  getFields,
  getGeneration,
  getGenerationOptions,
  getIdea,
  getIdeas,
  getIdeaVersions,
  getLedger,
  getMe,
  getPackages,
  googleLogin,
  logout,
  patchIdea,
  requestOtp,
  restoreIdeaVersion,
  verifyOtp,
  type CreateGenerationBody,
  type IdeaListParams,
} from '@/api/endpoints'

export const meKey = ['me'] as const
export const creditsKey = ['credits'] as const

export function useCurrentUser() {
  return useQuery({
    queryKey: meKey,
    queryFn: getMe,
    retry: false,
    staleTime: 60_000,
  })
}

export function useCredits() {
  return useQuery({
    queryKey: creditsKey,
    queryFn: getCredits,
    staleTime: 15_000,
  })
}

export function useLedger(page: number, size = 20) {
  return useQuery({
    queryKey: ['credits', 'ledger', page, size],
    queryFn: () => getLedger(page, size),
    placeholderData: (previous) => previous,
  })
}

export function usePackages() {
  return useQuery({
    queryKey: ['credits', 'packages'],
    queryFn: getPackages,
    staleTime: 5 * 60_000,
  })
}

export function useRequestOtp() {
  return useMutation({
    mutationFn: ({ email, turnstileToken }: { email: string; turnstileToken: string | null }) =>
      requestOtp(email, turnstileToken),
  })
}

export function useVerifyOtp() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ email, code }: { email: string; code: string }) => verifyOtp(email, code),
    onSuccess: (user) => {
      queryClient.setQueryData(meKey, user)
      void queryClient.invalidateQueries({ queryKey: creditsKey })
    },
  })
}

export function useGoogleLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ idToken, turnstileToken }: { idToken: string; turnstileToken: string | null }) =>
      googleLogin(idToken, turnstileToken),
    onSuccess: (user) => {
      queryClient.setQueryData(meKey, user)
      void queryClient.invalidateQueries({ queryKey: creditsKey })
    },
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: logout,
    onSettled: () => {
      queryClient.clear()
    },
  })
}

export function useFields() {
  return useQuery({
    queryKey: ['fields'],
    queryFn: getFields,
    staleTime: 10 * 60_000,
  })
}

export function useGenerationOptions() {
  return useQuery({
    queryKey: ['generation-options'],
    queryFn: getGenerationOptions,
    staleTime: 30 * 60_000,
  })
}

export function useCreateGeneration() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ body, idempotencyKey }: { body: CreateGenerationBody; idempotencyKey: string }) =>
      createGeneration(body, idempotencyKey),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: creditsKey })
    },
  })
}

/** Polls the run while it is active; stops once it reaches a terminal state. */
export function useGeneration(id: string | null) {
  return useQuery({
    queryKey: ['generation', id],
    queryFn: () => getGeneration(id as string),
    enabled: Boolean(id),
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'PENDING' || status === 'RUNNING' || status === undefined ? 1500 : false
    },
  })
}

export function useIdeas(params: IdeaListParams) {
  return useQuery({
    queryKey: ['ideas', params],
    queryFn: () => getIdeas(params),
    placeholderData: (previous) => previous,
  })
}

export function useIdea(id: string) {
  return useQuery({
    queryKey: ['idea', id],
    queryFn: () => getIdea(id),
    enabled: Boolean(id),
  })
}

/** Optimistic favorite/archive toggle on list and detail caches. */
export function usePatchIdea() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: { isFavorite?: boolean; isArchived?: boolean } }) =>
      patchIdea(id, body),
    onMutate: async ({ id, body }) => {
      await queryClient.cancelQueries({ queryKey: ['ideas'] })
      queryClient.setQueriesData<{ items: Array<Record<string, unknown>> }>({ queryKey: ['ideas'] }, (previous) => {
        if (!previous) return previous
        return {
          ...previous,
          items: previous.items.map((item) => (item.id === id ? { ...item, ...body } : item)),
        }
      })
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: ['ideas'] })
      void queryClient.invalidateQueries({ queryKey: ['idea'] })
    },
  })
}

export function useDeleteIdea() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteIdea,
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: ['ideas'] })
    },
  })
}

export function useIdeaVersions(id: string) {
  return useQuery({
    queryKey: ['idea', id, 'versions'],
    queryFn: () => getIdeaVersions(id),
    enabled: Boolean(id),
  })
}

export function useRestoreIdeaVersion(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (number: number) => restoreIdeaVersion(id, number),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['idea', id] })
      void queryClient.invalidateQueries({ queryKey: ['idea', id, 'versions'] })
      void queryClient.invalidateQueries({ queryKey: ['ideas'] })
    },
  })
}
