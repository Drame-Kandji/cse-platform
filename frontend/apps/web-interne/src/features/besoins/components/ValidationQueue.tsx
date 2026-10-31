import { ArrowRight, CheckCheck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { to } from '@/app/routes'
import { labelOf } from '@/shared/domain/labels'
import { formatDate, formatNumber, formatRelative } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
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
          <Skeleton key={i} className="h-32 w-full rounded-xl" />
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
      <ul className="grid gap-3" aria-label={t('validation.fileTab')}>
        {besoins.map((besoin) => (
          <li key={besoin.id}>
            <Card className="flex flex-col gap-4 p-4 sm:p-5 lg:flex-row lg:items-center">
              <div className="min-w-0 flex-1 space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge kind="priorite" value={besoin.priorite} />
                  <span className="font-mono text-xs text-muted-foreground">{besoin.reference}</span>
                  <span className="text-xs text-muted-foreground">
                    {t('validation.attenteDepuis', { duree: formatRelative(besoin.createdAt) })}
                  </span>
                </div>
                <p className="font-semibold">
                  {formatNumber(besoin.quantite)} × {besoin.designation}
                  <span className="ml-2 text-sm font-normal text-muted-foreground">
                    {labelOf('categorieMateriel', besoin.categorie)}
                  </span>
                </p>
                <p className="text-sm text-muted-foreground">
                  {t('validation.pourChantier', {
                    chantier: besoin.chantierNom,
                    demandeur: besoin.demandeurNom,
                  })}
                  {' · '}
                  {t('columns.dateBesoin')} {formatDate(besoin.dateBesoin)}
                </p>
                {besoin.justification && (
                  <p className="line-clamp-2 text-sm italic text-muted-foreground">
                    « {besoin.justification} »
                  </p>
                )}
              </div>
              <div className="flex flex-wrap items-center gap-2 lg:justify-end">
                <Button asChild variant="ghost" className="max-sm:h-11">
                  <Link to={to.besoinDetail(besoin.id)}>
                    {t('validation.voir')}
                    <ArrowRight aria-hidden />
                  </Link>
                </Button>
                <BesoinActions besoin={besoin} />
              </div>
            </Card>
          </li>
        ))}
      </ul>
      {query.data.totalElements > besoins.length && (
        <p className="text-center text-sm text-muted-foreground">
          {t('validation.plusDe', { count: besoins.length, total: query.data.totalElements })}
        </p>
      )}
    </div>
  )
}
