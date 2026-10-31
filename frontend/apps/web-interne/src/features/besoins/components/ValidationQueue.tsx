import { CheckCheck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { to } from '@/app/routes'
import { formatDate, formatNumber, formatRelative } from '@/shared/lib/format'
import { Card } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import { useBesoins } from '../hooks/useBesoins'
import { trierParPrioriteEtAnciennete } from '../lib/priorite'
import { BesoinActions } from './BesoinActions'

const FILE = { statut: 'EN_ATTENTE' as const, page: 0, size: 50, sort: 'createdAt,asc' }

/** File des besoins EN_ATTENTE (priorite puis anciennete) avec validation / rejet en ligne. */
export function ValidationQueue() {
  const { t } = useTranslation('besoins')
  const query = useBesoins(FILE)

  if (query.isPending) {
    return (
      <div className="grid gap-3" aria-busy>
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} className="h-16 w-full rounded-xl" />
        ))}
      </div>
    )
  }
  if (query.isError) {
    return (
      <Card>
        <ErrorState error={query.error} onRetry={() => void query.refetch()} />
      </Card>
    )
  }
  const besoins = trierParPrioriteEtAnciennete(query.data.content)
  if (besoins.length === 0) {
    return (
      <Card>
        <EmptyState
          icon={<CheckCheck aria-hidden />}
          title={t('validation.fileVideTitle')}
          description={t('validation.fileVideDescription')}
        />
      </Card>
    )
  }
  return (
    <div className="grid gap-3">
      <Card>
        <ul className="divide-y divide-border" aria-label={t('validation.fileTab')}>
          {besoins.map((besoin) => (
            <li
              key={besoin.id}
              className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0 space-y-0.5">
                <div className="flex flex-wrap items-center gap-2">
                  <Link
                    to={to.besoinDetail(besoin.id)}
                    className="truncate font-medium hover:text-primary-emphasis hover:underline"
                  >
                    {formatNumber(besoin.quantite)} × {besoin.designation}
                  </Link>
                  {besoin.priorite !== 'NORMAL' && <StatusBadge kind="priorite" value={besoin.priorite} />}
                </div>
                <p className="truncate text-sm text-muted-foreground" title={besoin.justification ?? undefined}>
                  <span className="font-mono text-xs">{besoin.reference}</span>
                  {' · '}
                  {besoin.chantierNom}
                  {' · '}
                  {t('columns.dateBesoin')} {formatDate(besoin.dateBesoin)}
                  {' · '}
                  {formatRelative(besoin.createdAt)}
                </p>
              </div>
              <div className="flex shrink-0 flex-wrap items-center gap-2">
                <BesoinActions besoin={besoin} size="sm" />
              </div>
            </li>
          ))}
        </ul>
      </Card>
      {query.data.totalElements > besoins.length && (
        <p className="text-center text-sm text-muted-foreground">
          {t('validation.plusDe', { count: besoins.length, total: query.data.totalElements })}
        </p>
      )}
    </div>
  )
}
