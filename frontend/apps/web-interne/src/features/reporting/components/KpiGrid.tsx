import { Sparkline } from '@/shared/ui/sparkline'
import { StatCard } from '@/shared/ui/stat-card'
import { buildKpis } from '../lib/kpis'
import type { Indicateur, Point, Tableau } from '../types'

export interface KpiGridProps {
  tableau: Tableau
  series: Partial<Record<Indicateur, readonly Point[]>>
  /** Nombre maximal de cartes (8 par defaut : deux rangees sur grand ecran). */
  max?: number
}

/** Rangee d'indicateurs cles avec sparklines des series disponibles. */
export function KpiGrid({ tableau, series, max = 8 }: KpiGridProps) {
  const kpis = buildKpis(tableau).slice(0, max)
  return (
    <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Indicateurs clés">
      {kpis.map((kpi) => {
        const values = kpi.serie ? series[kpi.serie]?.map((point) => point.valeur) : undefined
        return (
          <li key={kpi.id} className="grid">
            <StatCard
              label={kpi.label}
              value={kpi.value}
              hint={kpi.hint}
              icon={kpi.icon}
              iconTone={kpi.tone}
              sparkline={
                values && values.length > 1 ? (
                  <Sparkline values={values} className={kpi.sparkClass} />
                ) : undefined
              }
            />
          </li>
        )
      })}
    </ul>
  )
}

/** Squelette de la rangee de KPI. */
export function KpiGridSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {[0, 1, 2, 3].map((index) => (
        <StatCard key={index} label=" " value="" loading />
      ))}
    </div>
  )
}
