import { ChevronRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { ROLE_LABELS, ROLES, type Role } from '@/shared/domain/labels'
import { Badge } from '@/shared/ui/badge'

/** Selecteur de profil du mode developpement (aucun Keycloak, aucun jeton). */
export function DevRolePicker({ onSelect }: { onSelect: (role: Role) => void }) {
  const { t } = useTranslation('auth')
  return (
    <div>
      <Badge>{t('dev.warning')}</Badge>
      <h1 className="mt-4 text-2xl font-semibold tracking-tight">{t('dev.title')}</h1>
      <p className="mt-2 text-sm text-muted-foreground">{t('dev.subtitle')}</p>
      <ul className="mt-6 grid gap-2 sm:grid-cols-2">
        {ROLES.map((role) => {
          const descriptor = ROLE_LABELS[role]
          return (
            <li key={role}>
              <button
                type="button"
                onClick={() => {
                  onSelect(role)
                }}
                aria-label={t('dev.signInAs', { role: descriptor.label })}
                className="group flex w-full items-center justify-between gap-3 rounded-xl border border-border bg-surface px-4 py-3 text-left text-sm font-medium transition-colors hover:border-primary/40 hover:bg-primary/5"
              >
                {descriptor.label}
                <ChevronRight className="size-4 shrink-0 text-muted-foreground" aria-hidden />
              </button>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
