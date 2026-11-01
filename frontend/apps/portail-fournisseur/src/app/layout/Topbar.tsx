import { Menu } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import { IconButton } from '@/shared/ui/button'
import { LogoMark } from '@/shared/ui/logo'
import { Sheet, SheetContent } from '@/shared/ui/sheet'
import { NAV_ITEMS } from '../navigation'
import { paths } from '../routes'
import { UserMenu } from './UserMenu'

const linkClasses = ({ isActive }: { isActive: boolean }) =>
  cn(
    'inline-flex h-9 items-center gap-2 rounded-lg px-3 text-sm font-medium transition-colors',
    isActive
      ? 'bg-primary/[0.08] text-primary-emphasis'
      : 'text-muted-foreground hover:bg-muted hover:text-foreground',
  )

/** Liens de navigation (barre sur ordinateur, tiroir sur mobile). */
function NavLinks({ vertical = false }: { vertical?: boolean }) {
  const { t } = useTranslation('portail')
  return (
    <ul className={cn('flex gap-1', vertical && 'flex-col')}>
      {NAV_ITEMS.map((item) => (
        <li key={item.key}>
          <NavLink
            to={item.to}
            end={item.end}
            className={(state) => cn(linkClasses(state), vertical && 'h-11 w-full [&_svg]:size-4')}
          >
            {vertical && <item.icon aria-hidden />}
            {t(`nav.${item.key}`)}
          </NavLink>
        </li>
      ))}
    </ul>
  )
}

/** Barre superieure : marque, navigation, theme, utilisateur. */
export function Topbar() {
  const { t } = useTranslation('portail')
  const [open, setOpen] = useState(false)
  const location = useLocation()
  useEffect(() => {
    setOpen(false)
  }, [location.pathname])
  return (
    <header className="pt-safe sticky top-0 z-20 border-b border-border bg-surface/90 backdrop-blur-md supports-[backdrop-filter]:bg-surface/75">
      <div className="mx-auto flex h-topbar w-full max-w-6xl items-center gap-2 px-3 sm:px-6">
        <IconButton
          className="-ml-1 lg:hidden"
          label={t('shell.openMenu')}
          tooltip={false}
          icon={<Menu />}
          onClick={() => {
            setOpen(true)
          }}
        />
        <Link to={paths.accueil} className="mr-4 flex shrink-0 items-center gap-2.5 rounded-lg">
          <LogoMark />
          <span className="hidden text-[0.9375rem] font-semibold tracking-tight sm:inline">
            {t('app.name')}
          </span>
        </Link>
        <nav aria-label={t('nav.label')} className="hidden min-w-0 flex-1 lg:block">
          <NavLinks />
        </nav>
        <div className="ml-auto flex items-center gap-1">
          <ThemeToggle />
          <UserMenu />
        </div>
      </div>
      <Sheet open={open} onOpenChange={setOpen}>
        <SheetContent side="left" title={t('app.name')}>
          <nav aria-label={t('nav.label')}>
            <NavLinks vertical />
          </nav>
        </SheetContent>
      </Sheet>
    </header>
  )
}
