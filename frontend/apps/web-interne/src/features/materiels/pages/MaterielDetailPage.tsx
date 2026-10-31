import { ArrowLeft, Truck } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { paths } from '@/app/routes'
import { isApiError } from '@/shared/api/errors'
import { useMaterielDetail } from '@/shared/suivi'
import { Badge } from '@/shared/ui/badge'
import { Button } from '@/shared/ui/button'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { StatusBadge } from '@/shared/ui/status-badge'
import { MaterielActions } from '../components/actions/MaterielActions'
import { HistoriqueTabs } from '../components/detail/HistoriqueTabs'
import { DetailSkeleton, MaterielSubtitle } from '../components/detail/DetailParts'
import { MaterielMetrics } from '../components/detail/MaterielMetrics'

/** Fiche engin : identite, indicateurs, actions par role et historiques. */
export function Component() {
  const { id } = useParams()
  const query = useMaterielDetail(id)

  if (query.isPending) return <DetailSkeleton />
  if (query.isError) {
    if (isApiError(query.error) && query.error.status === 404) {
      return (
        <EmptyState
          icon={<Truck aria-hidden />}
          title="Matériel introuvable"
          description="Ce matériel n'existe pas ou a été retiré du parc."
          action={
            <Button variant="secondary" leftIcon={<ArrowLeft />} asChild>
              <Link to={paths.materiels}>Retour au parc</Link>
            </Button>
          }
        />
      )
    }
    if (isApiError(query.error) && query.error.status === 403) {
      return (
        <EmptyState
          icon={<Truck aria-hidden />}
          title="Matériel hors de vos chantiers"
          description="Vous ne pouvez consulter que les matériels affectés à vos chantiers."
          action={
            <Button variant="secondary" leftIcon={<ArrowLeft />} asChild>
              <Link to={paths.materiels}>Retour au parc</Link>
            </Button>
          }
        />
      )
    }
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  }

  const materiel = query.data
  return (
    <>
      <PageHeader
        title={materiel.designation}
        meta={
          <>
            <Badge tone="neutral" variant="outline" className="font-mono">
              {materiel.code}
            </Badge>
            {materiel.immatriculation && (
              <Badge tone="neutral" className="font-mono">
                {materiel.immatriculation}
              </Badge>
            )}
            <StatusBadge kind="etatMateriel" value={materiel.etat} />
            {materiel.reformeDemandee && materiel.etat !== 'REFORME' && (
              <Badge tone="danger" variant="outline">
                Réforme demandée
              </Badge>
            )}
          </>
        }
        subtitle={<MaterielSubtitle materiel={materiel} />}
        actions={<MaterielActions materiel={materiel} />}
      />
      <div className="grid gap-6">
        <MaterielMetrics materiel={materiel} />
        <HistoriqueTabs materiel={materiel} />
      </div>
    </>
  )
}
