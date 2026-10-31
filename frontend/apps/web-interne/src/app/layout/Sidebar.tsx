import * as ScrollArea from '@radix-ui/react-scroll-area'
import { PanelLeftClose, PanelLeftOpen } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { cn } from '@/shared/lib/cn'
import { IconButton } from '@/shared/ui/button'
import { Sheet, SheetContent } from '@/shared/ui/sheet'
import { paths } from '../routes'
import { Logo } from './Logo'
import { useShell } from './shell-context'
import { SidebarNav } from './SidebarNav'

function NavScroll({ collapsed, onNavigate }: { collapsed: boolean; onNavigate?: () => void }) {
  return (
    <ScrollArea.Root className="min-h-0 flex-1 overflow-hidden">
      <ScrollArea.Viewport className="size-full">
        <SidebarNav collapsed={collapsed} onNavigate={onNavigate} />
      </ScrollArea.Viewport>
      <ScrollArea.Scrollbar orientation="vertical" className="flex w-1.5 touch-none select-none p-px">
        <ScrollArea.Thumb className="relative flex-1 rounded-full bg-border-strong" />
      </ScrollArea.Scrollbar>
    </ScrollArea.Root>
  )
}

/** Barre laterale desktop (>= lg), repliable en rail d'icones ; preference persistee. */
export function Sidebar() {
  const { t } = useTranslation()
  const { sidebarCollapsed: collapsed, toggleSidebar } = useShell()
  return (
    <aside
      className={cn(
        'fixed inset-y-0 left-0 z-30 hidden flex-col border-r border-border bg-surface transition-[width] duration-200 ease-out lg:flex',
        collapsed ? 'w-sidebar-collapsed' : 'w-sidebar',
      )}
    >
      <div
        className={cn(
          'flex h-topbar shrink-0 items-center border-b border-border',
          collapsed ? 'justify-center px-2' : 'px-5',
        )}
      >
        <Link to={paths.dashboard} className="rounded-lg" aria-label={t('app.fullName')}>
          <Logo collapsed={collapsed} />
        </Link>
      </div>
      <NavScroll collapsed={collapsed} />
      <div
        className={cn(
          'flex shrink-0 items-center border-t border-border p-3',
          collapsed ? 'justify-center' : 'justify-between',
        )}
      >
        {!collapsed && <span className="pl-2.5 text-2xs text-muted-foreground">{t('app.fullName')}</span>}
        <IconButton
          size="icon-sm"
          label={collapsed ? t('shell.expand') : t('shell.collapse')}
          icon={collapsed ? <PanelLeftOpen /> : <PanelLeftClose />}
          onClick={toggleSidebar}
          className="text-muted-foreground"
        />
      </div>
    </aside>
  )
}

/** Tiroir de navigation mobile (< lg). */
export function MobileNav() {
  const { t } = useTranslation()
  const { mobileNavOpen, setMobileNavOpen } = useShell()
  const close = () => {
    setMobileNavOpen(false)
  }
  return (
    <Sheet open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
      <SheetContent side="left" title={t('app.product')} hideHeader bodyClassName="flex flex-col p-0">
        <div className="flex h-topbar shrink-0 items-center border-b border-border px-5">
          <Logo />
        </div>
        <NavScroll collapsed={false} onNavigate={close} />
      </SheetContent>
    </Sheet>
  )
}
