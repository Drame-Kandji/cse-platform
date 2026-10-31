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
  EmptyState,
  ErrorState,
  PageHeader,
  StatusBadge,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/shared/ui'
import { ClausesCard } from '../components/ClausesCard'
import { ContratActions } from '../components/ContratActions'
import { contratStepIndex } from '../components/contrat-domain'
import { ContratLifecycle } from '../components/ContratLifecycle'
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
        meta={<StatusBadge kind="statutContrat" value={contrat.statut} />}
        actions={
          gestionnaire ? (
            <ContratActions contrat={contrat} />
          ) : (
            <p className="flex items-center gap-2 text-sm text-muted-foreground">
              <Eye className="size-4 shrink-0" aria-hidden />
              {t('detail.readOnly')}
            </p>
          )
        }
      />
      <div className="grid gap-6">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <ContratLifecycle current={contratStepIndex(contrat.statut)} label={t('detail.lifecycle')} />
          <p className="text-lg font-semibold tabular-nums">{formatMoney(contrat.montant)}</p>
        </div>
        <ContratSummary contrat={contrat} />
        <Tabs defaultValue="clauses">
          <TabsList aria-label="Sections du contrat">
            <TabsTrigger value="clauses">{t('clauses.title')}</TabsTrigger>
            <TabsTrigger value="jalons">
              {t('jalons.title')}
              {contrat.jalons.length > 0 && (
                <span className="ml-1.5 tabular-nums text-muted-foreground">{contrat.jalons.length}</span>
              )}
            </TabsTrigger>
            <TabsTrigger value="engagement">{t('engagement.title')}</TabsTrigger>
          </TabsList>
          <TabsContent value="clauses">
            <ClausesCard contrat={contrat} editable={gestionnaire && contrat.statut === 'BROUILLON'} />
          </TabsContent>
          <TabsContent value="jalons">
            <JalonsCard contrat={contrat} canMark={gestionnaire && contrat.statut === 'EN_LIVRAISON'} />
          </TabsContent>
          <TabsContent value="engagement">
            <EngagementCard engagement={contrat.engagement} statutContrat={contrat.statut} />
          </TabsContent>
        </Tabs>
      </div>
    </>
  )
}
