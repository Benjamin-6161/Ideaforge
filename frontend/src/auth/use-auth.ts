import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createGeneration,
  getCredits,
  getFields,
  getGeneration,
  getGenerationOptions,
  getLedger,
  getMe,
  getPackages,
  googleLogin,
  logout,
  requestOtp,
  verifyOtp,
  type CreateGenerationBody,
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
