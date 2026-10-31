import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Outlet, useLocation, useNavigation } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'
import { storage, STORAGE_KEYS } from '@/shared/lib/storage'
import { ProgressBar } from '@/shared/ui/skeleton'
import { AssistantLauncher, AssistantPanel } from './AssistantPanel'
import { CommandPalette } from './CommandPalette'
import { ShellContext, type ShellContextValue } from './shell-context'
import { MobileNav, Sidebar } from './Sidebar'
import { Topbar } from './Topbar'

/** Coquille applicative : sidebar, barre superieure, contenu, palette et assistant. */
export function AppShell() {
  const { t } = useTranslation()
  const navigation = useNavigation()
  const location = useLocation()
  const [sidebarCollapsed, setSidebarCollapsed] = useState(
    () => storage.get(STORAGE_KEYS.sidebarCollapsed) === 'true',
  )
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const [paletteOpen, setPaletteOpen] = useState(false)
  const [assistantOpen, setAssistantOpen] = useState(false)

  const toggleSidebar = useCallback(() => {
    setSidebarCollapsed((current) => {
      storage.set(STORAGE_KEYS.sidebarCollapsed, String(!current))
      return !current
    })
  }, [])

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setPaletteOpen((open) => !open)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => {
      window.removeEventListener('keydown', onKeyDown)
    }
  }, [])

  useEffect(() => {
    setMobileNavOpen(false)
  }, [location.pathname])

  const value = useMemo<ShellContextValue>(
    () => ({
      sidebarCollapsed,
      toggleSidebar,
      mobileNavOpen,
      setMobileNavOpen,
      paletteOpen,
      setPaletteOpen,
      assistantOpen,
      setAssistantOpen,
    }),
    [sidebarCollapsed, toggleSidebar, mobileNavOpen, paletteOpen, assistantOpen],
  )

  return (
    <ShellContext.Provider value={value}>
      <a
        href="#contenu"
        className="sr-only z-50 rounded-lg bg-primary px-4 py-2 text-primary-foreground focus:not-sr-only focus:fixed focus:left-4 focus:top-4"
      >
        {t('shell.skipToContent')}
      </a>
      <ProgressBar active={navigation.state === 'loading'} className="fixed inset-x-0 top-0 z-50" />
      <Sidebar />
      <MobileNav />
      <div
        className={cn(
          'flex min-h-dvh flex-col transition-[padding] duration-200 ease-out',
          sidebarCollapsed ? 'lg:pl-sidebar-collapsed' : 'lg:pl-sidebar',
        )}
      >
        <Topbar />
        <main
          id="contenu"
          tabIndex={-1}
          className="flex-1 px-4 pb-28 pt-6 focus:outline-none sm:px-6 sm:pt-8 lg:px-8"
        >
          <div className="mx-auto w-full max-w-[90rem]">
            <Outlet />
          </div>
        </main>
      </div>
      <AssistantLauncher />
      <AssistantPanel />
      <CommandPalette />
    </ShellContext.Provider>
  )
}
