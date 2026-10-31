import { Menu, Search } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { isApplePlatform } from '@/shared/lib/hooks'
import { IconButton } from '@/shared/ui/button'
import { Kbd } from '@/shared/ui/kbd'
import { Breadcrumbs } from './Breadcrumbs'
import { NotificationBell } from './NotificationBell'
import { useShell } from './shell-context'
import { ThemeToggle } from './ThemeToggle'
import { UserMenu } from './UserMenu'

/** Barre superieure : menu mobile, fil d'Ariane, recherche, notifications, theme, utilisateur. */
export function Topbar() {
  const { t } = useTranslation()
  const { setMobileNavOpen, setPaletteOpen } = useShell()
  const openPalette = () => {
    setPaletteOpen(true)
  }
  return (
    <header className="pt-safe sticky top-0 z-20 border-b border-border bg-background/85 backdrop-blur-md supports-[backdrop-filter]:bg-background/70">
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
          className="hidden h-9 w-64 items-center gap-2 rounded-lg border border-border bg-surface px-3 text-sm text-muted-foreground shadow-xs transition-colors hover:border-border-strong hover:text-foreground md:flex"
        >
          <Search className="size-4" aria-hidden />
          <span className="flex-1 truncate text-left">{t('shell.searchButton')}</span>
          <span className="flex items-center gap-0.5">
            <Kbd>{isApplePlatform() ? '⌘' : 'Ctrl'}</Kbd>
            <Kbd>K</Kbd>
          </span>
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
        <span className="mx-1 hidden h-6 w-px bg-border sm:block" aria-hidden />
        <UserMenu />
      </div>
    </header>
  )
}
