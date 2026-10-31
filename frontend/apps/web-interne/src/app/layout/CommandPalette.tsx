import * as DialogPrimitive from '@radix-ui/react-dialog'
import { Command } from 'cmdk'
import { ArrowRight, CornerDownLeft, LogOut, Moon, Palette, Plus, Search, Sparkles, Sun } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/shared/auth/context'
import { useTheme } from '@/shared/theme/theme-context'
import { overlayClasses } from '@/shared/ui/dialog'
import { Kbd } from '@/shared/ui/kbd'
import { access } from '../access'
import { navigationFor } from '../navigation'
import { paths } from '../routes'
import { useShell } from './shell-context'

const groupClasses =
  '[&_[cmdk-group-heading]]:px-3 [&_[cmdk-group-heading]]:pb-1.5 [&_[cmdk-group-heading]]:pt-3 [&_[cmdk-group-heading]]:text-2xs [&_[cmdk-group-heading]]:font-semibold [&_[cmdk-group-heading]]:uppercase [&_[cmdk-group-heading]]:tracking-[0.08em] [&_[cmdk-group-heading]]:text-muted-foreground'

function Item({
  icon,
  children,
  hint,
  onSelect,
  keywords,
}: {
  icon: ReactNode
  children: ReactNode
  hint?: string
  onSelect: () => void
  keywords?: readonly string[]
}) {
  return (
    <Command.Item
      onSelect={onSelect}
      keywords={keywords ? [...keywords] : undefined}
      className="group flex h-11 cursor-pointer items-center gap-3 rounded-lg px-3 text-sm outline-none data-[selected=true]:bg-muted [&_svg]:size-4"
    >
      <span className="text-muted-foreground group-data-[selected=true]:text-foreground">{icon}</span>
      <span className="flex-1 truncate">{children}</span>
      {hint && <span className="text-xs text-muted-foreground">{hint}</span>}
      <ArrowRight
        className="text-muted-foreground opacity-0 group-data-[selected=true]:opacity-100"
        aria-hidden
      />
    </Command.Item>
  )
}

/** Palette de commandes (Ctrl/Cmd+K) : navigation filtree par role + actions rapides. */
export function CommandPalette() {
  const { t } = useTranslation()
  const { t: tn } = useTranslation('navigation')
  const navigate = useNavigate()
  const { hasAnyRole, logout } = useAuth()
  const { resolvedTheme, toggleTheme } = useTheme()
  const { paletteOpen, setPaletteOpen, setAssistantOpen } = useShell()
  const run = (action: () => void) => () => {
    setPaletteOpen(false)
    action()
  }
  return (
    <DialogPrimitive.Root open={paletteOpen} onOpenChange={setPaletteOpen}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className={overlayClasses} />
        <DialogPrimitive.Content
          aria-describedby={undefined}
          className="fixed left-1/2 top-[12vh] z-50 w-[calc(100vw-1.5rem)] max-w-xl -translate-x-1/2 overflow-hidden rounded-2xl border border-border bg-surface-raised shadow-overlay data-[state=open]:animate-pop-in"
        >
          <DialogPrimitive.Title className="sr-only">{t('palette.placeholder')}</DialogPrimitive.Title>
          <Command loop className="flex max-h-[min(32rem,70vh)] flex-col">
            <div className="flex items-center gap-3 border-b border-border px-4">
              <Search className="size-4 shrink-0 text-muted-foreground" aria-hidden />
              <Command.Input
                placeholder={t('palette.placeholder')}
                className="h-14 w-full bg-transparent text-[0.9375rem] outline-none placeholder:text-muted-foreground focus-visible:ring-0 focus-visible:ring-offset-0"
              />
              <Kbd>Esc</Kbd>
            </div>
            <Command.List className="scrollbar-thin flex-1 overflow-y-auto p-2">
              <Command.Empty className="py-10 text-center text-sm text-muted-foreground">
                {t('palette.empty')}
              </Command.Empty>
              <Command.Group heading={t('palette.actions')} className={groupClasses}>
                {hasAnyRole(access.besoinNouveau) && (
                  <Item
                    icon={<Plus />}
                    onSelect={run(() => {
                      navigate(paths.besoinNouveau)
                    })}
                    keywords={['creer', 'demande']}
                  >
                    {t('palette.newBesoin')}
                  </Item>
                )}
                <Item
                  icon={<Sparkles />}
                  onSelect={run(() => {
                    setAssistantOpen(true)
                  })}
                  keywords={['ia', 'chat', 'aide']}
                >
                  {t('palette.openAssistant')}
                </Item>
              </Command.Group>
              {navigationFor(hasAnyRole).map((group) => (
                <Command.Group key={group.key} heading={tn(`groups.${group.key}`)} className={groupClasses}>
                  {group.items.map((item) => (
                    <Item
                      key={item.key}
                      icon={<item.icon />}
                      keywords={item.keywords}
                      onSelect={run(() => {
                        navigate(item.path)
                      })}
                    >
                      {tn(`items.${item.key}`)}
                    </Item>
                  ))}
                </Command.Group>
              ))}
              <Command.Group heading={t('palette.preferences')} className={groupClasses}>
                <Item
                  icon={resolvedTheme === 'dark' ? <Sun /> : <Moon />}
                  onSelect={run(toggleTheme)}
                  keywords={['theme', 'sombre', 'clair']}
                >
                  {t('palette.toggleTheme')}
                </Item>
                {import.meta.env.DEV && (
                  <Item
                    icon={<Palette />}
                    onSelect={run(() => {
                      navigate(paths.design)
                    })}
                    keywords={['ui', 'composants']}
                  >
                    {t('palette.designSystem')}
                  </Item>
                )}
                <Item
                  icon={<LogOut />}
                  onSelect={run(() => {
                    void logout()
                  })}
                >
                  {t('shell.logout')}
                </Item>
              </Command.Group>
            </Command.List>
            <div className="flex items-center gap-4 border-t border-border bg-surface-sunken/60 px-4 py-2.5 text-xs text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <Kbd>↑</Kbd>
                <Kbd>↓</Kbd>
                {t('palette.hint')}
              </span>
              <span className="flex items-center gap-1.5">
                <Kbd>
                  <CornerDownLeft className="size-3" aria-hidden />
                </Kbd>
                {t('palette.select')}
              </span>
            </div>
          </Command>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}
