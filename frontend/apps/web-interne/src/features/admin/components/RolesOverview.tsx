import { ShieldCheck } from 'lucide-react'
import { describeStatus } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { dotToneClasses } from '@/shared/ui/tone'
import { useRoles } from '../hooks/useUtilisateurs'

export interface RolesOverviewProps {
  /** Role actuellement filtre (carte mise en avant). */
  selected?: string
  onSelect: (role: string | undefined) => void
}

/** Cartes des roles : effectif, description, exigence OTP ; un clic filtre le tableau. */
export function RolesOverview({ selected, onSelect }: RolesOverviewProps) {
  const query = useRoles()
  if (query.isPending) {
    return (
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {[0, 1, 2, 3].map((index) => (
          <Skeleton key={index} className="h-24 rounded-xl" />
        ))}
      </div>
    )
  }
  if (query.isError)
    return <ErrorState size="compact" error={query.error} onRetry={() => void query.refetch()} />
  return (
    <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4" aria-label="Rôles">
      {query.data.map((role) => {
        const tone = describeStatus('role', role.code).tone
        const active = selected === role.code
        return (
          <li key={role.code}>
            <button
              type="button"
              aria-pressed={active}
              onClick={() => {
                onSelect(active ? undefined : role.code)
              }}
              className={cn(
                'flex h-full w-full flex-col gap-1.5 rounded-xl border bg-surface p-4 text-left shadow-card transition-colors hover:border-border-strong focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                active ? 'border-primary ring-1 ring-primary/30' : 'border-border',
              )}
            >
              <span className="flex items-center justify-between gap-2">
                <span className="flex min-w-0 items-center gap-2 text-sm font-semibold">
                  <span className={cn('size-2 shrink-0 rounded-full', dotToneClasses[tone])} aria-hidden />
                  <span className="truncate">{role.libelle}</span>
                </span>
                <span className="text-lg font-semibold tabular-nums">
                  {formatNumber(role.nombreUtilisateurs)}
                </span>
              </span>
              <span className="line-clamp-2 text-xs text-muted-foreground">{role.description}</span>
              {role.otpObligatoire && (
                <Badge size="sm" tone="info" className="mt-auto w-fit">
                  <ShieldCheck aria-hidden /> OTP obligatoire
                </Badge>
              )}
            </button>
          </li>
        )
      })}
    </ul>
  )
}
