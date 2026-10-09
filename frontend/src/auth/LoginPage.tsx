import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { z } from 'zod'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Logo } from '@/components/layout/Logo'
import { isGoogleConfigured } from '@/lib/env'
import { GoogleSignInButton } from './GoogleSignInButton'
import { OtpInput } from './OtpInput'
import { TurnstileWidget } from './TurnstileWidget'
import { useCurrentUser, useGoogleLogin, useRequestOtp, useVerifyOtp } from './use-auth'

const emailFormSchema = z.object({
  email: z.string().trim().email('Enter a valid email address'),
})

const codeFormSchema = z.object({
  code: z.string().regex(/^\d{6}$/, 'Enter the six-digit code'),
})

type EmailForm = z.infer<typeof emailFormSchema>
type CodeForm = z.infer<typeof codeFormSchema>

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'INVALID_CODE':
        return "That code didn't work. Check the six digits and try again."
      case 'TURNSTILE_FAILED':
        return 'Verification failed. Refresh the page and try again.'
      default:
        return error.message
    }
  }
  return 'Something went wrong. Try again.'
}

export default function LoginPage() {
  const { data: user, isPending } = useCurrentUser()
  const location = useLocation()
  const navigate = useNavigate()
  const [step, setStep] = useState<'email' | 'code'>('email')
  const [email, setEmail] = useState('')
  const [turnstileToken, setTurnstileToken] = useState<string | null>(null)
  const [resendIn, setResendIn] = useState(0)

  const requestOtp = useRequestOtp()
  const verifyOtp = useVerifyOtp()
  const googleLogin = useGoogleLogin()

  const emailForm = useForm<EmailForm>({
    resolver: zodResolver(emailFormSchema),
    defaultValues: { email: '' },
  })
  const codeForm = useForm<CodeForm>({
    resolver: zodResolver(codeFormSchema),
    defaultValues: { code: '' },
  })

  useEffect(() => {
    if (resendIn <= 0) {
      return
    }
    const timer = window.setInterval(() => setResendIn((value) => Math.max(0, value - 1)), 1000)
    return () => window.clearInterval(timer)
  }, [resendIn])

  if (!isPending && user) {
    const from = (location.state as { from?: string } | null)?.from
    return <Navigate to={from ?? '/'} replace />
  }

  const sendCode = (values: EmailForm) => {
    requestOtp.mutate(
      { email: values.email, turnstileToken },
      {
        onSuccess: () => {
          setEmail(values.email)
          setStep('code')
          setResendIn(60)
          toast.success('Code sent. Check your inbox.')
        },
        onError: (error) => toast.error(errorMessage(error)),
      },
    )
  }

  const resend = () => {
    requestOtp.mutate(
      { email, turnstileToken },
      {
        onSuccess: () => {
          setResendIn(60)
          toast.success('New code sent.')
        },
        onError: (error) => toast.error(errorMessage(error)),
      },
    )
  }

  const verify = (values: CodeForm) => {
    verifyOtp.mutate(
      { email, code: values.code },
      {
        onSuccess: () => {
          const from = (location.state as { from?: string } | null)?.from
          toast.success('Signed in.')
          navigate(from ?? '/', { replace: true })
        },
        onError: (error) => {
          codeForm.reset()
          toast.error(errorMessage(error))
        },
      },
    )
  }

  return (
    <div className="grid min-h-svh lg:grid-cols-[7fr_5fr]">
      <ProvingGround />
      <main className="flex items-center justify-center p-6 sm:p-10">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2 lg:hidden">
            <Logo />
            <span className="font-heading text-lg font-semibold">IdeaForge</span>
          </div>

          {step === 'email' ? (
            <>
              <h1 className="text-3xl">Sign in to IdeaForge</h1>
              <p className="mt-2 text-sm text-muted-foreground">
                No passwords. We email you a six-digit code.
              </p>
              <form className="mt-6 space-y-4" onSubmit={emailForm.handleSubmit(sendCode)} noValidate>
                <div className="space-y-1.5">
                  <Label htmlFor="email">Email</Label>
                  <Input
                    id="email"
                    type="email"
                    autoComplete="email"
                    placeholder="you@company.com"
                    aria-invalid={emailForm.formState.errors.email ? true : undefined}
                    {...emailForm.register('email')}
                  />
                  {emailForm.formState.errors.email && (
                    <p className="text-xs text-destructive">{emailForm.formState.errors.email.message}</p>
                  )}
                </div>
                <TurnstileWidget onToken={setTurnstileToken} />
                <Button type="submit" size="lg" className="w-full" disabled={requestOtp.isPending}>
                  {requestOtp.isPending ? 'Sending code…' : 'Email me a code'}
                </Button>
              </form>

              {isGoogleConfigured() ? (
                <>
                  <div className="my-6 flex items-center gap-3 text-xs text-muted-foreground">
                    <span className="h-px flex-1 bg-border" />
                    or
                    <span className="h-px flex-1 bg-border" />
                  </div>
                  <GoogleSignInButton
                    onCredential={(idToken) =>
                      googleLogin.mutate(
                        { idToken, turnstileToken },
                        {
                          onSuccess: () => navigate('/', { replace: true }),
                          onError: (error) => toast.error(errorMessage(error)),
                        },
                      )
                    }
                  />
                </>
              ) : null}
              {import.meta.env.DEV && (
                <p className="mt-8 text-xs text-muted-foreground">
                  Dev: codes show up in{' '}
                  <a
                    className="underline underline-offset-2 hover:text-foreground"
                    href="http://localhost:8025"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Mailpit
                  </a>
                  . Test users <span className="font-mono">test1…3@ideaforge.test</span>, code{' '}
                  <span className="font-mono">000000</span>.
                </p>
              )}
            </>
          ) : (
            <>
              <h1 className="text-3xl">Check your email</h1>
              <p className="mt-2 text-sm text-muted-foreground">
                We sent a six-digit code to <span className="font-medium text-foreground">{email}</span>.
              </p>
              <form className="mt-6 space-y-5" onSubmit={codeForm.handleSubmit(verify)} noValidate>
                <Controller
                  control={codeForm.control}
                  name="code"
                  render={({ field }) => (
                    <OtpInput value={field.value} onChange={field.onChange} autoFocus disabled={verifyOtp.isPending} />
                  )}
                />
                <Button type="submit" size="lg" className="w-full" disabled={verifyOtp.isPending}>
                  {verifyOtp.isPending ? 'Verifying…' : 'Verify code'}
                </Button>
              </form>
              <div className="mt-5 flex items-center justify-between text-sm">
                <button
                  type="button"
                  className="text-muted-foreground underline-offset-2 hover:text-foreground hover:underline disabled:opacity-50"
                  onClick={resend}
                  disabled={resendIn > 0 || requestOtp.isPending}
                >
                  {resendIn > 0 ? `Resend in ${resendIn}s` : 'Resend code'}
                </button>
                <button
                  type="button"
                  className="text-muted-foreground underline-offset-2 hover:text-foreground hover:underline"
                  onClick={() => {
                    setStep('email')
                    codeForm.reset()
                  }}
                >
                  Use a different email
                </button>
              </div>
            </>
          )}
        </div>
      </main>
    </div>
  )
}

/** Decorative left panel: shows what a run produces, in the product's own language. */
function ProvingGround() {
  const specimens = [
    { title: 'Claim denial autopilot', tagline: 'Appeals drafted from payer policy diffs', scores: [8, 7, 4, 6], rot: '-rotate-2' },
    { title: 'Menu margin copilot', tagline: 'Reprices dishes as supplier costs move', scores: [7, 8, 5, 5], rot: 'rotate-1' },
    { title: 'Site-log intelligence', tagline: 'Turns daily site reports into risk flags', scores: [6, 7, 6, 7], rot: '-rotate-1' },
  ]
  const labels = ['Demand', 'Profit', 'Competition', 'Novelty']

  return (
    <aside className="relative hidden flex-col justify-between overflow-hidden bg-secondary p-10 text-secondary-foreground lg:flex">
      <div className="hatch-grid pointer-events-none absolute inset-0 opacity-30" aria-hidden="true" />
      <div className="relative flex items-center gap-2">
        <Logo className="bg-background/10 text-secondary-foreground" />
        <span className="font-heading text-lg font-semibold tracking-tight">IdeaForge</span>
      </div>

      <div className="relative max-w-lg">
        <p className="font-mono text-xs text-signal">SaaS idea generator</p>
        <h2 className="mt-3 text-4xl leading-tight xl:text-5xl">
          Five researched ideas, forged from one field.
        </h2>
        <p className="mt-4 max-w-md text-sm leading-relaxed opacity-80">
          Pick an industry. Get five specific, AI-powered SaaS ideas with evidence-backed scores — then
          refine one in chat and finalize it into a build-ready plan.
        </p>

        <div className="mt-10 space-y-3" aria-hidden="true">
          {specimens.map((specimen) => (
            <div
              key={specimen.title}
              className={`${specimen.rot} rounded-lg border border-white/10 bg-background/95 p-4 text-foreground shadow-lg`}
            >
              <p className="font-heading text-base font-semibold">{specimen.title}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">{specimen.tagline}</p>
              <dl className="mt-3 flex gap-3">
                {specimen.scores.map((score, index) => (
                  <div key={labels[index]} className="flex items-center gap-1.5">
                    <dt className="text-[10px] text-muted-foreground">{labels[index]}</dt>
                    <dd className="font-mono text-xs font-medium">{score}/10</dd>
                  </div>
                ))}
              </dl>
            </div>
          ))}
        </div>
      </div>

      <p className="relative font-mono text-xs opacity-70">
        2 credits per run · 0.2 per message · 1 to finalize
      </p>
    </aside>
  )
}
