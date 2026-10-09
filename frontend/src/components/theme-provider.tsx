import { useEffect, useMemo, useState, useSyncExternalStore, type ReactNode } from 'react'
import {
  readStoredTheme,
  subscribeToColorScheme,
  ThemeContext,
  type ResolvedTheme,
  type Theme,
} from './theme-context'

/** Light/dark/system theme with localStorage persistence. */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(readStoredTheme)
  const prefersDark = useSyncExternalStore(
    subscribeToColorScheme,
    () => window.matchMedia('(prefers-color-scheme: dark)').matches,
    () => false,
  )

  const resolvedTheme: ResolvedTheme = theme === 'system' ? (prefersDark ? 'dark' : 'light') : theme

  useEffect(() => {
    document.documentElement.classList.toggle('dark', resolvedTheme === 'dark')
  }, [resolvedTheme])

  const value = useMemo(
    () => ({
      theme,
      resolvedTheme,
      setTheme: (next: Theme) => {
        window.localStorage.setItem('ideaforge-theme', next)
        setThemeState(next)
      },
    }),
    [theme, resolvedTheme],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
