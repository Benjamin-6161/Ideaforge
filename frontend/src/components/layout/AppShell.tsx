import { NavLink, Outlet } from 'react-router'
import { Button } from '@/components/ui/button'
import { CreditChip } from './CreditChip'
import { Logo } from './Logo'
import { MobileNav } from './MobileNav'
import { ThemeToggle } from './ThemeToggle'
import { UserMenu } from './UserMenu'

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/generate', label: 'Generate', end: false },
  { to: '/ideas', label: 'My Ideas', end: false },
]

export function AppShell() {
  return (
    <div className="flex min-h-svh flex-col">
      <header className="sticky top-0 z-40 border-b bg-background/85 backdrop-blur">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center gap-2 px-4">
          <NavLink to="/" className="flex items-center gap-2" aria-label="IdeaForge home">
            <Logo />
            <span className="font-heading text-lg font-semibold tracking-tight">IdeaForge</span>
          </NavLink>

          <nav className="ml-4 hidden items-center gap-1 md:flex" aria-label="Main">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `rounded-md px-3 py-1.5 text-sm transition-colors ${
                    isActive
                      ? 'bg-muted font-medium text-foreground'
                      : 'text-muted-foreground hover:text-foreground'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-2">
            <ThemeToggle />
            <CreditChip />
            <Button asChild size="sm" className="hidden sm:inline-flex">
              <NavLink to="/generate">New generation</NavLink>
            </Button>
            <UserMenu />
            <MobileNav />
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <Outlet />
      </main>

      <footer className="border-t">
        <div className="mx-auto flex w-full max-w-6xl flex-col items-center justify-between gap-1 px-4 py-4 text-xs text-muted-foreground sm:flex-row">
          <p>Scores are AI estimates with cited evidence — not guarantees.</p>
          <p className="font-mono">Credits never expire · payments soon</p>
        </div>
      </footer>
    </div>
  )
}
