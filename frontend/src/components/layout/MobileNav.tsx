import { CreditCard, Lightbulb, Menu, PlusCircle, LayoutDashboard } from 'lucide-react'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'

const links = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/generate', label: 'Generate', icon: PlusCircle, end: false },
  { to: '/ideas', label: 'My Ideas', icon: Lightbulb, end: false },
  { to: '/credits', label: 'Credits', icon: CreditCard, end: false },
]

export function MobileNav() {
  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button variant="ghost" size="icon" className="md:hidden" aria-label="Open navigation menu">
          <Menu />
        </Button>
      </SheetTrigger>
      <SheetContent side="right" className="w-64">
        <SheetHeader>
          <SheetTitle className="font-heading">IdeaForge</SheetTitle>
        </SheetHeader>
        <nav className="mt-4 flex flex-col gap-1 px-2">
          {links.map((link) => (
            <SheetClose asChild key={link.to}>
              <Link
                to={link.to}
                className="flex items-center gap-3 rounded-md px-3 py-2 text-sm text-muted-foreground hover:bg-muted hover:text-foreground"
              >
                <link.icon className="size-4" />
                {link.label}
              </Link>
            </SheetClose>
          ))}
        </nav>
      </SheetContent>
    </Sheet>
  )
}
