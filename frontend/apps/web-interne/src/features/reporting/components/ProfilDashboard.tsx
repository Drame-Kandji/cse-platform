import { BarChart3 } from 'lucide-react'
import { formatRelative } from '@/shared/lib/format'
import { Card } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { ProgressBar, Skeleton } from '@/shared/ui/skeleton'
import { useSeries, useTableau } from '../hooks/useReporting'
import { granulariteFor, type Period } from '../lib/period'
import type { QueueDef } from '../lib/queues'
import { seriesFor } from '../lib/series'
import type { Profil } from '../types'
import { AchatsPipeline } from './AchatsPipeline'
import { ATraiterCard } from './ATraiterCard'
import { CategorieChart } from './charts/CategorieChart'
import { FinanceChart } from './charts/FinanceChart'
import { ParcTrendChart } from './charts/ParcTrendChart'
import { PrioriteDonut } from './charts/PrioriteDonut'
import { ChantiersTable } from './ChantiersTable'
import { KpiGrid, KpiGridSkeleton } from './KpiGrid'
import { TopFournisseurs } from './TopFournisseurs'

export interface ProfilDashboardProps {
  profil: Profil
  period: Period
  queues: readonly QueueDef[]
}

function DashboardSkeleton() {
  return (
    <div className="grid gap-6" aria-busy>
      <KpiGridSkeleton />
      <div className="grid gap-6 lg:grid-cols-3">
        <Skeleton className="h-80 rounded-xl lg:col-span-2" />
        <Skeleton className="h-80 rounded-xl" />
      </div>
    </div>
  )
}

/**
 * Corps du tableau de bord d'un profil : KPI, graphiques et tableaux, en ne rendant que les
 * sections renvoyees par ms-reporting pour ce profil.
 */
export function ProfilDashboard({ profil, period, queues }: ProfilDashboardProps) {
  const query = useTableau({ profil, du: period.du, au: period.au })
  const tableau = query.data
  const { points, isPending: seriesPending } = useSeries(seriesFor(tableau), {
    granularite: granulariteFor(period),
    du: period.du,
    au: period.au,
  })

  if (query.isPending) return <DashboardSkeleton />
  if (query.isError) {
    return (
      <Card>
        <ErrorState error={query.error} onRetry={() => void query.refetch()} />
      </Card>
    )
  }

  const { parc, besoins, achats, fournisseurs, finances, chantiers } = query.data
  const hasContent = parc ?? besoins ?? achats ?? fournisseurs ?? finances ?? chantiers
  return (
    <div className="grid gap-6">
      <ProgressBar active={query.isFetching} className="-mb-6" />
      <p className="-mt-2 text-xs text-muted-foreground">
        Données consolidées {formatRelative(query.data.genereLe)}
      </p>
      {hasContent ? <KpiGrid tableau={query.data} series={points} /> : null}
      <div className="grid grid-flow-row-dense gap-6 lg:grid-cols-3">
        {parc && (
          <div className="lg:col-span-2" data-section="parc">
            <ParcTrendChart
              loading={seriesPending}
              disponibilite={points.TAUX_DISPONIBILITE}
              utilisation={points.TAUX_UTILISATION}
            />
          </div>
        )}
        {queues.length > 0 && <ATraiterCard queues={queues} />}
        {parc && parc.parCategorie.length > 0 && (
          <div className="lg:col-span-2" data-section="parc-categories">
            <CategorieChart categories={parc.parCategorie} />
          </div>
        )}
        {besoins && (
          <div data-section="besoins">
            <PrioriteDonut besoins={besoins} />
          </div>
        )}
        {finances && (
          <div className="lg:col-span-2" data-section="finances">
            <FinanceChart finances={finances} />
          </div>
        )}
        {fournisseurs && (
          <div data-section="fournisseurs">
            <TopFournisseurs fournisseurs={fournisseurs} />
          </div>
        )}
        {achats && (
          <div data-section="achats">
            <AchatsPipeline achats={achats} />
          </div>
        )}
      </div>
      {chantiers && (
        <div data-section="chantiers">
          <ChantiersTable
            chantiers={chantiers}
            title={profil === 'CHEF_CHANTIER' ? 'Mes chantiers' : 'Chantiers'}
          />
        </div>
      )}
      {!hasContent && (
        <Card>
          <EmptyState
            icon={<BarChart3 aria-hidden />}
            title="Aucun indicateur pour cette vue"
            description="Les indicateurs seront disponibles dès que des données auront été consolidées sur la période."
          />
        </Card>
      )}
    </div>
  )
}
