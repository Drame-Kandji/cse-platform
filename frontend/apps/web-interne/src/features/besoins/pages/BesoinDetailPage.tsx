import { AlertTriangle, RefreshCw } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router-dom'
import { access } from '@/app/access'
import { Can } from '@/shared/auth'
import { labelOf } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatDate, formatDateTime, formatNumber } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { StatusBadge } from '@/shared/ui/status-badge'
import { softToneClasses } from '@/shared/ui/tone'
import { BesoinActions } from '../components/BesoinActions'
import { DetailSkeleton, Fact } from '../components/BesoinDetailParts'
import { BesoinHistorique } from '../components/BesoinHistorique'
import { BesoinWorkflow } from '../components/BesoinWorkflow'
import { MaterielsAffectes } from '../components/MaterielsAffectes'
import { useOrienterBesoin } from '../hooks/useBesoinMutations'
import { useBesoin } from '../hooks/useBesoins'

/** Fiche d'un besoin : faits cles, workflow, historique, materiels affectes et actions RM. */
export function Component() {
  const { t } = useTranslation('besoins')
  const { id = '' } = useParams()
  const query = useBesoin(id)
  const orienter = useOrienterBesoin()

  if (query.isPending) return <DetailSkeleton />
  if (query.isError) {
    return (
      <Card>
        <ErrorState error={query.error} onRetry={() => void query.refetch()} />
      </Card>
    )
  }
  const besoin = query.data

  return (
    <>
      <PageHeader
        title={besoin.designation}
        meta={
          <>
            <span className="font-mono text-sm text-muted-foreground">{besoin.reference}</span>
            <StatusBadge kind="statutBesoin" value={besoin.statut} />
            <StatusBadge kind="priorite" value={besoin.priorite} />
          </>
        }
        description={`${besoin.chantierNom} · ${besoin.demandeurNom}`}
        actions={<BesoinActions besoin={besoin} />}
      />

      {besoin.statut === 'REJETE' && besoin.motifRejet && (
        <div
          role="note"
          className={cn('mb-6 rounded-xl p-4 text-sm ring-1 ring-inset', softToneClasses.danger)}
        >
          <p className="font-semibold">{t('detail.motifRejet')}</p>
          <p className="mt-1">{besoin.motifRejet}</p>
        </div>
      )}

      {besoin.statut === 'VALIDE' && besoin.orientationEnAttente && (
        <div
          role="status"
          className={cn(
            'mb-6 flex flex-col gap-3 rounded-xl p-4 text-sm ring-1 ring-inset sm:flex-row sm:items-center',
            softToneClasses.warning,
          )}
        >
          <AlertTriangle className="size-5 shrink-0" aria-hidden />
          <div className="flex-1">
            <p className="font-semibold">{t('detail.orientationTitle')}</p>
            <p>{t('detail.orientationDescription')}</p>
          </div>
          <Can roles={access.validation}>
            <Button
              variant="secondary"
              leftIcon={<RefreshCw />}
              loading={orienter.isPending}
              onClick={() => {
                orienter.mutate(besoin.id)
              }}
            >
              {t('detail.relancer')}
            </Button>
          </Can>
        </div>
      )}

      <Card className="mb-6">
        <CardHeader title={t('detail.workflow')} />
        <CardContent>
          <BesoinWorkflow besoin={besoin} />
        </CardContent>
      </Card>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="grid content-start gap-6 lg:col-span-2">
          <Card>
            <CardHeader title={t('detail.faits')} />
            <CardContent>
              <dl className="grid grid-cols-2 gap-x-6 gap-y-4 sm:grid-cols-3">
                <Fact label={t('detail.chantier')}>{besoin.chantierNom}</Fact>
                <Fact label={t('detail.categorie')}>{labelOf('categorieMateriel', besoin.categorie)}</Fact>
                <Fact label={t('detail.quantite')}>{formatNumber(besoin.quantite)}</Fact>
                <Fact label={t('detail.duree')}>
                  {t('detail.dureeValue', { count: besoin.dureeUtilisationJours })}
                </Fact>
                <Fact label={t('detail.dateBesoin')}>{formatDate(besoin.dateBesoin)}</Fact>
                <Fact label={t('detail.demandeur')}>{besoin.demandeurNom}</Fact>
                <Fact label={t('detail.creeLe')}>{formatDateTime(besoin.createdAt)}</Fact>
                {besoin.validateurNom && <Fact label={t('detail.validateur')}>{besoin.validateurNom}</Fact>}
                {besoin.dateValidation && (
                  <Fact label={t('detail.dateValidation')}>{formatDateTime(besoin.dateValidation)}</Fact>
                )}
              </dl>
              <div className="mt-6 grid gap-4 border-t border-border pt-4">
                <div>
                  <p className="text-xs text-muted-foreground">{t('detail.specifications')}</p>
                  <p className="mt-1 whitespace-pre-line text-sm">
                    {besoin.specifications ?? t('detail.aucuneSpecification')}
                  </p>
                </div>
                {besoin.justification && (
                  <div>
                    <p className="text-xs text-muted-foreground">{t('detail.justification')}</p>
                    <p className="mt-1 whitespace-pre-line text-sm">{besoin.justification}</p>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader title={t('detail.materiels')} />
            <CardContent>
              <MaterielsAffectes ids={besoin.materielIdsAffectes} />
            </CardContent>
          </Card>
        </div>
        <Card className="content-start">
          <CardHeader title={t('detail.historique')} />
          <CardContent>
            <BesoinHistorique besoinId={besoin.id} />
          </CardContent>
        </Card>
      </div>
    </>
  )
}
