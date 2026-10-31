import { StatCard } from '@/shared/ui/stat-card'
import { DASHBOARD_KPI_COUNT, dashboardKpis } from '../lib/kpis'
import type { Tableau } from '../types'

export interface KpiGridProps {
  tableau: Tableau
  /** Nombre maximal de cartes (4 : une seule rangee). */
  max?: number
}

/** Rangee des indicateurs cles du profil : libelle + grand chiffre, sans icone ni courbe. */
export function KpiGrid({ tableau, max = DASHBOARD_KPI_COUNT }: KpiGridProps) {
  const kpis = dashboardKpis(tableau, max)
  return (
    <ul className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4" aria-label="Indicateurs clés">
      {kpis.map((kpi) => (
        <li key={kpi.id} className="grid min-w-0">
          <StatCard label={kpi.label} value={kpi.value} hint={kpi.hint} alert={kpi.alert} />
        </li>
      ))}
    </ul>
  )
}

/** Squelette de la rangee de KPI. */
export function KpiGridSkeleton() {
  return (
    <div className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
      {[0, 1, 2, 3].map((index) => (
        <StatCard key={index} label=" " value="" loading />
      ))}
    </div>
  )
}
