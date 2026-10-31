import { ArrowLeft, Eye } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { access } from '@/app/access'
import { paths } from '@/app/routes'
import { isApiError } from '@/shared/api'
import { useHasRole } from '@/shared/auth'
import { formatMoney } from '@/shared/lib/format'
import {
  Button,
  Card,
  CardContent,
  EmptyState,
  ErrorState,
  PageHeader,
  StatusBadge,
  Stepper,
} from '@/shared/ui'
import { ClausesCard } from '../components/ClausesCard'
import { ContratActions } from '../components/ContratActions'
import { CONTRAT_STEPS, contratStepIndex } from '../components/contrat-domain'
import { ContratSummary } from '../components/ContratSummary'
import { ContratDetailSkeleton } from '../components/ContratDetailSkeleton'
import { EngagementCard } from '../components/EngagementCard'
import { JalonsCard } from '../components/JalonsCard'
import { useContrat } from '../hooks/useContrats'

/** Detail d'un contrat : cycle de vie, synthese, engagement, clauses, jalons et actions RA. */
export function Component() {
  const { t } = useTranslation('contrats')
  const { id = '' } = useParams()
  const query = useContrat(id)
  const gestionnaire = useHasRole(access.achatsGestion)
  const back = (
    <Button asChild variant="ghost" size="sm" className="-ml-2 w-fit">
      <Link to={paths.contrats}>
        <ArrowLeft aria-hidden />
        {t('detail.back')}
      </Link>
    </Button>
  )

  if (query.isPending) return <ContratDetailSkeleton />
  if (query.isError) {
    return isApiError(query.error) && query.error.status === 404 ? (
      <EmptyState title={t('detail.notFound')} action={back} />
    ) : (
      <ErrorState error={query.error} onRetry={() => void query.refetch()} />
    )
  }

  const contrat = query.data
  return (
    <>
      <PageHeader
        breadcrumb={back}
        title={contrat.numero}
        subtitle={contrat.objet}
        meta={
          <>
            <StatusBadge kind="statutContrat" value={contrat.statut} />
            <span className="text-sm font-semibold tabular-nums text-foreground">
              {formatMoney(contrat.montant)}
            </span>
          </>
        }
      />
      <div className="grid gap-6">
        <Card>
          <CardContent className="grid gap-5">
            <h2 className="sr-only">{t('detail.lifecycle')}</h2>
            <Stepper steps={CONTRAT_STEPS} current={contratStepIndex(contrat.statut)} />
            {gestionnaire ? (
              <ContratActions contrat={contrat} />
            ) : (
              <p className="flex items-center gap-2 text-sm text-muted-foreground">
                <Eye className="size-4 shrink-0" aria-hidden />
                {t('detail.readOnly')}
              </p>
            )}
          </CardContent>
        </Card>
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="grid content-start gap-6 lg:col-span-2">
            <ContratSummary contrat={contrat} />
            <JalonsCard contrat={contrat} canMark={gestionnaire && contrat.statut === 'EN_LIVRAISON'} />
          </div>
          <div className="grid content-start gap-6">
            <EngagementCard engagement={contrat.engagement} statutContrat={contrat.statut} />
            <ClausesCard contrat={contrat} editable={gestionnaire && contrat.statut === 'BROUILLON'} />
          </div>
        </div>
      </div>
    </>
  )
}
