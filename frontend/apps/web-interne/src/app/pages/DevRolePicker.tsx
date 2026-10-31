import {
  Calculator,
  ChevronRight,
  HardHat,
  Landmark,
  ShieldCheck,
  ShoppingCart,
  SquareTerminal,
  Truck,
  type LucideIcon,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { ROLE_LABELS, ROLES, type Role } from '@/shared/domain/labels'
import { Badge } from '@/shared/ui/badge'

const ICONS: Readonly<Record<Role, LucideIcon>> = {
  CHEF_CHANTIER: HardHat,
  RESPONSABLE_MATERIEL: Truck,
  RESPONSABLE_ACHATS: ShoppingCart,
  DAF: Calculator,
  DIRECTION_GENERALE: Landmark,
  ADMIN_SYSTEME: ShieldCheck,
}

/** Selecteur de profil du mode developpement (aucun Keycloak, aucun jeton). */
export function DevRolePicker({ onSelect }: { onSelect: (role: Role) => void }) {
  const { t } = useTranslation('auth')
  return (
    <div>
      <Badge tone="warning" dot>
        {t('dev.warning')}
      </Badge>
      <h2 className="mt-4 text-2xl font-semibold tracking-tight">{t('dev.title')}</h2>
      <p className="mt-2 text-sm text-muted-foreground">{t('dev.subtitle')}</p>
      <ul className="mt-8 grid gap-3 sm:grid-cols-2">
        {ROLES.map((role) => {
          const Icon = ICONS[role]
          const descriptor = ROLE_LABELS[role]
          return (
            <li key={role}>
              <button
                type="button"
                onClick={() => {
                  onSelect(role)
                }}
                aria-label={t('dev.signInAs', { role: descriptor.label })}
                className="group flex h-full w-full items-start gap-3 rounded-xl border border-border bg-surface p-4 text-left shadow-card transition-all hover:-translate-y-px hover:border-primary/40 hover:shadow-overlay"
              >
                <span className="grid size-10 shrink-0 place-items-center rounded-lg bg-muted text-muted-foreground transition-colors group-hover:bg-primary/10 group-hover:text-primary-emphasis">
                  <Icon className="size-5" aria-hidden />
                </span>
                <span className="grid min-w-0 flex-1 gap-0.5">
                  <span className="text-sm font-semibold">{descriptor.label}</span>
                  <span className="text-xs text-muted-foreground">{descriptor.description}</span>
                </span>
                <ChevronRight
                  className="mt-0.5 size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5"
                  aria-hidden
                />
              </button>
            </li>
          )
        })}
      </ul>
      <p className="mt-6 flex items-center gap-2 text-xs text-muted-foreground">
        <SquareTerminal className="size-3.5" aria-hidden />
        VITE_AUTH_MODE=dev
      </p>
    </div>
  )
}
