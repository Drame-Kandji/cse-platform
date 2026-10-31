import { AlarmClock, BadgeCheck, FileText, Hourglass, Landmark, Wallet } from 'lucide-react'
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

/** Bandeau d'indicateurs financiers (GET /finance/synthese). */
export function SyntheseKpis({ synthese, loading, error, onRetry }: SyntheseKpisProps) {
  const { t } = useTranslation('finance')
  if (error) return <ErrorState error={error} onRetry={onRetry} size="compact" />
  const s = synthese
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
      <StatCard
        label={t('kpi.engage')}
        value={formatMoney(s?.montantEngage)}
        hint={t('kpi.engageHint')}
        icon={<Landmark aria-hidden />}
        loading={loading}
      />
      <StatCard
        label={t('kpi.enAttente')}
        value={formatMoney(s?.montantEngagementsEnAttente)}
        hint={t('kpi.enAttenteHint', { count: s?.engagementsEnAttente ?? 0 })}
        icon={<Hourglass aria-hidden />}
        iconTone="warning"
        loading={loading}
      />
      <StatCard
        label={t('kpi.facture')}
        value={formatMoney(s?.montantFacture)}
        icon={<FileText aria-hidden />}
        iconTone="info"
        loading={loading}
      />
      <StatCard
        label={t('kpi.mandate')}
        value={formatMoney(s?.montantMandate)}
        icon={<BadgeCheck aria-hidden />}
        iconTone="accent"
        loading={loading}
      />
      <StatCard
        label={t('kpi.paye')}
        value={formatMoney(s?.montantPaye)}
        icon={<Wallet aria-hidden />}
        iconTone="success"
        loading={loading}
      />
      <StatCard
        label={t('kpi.retard')}
        value={formatNumber(s?.facturesEnRetard)}
        hint={t('kpi.retardHint')}
        icon={<AlarmClock aria-hidden />}
        iconTone={s && s.facturesEnRetard > 0 ? 'danger' : 'neutral'}
        loading={loading}
      />
    </div>
  )
}
