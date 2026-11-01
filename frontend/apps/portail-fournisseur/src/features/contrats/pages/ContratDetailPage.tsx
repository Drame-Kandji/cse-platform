import { ArrowLeft, SearchX } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router-dom'
import { Button } from '@/shared/ui/button'
import { Card, CardContent } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { Skeleton } from '@/shared/ui/skeleton'
import { useContrat, useFactures } from '@/api/queries'
import { paths } from '@/app/routes'
import { ProgressSteps } from '@/components/ProgressSteps'
import { ContratBadge } from '@/domain/badges'
import { actionsContrat, ETAPES_CONTRAT, etapeCourante } from '@/domain/contrats'
import { estIntrouvable } from '@/domain/errors'
import { contratFacture } from '@/domain/factures'
import type { Jalon } from '@/domain/types'
import { ClausesCard } from '../components/ClausesCard'
import { ContratActions } from '../components/ContratActions'
import { ContratInfos } from '../components/ContratInfos'
import { JalonDialog } from '../components/JalonDialog'
import { JalonsCard } from '../components/JalonsCard'

/** Detail d'un contrat : progression, faits cles, clauses, jalons et actions selon le statut. */
export function Component() {
  const { t } = useTranslation('portail')
  const { id = '' } = useParams()
  const query = useContrat(id)
  const factures = useFactures()
  const [jalon, setJalon] = useState<Jalon | null>(null)

  if (query.isPending) {
    return (
      <div className="grid gap-4">
        <Skeleton className="h-9 w-2/3" />
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }
  if (query.isError) {
    return estIntrouvable(query.error) ? (
      <EmptyState
        icon={<SearchX aria-hidden />}
        title={t('contrats.introuvable')}
        action={
          <Button asChild variant="secondary" leftIcon={<ArrowLeft />}>
            <Link to={paths.contrats}>{t('nav.contrats')}</Link>
          </Button>
        }
      />
    ) : (
      <ErrorState error={query.error} onRetry={() => void query.refetch()} />
    )
  }

  const contrat = query.data
  const liste = factures.data?.content ?? []
  const actions = factures.isPending
    ? actionsContrat(contrat, []).filter((a) => a !== 'FACTURER')
    : actionsContrat(contrat, liste)
  return (
    <>
      <PageHeader
        title={contrat.objet}
        subtitle={`${contrat.numero} · ${contrat.appelOffreReference}`}
        meta={<ContratBadge statut={contrat.statut} />}
        actions={actions.length > 0 && <ContratActions contrat={contrat} actions={actions} />}
      />
      <Card className="mb-4">
        <CardContent className="grid gap-3 pt-5">
          <ProgressSteps
            label={t('contrats.progression')}
            steps={ETAPES_CONTRAT}
            current={etapeCourante(contrat.statut, contratFacture(contrat.id, liste))}
          />
          {contrat.statut === 'BROUILLON' && contrat.motifRefusSignature && (
            <p className="text-sm text-muted-foreground">
              {t('contrats.refuseMotif', { motif: contrat.motifRefusSignature })}
            </p>
          )}
        </CardContent>
      </Card>
      <div className="grid gap-4 lg:grid-cols-3">
        <div className="grid content-start gap-4 lg:col-span-2">
          <JalonsCard jalons={contrat.jalons} editable={actions.includes('JALON')} onAtteint={setJalon} />
          <ClausesCard clauses={contrat.clauses} />
        </div>
        <ContratInfos contrat={contrat} />
      </div>
      <JalonDialog
        contratId={contrat.id}
        jalon={jalon}
        onClose={() => {
          setJalon(null)
        }}
      />
    </>
  )
}
