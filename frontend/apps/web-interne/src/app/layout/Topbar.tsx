import { Menu, Search } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { isApplePlatform } from '@/shared/lib/hooks'
import { IconButton } from '@/shared/ui/button'
import { Breadcrumbs } from './Breadcrumbs'
import { NotificationBell } from './NotificationBell'
import { useShell } from './shell-context'
import { ThemeToggle } from '@/shared/theme/ThemeToggle'
import { UserMenu } from './UserMenu'

/** Barre superieure : menu mobile, fil d'Ariane, recherche, notifications, theme, utilisateur. */
export function Topbar() {
  const { t } = useTranslation()
  const { setMobileNavOpen, setPaletteOpen } = useShell()
  const openPalette = () => {
    setPaletteOpen(true)
  }
  return (
    <header className="pt-safe sticky top-0 z-20 border-b border-border bg-surface/90 backdrop-blur-md supports-[backdrop-filter]:bg-surface/75">
      <div className="flex h-topbar items-center gap-2 px-3 sm:px-6 lg:px-8">
        <IconButton
          className="-ml-1 lg:hidden"
          label={t('shell.openMenu')}
          tooltip={false}
          icon={<Menu />}
          onClick={() => {
            setMobileNavOpen(true)
          }}
        />
        <div className="min-w-0 flex-1">
          <Breadcrumbs />
        </div>
        <button
          type="button"
          onClick={openPalette}
          className="hidden h-9 w-56 items-center gap-2 rounded-lg bg-muted px-3 text-sm text-muted-foreground transition-colors hover:text-foreground md:flex"
        >
          <Search className="size-4" aria-hidden />
          <span className="flex-1 truncate text-left">{t('shell.searchButton')}</span>
          <span className="text-xs text-muted-foreground">{isApplePlatform() ? '⌘K' : 'Ctrl K'}</span>
        </button>
        <IconButton
          className="md:hidden"
          label={t('shell.searchButton')}
          tooltip={false}
          icon={<Search />}
          onClick={openPalette}
        />
        <NotificationBell />
        <ThemeToggle />
        <UserMenu />
      </div>
    </header>
  )
}
