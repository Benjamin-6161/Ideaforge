import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getCredits,
  getLedger,
  getMe,
  getPackages,
  googleLogin,
  logout,
  requestOtp,
  verifyOtp,
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
