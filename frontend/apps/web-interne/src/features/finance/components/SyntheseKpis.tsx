import { useTranslation } from 'react-i18next'
import { formatMoney, formatNumber } from '@/shared/lib/format'
import { ErrorState } from '@/shared/ui/error-state'
import { StatCard } from '@/shared/ui/stat-card'
import type { SyntheseFinance } from '../api/types'

interface SyntheseKpisProps {
  synthese: SyntheseFinance | undefined
  loading: boolean
  error: unknown
  onRetry: () => void
}

/**
 * Indicateurs financiers (GET /finance/synthese) : montants CUMULES depuis le debut (sans periode,
 * contrairement au tableau de bord), d'ou la mention explicite au-dessus des chiffres.
 */
export function SyntheseKpis({ synthese, loading, error, onRetry }: SyntheseKpisProps) {
  const { t } = useTranslation('finance')
  if (error) return <ErrorState error={error} onRetry={onRetry} size="compact" />
  const s = synthese
  return (
    <section aria-labelledby="finance-cumul" className="grid gap-3">
      <h2 id="finance-cumul" className="text-sm font-normal text-muted-foreground">
        {t('page.cumul')}
      </h2>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label={t('kpi.engage')} value={formatMoney(s?.montantEngage)} loading={loading} />
        <StatCard label={t('kpi.facture')} value={formatMoney(s?.montantFacture)} loading={loading} />
        <StatCard label={t('kpi.paye')} value={formatMoney(s?.montantPaye)} loading={loading} />
        <StatCard
          label={t('kpi.retard')}
          value={formatNumber(s?.facturesEnRetard)}
          alert={(s?.facturesEnRetard ?? 0) > 0}
          loading={loading}
        />
      </div>
    </section>
  )
}
