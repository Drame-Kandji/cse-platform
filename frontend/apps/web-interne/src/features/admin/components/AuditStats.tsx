import { formatNumber } from '@/shared/lib/format'
import { StatCard } from '@/shared/ui/stat-card'
import { useAuditStatistiques } from '../hooks/useAudit'

/** En-tete statistique du journal d'audit : trois volumes, libelle + chiffre. */
export function AuditStats() {
  const stats = useAuditStatistiques()
  return (
    <div className="grid grid-cols-3 gap-3 sm:gap-4">
      <StatCard label="Événements" value={formatNumber(stats.data?.total)} loading={stats.isPending} />
      <StatCard label="Aujourd'hui" value={formatNumber(stats.data?.aujourdhui)} loading={stats.isPending} />
      <StatCard
        label="7 jours"
        value={formatNumber(stats.data?.septDerniersJours)}
        loading={stats.isPending}
      />
    </div>
  )
}
