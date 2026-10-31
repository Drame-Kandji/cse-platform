import { AlertTriangle, Layers } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { formatNumber } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Card } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { RadioGroup } from '@/shared/ui/radio-group'
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
    <div className="grid gap-4">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="max-w-2xl text-sm text-muted-foreground">{t('validation.consolidationDescription')}</p>
        <RadioGroup
          aria-label={t('filters.statut')}
          orientation="horizontal"
          variant="card"
          className="sm:w-72"
          value={statut}
          onValueChange={setStatut}
          options={[
            { value: 'EN_ATTENTE', label: t('validation.statutEnAttente') },
            { value: 'VALIDE', label: t('validation.statutValide') },
          ]}
        />
      </div>
      {query.isPending ? (
        <div className="grid gap-3 md:grid-cols-2">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-36 rounded-xl" />
          ))}
        </div>
      ) : query.isError ? (
        <Card>
          <ErrorState error={query.error} onRetry={() => void query.refetch()} />
        </Card>
      ) : groupes.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Layers aria-hidden />}
            title={t('validation.consolidationVideTitle')}
            description={t('validation.consolidationVideDescription')}
          />
        </Card>
      ) : (
        <ul className="grid gap-3 md:grid-cols-2">
          {groupes.map((groupe) => (
            <li key={`${groupe.categorie}-${groupe.designation}`}>
              <Card className="grid h-full gap-3 p-4 sm:p-5">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="font-semibold">{groupe.designation}</p>
                    <StatusBadge kind="categorieMateriel" value={groupe.categorie} dot={false} size="sm" />
                  </div>
                  {groupe.doublonPotentiel && (
                    <Badge tone="warning">
                      <AlertTriangle aria-hidden />
                      {t('validation.doublon')}
                    </Badge>
                  )}
                </div>
                <dl className="grid grid-cols-3 gap-3 text-sm">
                  <div>
                    <dt className="text-xs text-muted-foreground">{t('validation.quantiteTotale')}</dt>
                    <dd className="text-lg font-semibold tabular-nums">
                      {formatNumber(groupe.quantiteTotale)}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">{t('validation.nombreDemandes')}</dt>
                    <dd className="font-medium">
                      {t('validation.demandes', { count: groupe.nombreDemandes })}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-xs text-muted-foreground">{t('validation.prioriteMax')}</dt>
                    <dd>
                      <StatusBadge kind="priorite" value={groupe.prioriteMax} />
                    </dd>
                  </div>
                </dl>
                <div>
                  <p className="text-xs text-muted-foreground">{t('validation.chantiers')}</p>
                  <p className="text-sm">{groupe.chantiers.map((c) => c.nom).join(', ')}</p>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
