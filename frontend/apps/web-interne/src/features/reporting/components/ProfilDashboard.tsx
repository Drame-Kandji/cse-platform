import { ArrowRight, BarChart3 } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'
import { access } from '@/app/access'
import { paths } from '@/app/routes'
import { Can } from '@/shared/auth/Can'
import { Card } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { ProgressBar, Skeleton } from '@/shared/ui/skeleton'
import { useSeries, useTableau } from '../hooks/useReporting'
import { granulariteFor, type Period } from '../lib/period'
import type { QueueDef } from '../lib/queues'
import type { Indicateur, Profil, Tableau } from '../types'
import { AchatsPipeline } from './AchatsPipeline'
import { ATraiterCard } from './ATraiterCard'
import { FinanceChart } from './charts/FinanceChart'
import { ParcTrendChart } from './charts/ParcTrendChart'
import { PrioriteDonut } from './charts/PrioriteDonut'
import { KpiGrid, KpiGridSkeleton } from './KpiGrid'

export interface ProfilDashboardProps {
  profil: Profil
  period: Period
  queues: readonly QueueDef[]
}

const PARC_SERIES: readonly Indicateur[] = ['TAUX_DISPONIBILITE', 'TAUX_UTILISATION']
const NO_SERIES: readonly Indicateur[] = []

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

/** Le graphique principal du profil (un seul, en bleu) : parc, sinon finances, achats ou besoins. */
function MainChart({
  tableau,
  series,
}: {
  tableau: Tableau
  series: ReturnType<typeof useSeries>
}) {
  if (tableau.parc) {
    return (
      <div data-section="parc">
        <ParcTrendChart
          loading={series.isPending}
          disponibilite={series.points.TAUX_DISPONIBILITE}
          utilisation={series.points.TAUX_UTILISATION}
        />
      </div>
    )
  }
  if (tableau.finances)
    return (
      <div data-section="finances">
        <FinanceChart finances={tableau.finances} />
      </div>
    )
  if (tableau.achats)
    return (
      <div data-section="achats">
        <AchatsPipeline achats={tableau.achats} />
      </div>
    )
  if (tableau.besoins)
    return (
      <div data-section="besoins">
        <PrioriteDonut besoins={tableau.besoins} />
      </div>
    )
  return null
}

/**
 * Corps du tableau de bord d'un profil, volontairement court : 4 KPI, un graphique principal,
 * la liste "A traiter" et un lien vers l'analyse detaillee (Rapports) pour tout le reste.
 */
export function ProfilDashboard({ profil, period, queues }: ProfilDashboardProps) {
  const location = useLocation()
  const query = useTableau({ profil, du: period.du, au: period.au })
  const series = useSeries(query.data?.parc ? PARC_SERIES : NO_SERIES, {
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
    <div className="relative grid gap-6">
      <ProgressBar active={query.isFetching} className="absolute inset-x-0 -top-3" />
      {hasContent ? <KpiGrid tableau={query.data} /> : null}
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="min-w-0 lg:col-span-2">
          <MainChart tableau={query.data} series={series} />
        </div>
        {queues.length > 0 && <ATraiterCard queues={queues} />}
      </div>
      {!hasContent && (
        <Card>
          <EmptyState
            icon={<BarChart3 aria-hidden />}
            title="Aucun indicateur pour cette vue"
            description="Les indicateurs apparaîtront dès que des données seront consolidées."
          />
        </Card>
      )}
      <Can roles={access.rapports}>
        <Link
          to={{ pathname: paths.rapports, search: location.search }}
          className="inline-flex items-center gap-1.5 justify-self-start rounded-md text-sm font-medium text-primary-emphasis hover:underline"
        >
          Voir l'analyse détaillée
          <ArrowRight className="size-4" aria-hidden />
        </Link>
      </Can>
    </div>
  )
}
