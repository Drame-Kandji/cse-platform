import { Layers } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { labelOf } from '@/shared/domain/labels'
import { formatNumber } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Card } from '@/shared/ui/card'
import { CountTabs } from '@/shared/ui/count-tabs'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import { useConsolidation } from '../hooks/useBesoins'

type StatutConsolidation = 'EN_ATTENTE' | 'VALIDE'

/** Regroupements de besoins similaires (GET /consolidation), doublons potentiels en tete. */
export function ConsolidationList() {
  const { t } = useTranslation('besoins')
  const [statut, setStatut] = useState<StatutConsolidation>('EN_ATTENTE')
  const query = useConsolidation(statut)
  const groupes = [...(query.data ?? [])].sort(
    (a, b) => Number(b.doublonPotentiel) - Number(a.doublonPotentiel) || b.quantiteTotale - a.quantiteTotale,
  )

  return (
    <div className="grid gap-3">
      <CountTabs<StatutConsolidation>
        label={t('filters.statut')}
        className="mb-1"
        value={statut}
        onChange={(value) => {
          if (value) setStatut(value)
        }}
        items={[
          { value: 'EN_ATTENTE', label: t('validation.statutEnAttente') },
          { value: 'VALIDE', label: t('validation.statutValide') },
        ]}
      />
      {query.isPending ? (
        <div className="grid gap-2">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-14 rounded-xl" />
          ))}
        </div>
      ) : query.isError ? (
        <Card>
          <ErrorState error={query.error} onRetry={() => void query.refetch()} />
        </Card>
      ) : groupes.length === 0 ? (
        <Card>
          <EmptyState icon={<Layers aria-hidden />} title={t('validation.consolidationVideTitle')} />
        </Card>
      ) : (
        <Card>
          <ul className="divide-y divide-border" aria-label={t('validation.consolidationTab')}>
            {groupes.map((groupe) => (
              <li
                key={`${groupe.categorie}-${groupe.designation}`}
                className="flex flex-col gap-2 px-5 py-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="min-w-0 space-y-0.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="font-medium">{groupe.designation}</p>
                    {groupe.doublonPotentiel && <Badge tone="primary">{t('validation.doublon')}</Badge>}
                    {groupe.prioriteMax !== 'NORMAL' && (
                      <StatusBadge kind="priorite" value={groupe.prioriteMax} />
                    )}
                  </div>
                  <p className="truncate text-sm text-muted-foreground">
                    {labelOf('categorieMateriel', groupe.categorie)}
                    {' · '}
                    {groupe.chantiers.map((c) => c.nom).join(', ')}
                  </p>
                </div>
                <div className="flex shrink-0 items-baseline gap-2 sm:flex-col sm:items-end sm:gap-0">
                  <span className="text-lg font-semibold tabular-nums" title={t('validation.quantiteTotale')}>
                    {formatNumber(groupe.quantiteTotale)}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {t('validation.demandes', { count: groupe.nombreDemandes })}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  )
}
