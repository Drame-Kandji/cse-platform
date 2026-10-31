import { useTranslation } from 'react-i18next'
import { NavLink } from 'react-router-dom'
import { useAuth } from '@/shared/auth/context'
import { cn } from '@/shared/lib/cn'
import { Tooltip } from '@/shared/ui/tooltip'
import { navigationFor } from '../navigation'

export interface SidebarNavProps {
  collapsed?: boolean
  /** Appele apres navigation (fermeture du tiroir mobile). */
  onNavigate?: () => void
}

/** Liste des liens de navigation groupes, filtres par role. */
export function SidebarNav({ collapsed = false, onNavigate }: SidebarNavProps) {
  const { t } = useTranslation('navigation')
  const { t: tc } = useTranslation()
  const { hasAnyRole } = useAuth()
  const groups = navigationFor(hasAnyRole)
  return (
    <nav aria-label={tc('shell.mainNav')} className="grid gap-3 px-3 py-4">
      {groups.map((group, index) => (
        // Groupes separes par un simple filet (pas d'intitule de section : navigation plus calme).
        <div key={group.key} className="grid gap-0.5" role="group" aria-label={t(`groups.${group.key}`)}>
          {index > 0 && (
            <span className={cn('mb-2 h-px bg-border', collapsed ? 'mx-auto w-6' : 'mx-2.5')} aria-hidden />
          )}
          {group.items.map((item) => {
            const label = t(`items.${item.key}`)
            const Icon = item.icon
            return (
              <Tooltip key={item.key} content={label} side="right" disabled={!collapsed}>
                <NavLink
                  to={item.path}
                  end={item.end}
                  onClick={onNavigate}
                  aria-label={collapsed ? label : undefined}
                  className={({ isActive }) =>
                    cn(
                      'group relative flex h-10 items-center gap-3 rounded-lg px-2.5 text-sm transition-colors lg:h-9',
                      collapsed && 'mx-auto w-10 justify-center px-0',
                      isActive
                        ? 'bg-primary/[0.08] font-medium text-primary-emphasis'
                        : 'text-foreground/80 hover:bg-muted hover:text-foreground',
                    )
                  }
                >
                  {({ isActive }) => (
                    <>
                      <Icon
                        className={cn(
                          'size-[18px] shrink-0 stroke-[1.75]',
                          isActive
                            ? 'text-primary-emphasis'
                            : 'text-muted-foreground group-hover:text-foreground',
                        )}
                        aria-hidden
                      />
                      {!collapsed && <span className="truncate">{label}</span>}
                    </>
                  )}
                </NavLink>
              </Tooltip>
            )
          })}
        </div>
      ))}
    </nav>
  )
}
