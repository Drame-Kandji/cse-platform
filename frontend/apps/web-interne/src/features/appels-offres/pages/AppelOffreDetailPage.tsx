import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router-dom'
import { access } from '@/app/access'
import { Can, useHasRole } from '@/shared/auth'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs'
import { AppelOffreSummary } from '../components/AppelOffreSummary'
import { AttributionTab } from '../components/AttributionTab'
import { ComparatifTab } from '../components/ComparatifTab'
import { CloturerButton } from '../components/CloturerButton'
import { Countdown } from '../components/Countdown'
import { EvaluationTab } from '../components/EvaluationTab'
import { OffresTab } from '../components/OffresTab'
import { ProcesVerbauxTab } from '../components/ProcesVerbauxTab'
import { useAppelOffre } from '../hooks/useAppelsOffres'

const ONGLETS = ['offres', 'evaluation', 'comparatif', 'attribution', 'pv'] as const
type Onglet = (typeof ONGLETS)[number]

/** Detail d'un appel d'offres : offres, evaluation, comparatif, attribution, proces-verbaux. */
export function Component() {
  const { t } = useTranslation('appelsOffres')
  const { id = '' } = useParams()
  const [params, setParams] = useSearchParams()
  const query = useAppelOffre(id)
  const voitOffres = useHasRole(access.offresLecture)

  if (query.isPending) {
    return (
      <div className="grid gap-4" aria-busy>
        <Skeleton className="h-10 w-80" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  const ao = query.data

  // Le comparatif renvoie 409 AO_NON_CLOTURE tant que l'AO est PUBLIE : onglet masque.
  const disponibles: Onglet[] = voitOffres
    ? ONGLETS.filter((value) => value !== 'comparatif' || ao.statut !== 'PUBLIE')
    : ['attribution']
  const demande = params.get('onglet') as Onglet | null
  const onglet: Onglet =
    demande && disponibles.includes(demande) ? demande : (disponibles[0] ?? 'attribution')

  return (
    <>
      <PageHeader
        title={`${ao.reference} — ${ao.objet}`}
        meta={
          <>
            <StatusBadge kind="statutAO" value={ao.statut} />
            <StatusBadge kind="typeAppelOffre" value={ao.type} dot={false} />
            {ao.statut === 'PUBLIE' && <Countdown dateLimite={ao.dateLimite} className="text-sm" />}
          </>
        }
        actions={
          ao.statut === 'PUBLIE' ? (
            <Can roles={access.achatsGestion}>
              <CloturerButton ao={ao} />
            </Can>
          ) : undefined
        }
      />
      <div className="grid gap-6">
        <AppelOffreSummary ao={ao} />
        <Tabs
          value={onglet}
          onValueChange={(value) => {
            setParams(
              (current) => {
                const next = new URLSearchParams(current)
                next.set('onglet', value)
                return next
              },
              { replace: true },
            )
          }}
        >
          <TabsList aria-label={t('detail.onglets')}>
            {disponibles.map((value) => (
              <TabsTrigger key={value} value={value}>
                {t(`detail.tabs.${value}`)}
              </TabsTrigger>
            ))}
          </TabsList>
          {voitOffres && (
            <>
              <TabsContent value="offres">
                <OffresTab ao={ao} />
              </TabsContent>
              <TabsContent value="evaluation">
                <EvaluationTab ao={ao} />
              </TabsContent>
              {ao.statut !== 'PUBLIE' && (
                <TabsContent value="comparatif">
                  <ComparatifTab ao={ao} />
                </TabsContent>
              )}
              <TabsContent value="pv">
                <ProcesVerbauxTab ao={ao} />
              </TabsContent>
            </>
          )}
          <TabsContent value="attribution">
            <AttributionTab ao={ao} />
          </TabsContent>
        </Tabs>
      </div>
    </>
  )
}
