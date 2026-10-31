import { RefreshCw } from 'lucide-react'
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

/** Fiche d'un besoin : en-tete (statut + actions), avancement, faits cles, materiels affectes, historique. */
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
          </>
        }
        subtitle={besoin.chantierNom}
        actions={<BesoinActions besoin={besoin} />}
      />

      {besoin.statut === 'REJETE' && besoin.motifRejet && (
        <div role="note" className={cn('mb-6 rounded-xl px-4 py-3 text-sm ring-1 ring-inset', softToneClasses.danger)}>
          <span className="font-medium">{t('detail.motifRejet')} : </span>
          <span>{besoin.motifRejet}</span>
        </div>
      )}

      {besoin.statut === 'VALIDE' && besoin.orientationEnAttente && (
        <div
          role="status"
          title={t('detail.orientationDescription')}
          className={cn(
            'mb-6 flex flex-wrap items-center justify-between gap-3 rounded-xl px-4 py-3 text-sm ring-1 ring-inset',
            softToneClasses.primary,
          )}
        >
          <p className="font-medium">{t('detail.orientationTitle')}</p>
          <Can roles={access.validation}>
            <Button
              variant="secondary"
              size="sm"
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

      <section aria-label={t('detail.workflow')} className="mb-8 max-w-3xl">
        <BesoinWorkflow besoin={besoin} />
      </section>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="content-start lg:col-span-2">
          <CardContent className="grid gap-6">
            <dl className="grid grid-cols-2 gap-x-6 gap-y-4 sm:grid-cols-3">
              <Fact label={t('detail.categorie')}>{labelOf('categorieMateriel', besoin.categorie)}</Fact>
              <Fact label={t('detail.quantite')}>{formatNumber(besoin.quantite)}</Fact>
              <Fact label={t('detail.priorite')}>{labelOf('priorite', besoin.priorite)}</Fact>
              <Fact label={t('detail.dateBesoin')}>{formatDate(besoin.dateBesoin)}</Fact>
              <Fact label={t('detail.duree')}>
                {t('detail.dureeValue', { count: besoin.dureeUtilisationJours })}
              </Fact>
              <Fact label={t('detail.demandeur')}>{besoin.demandeurNom}</Fact>
              <Fact label={t('detail.creeLe')}>{formatDateTime(besoin.createdAt)}</Fact>
              {besoin.validateurNom && <Fact label={t('detail.validateur')}>{besoin.validateurNom}</Fact>}
              {besoin.dateValidation && (
                <Fact label={t('detail.dateValidation')}>{formatDateTime(besoin.dateValidation)}</Fact>
              )}
            </dl>
            {(besoin.specifications || besoin.justification) && (
              <dl className="grid gap-4 border-t border-border pt-5">
                {besoin.specifications && (
                  <Fact label={t('detail.specifications')} multiline>
                    {besoin.specifications}
                  </Fact>
                )}
                {besoin.justification && (
                  <Fact label={t('detail.justification')} multiline>
                    {besoin.justification}
                  </Fact>
                )}
              </dl>
            )}
            <section aria-label={t('detail.materiels')} className="grid gap-3 border-t border-border pt-5">
              <h2 className="text-sm font-medium">{t('detail.materiels')}</h2>
              <MaterielsAffectes ids={besoin.materielIdsAffectes} />
            </section>
          </CardContent>
        </Card>
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
