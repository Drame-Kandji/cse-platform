import { Pencil, ShieldAlert } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams, useSearchParams } from 'react-router-dom'
import { access } from '@/app/access'
import { isApiError } from '@/shared/api'
import { Can, useHasRole } from '@/shared/auth'
import { formatDate } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { DialogTrigger } from '@/shared/ui/dialog'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs'
import { CatalogueTab } from '../components/CatalogueTab'
import { EvaluationsTab } from '../components/EvaluationsTab'
import { FournisseurFormDialog } from '../components/FournisseurFormDialog'
import { InformationsTab } from '../components/InformationsTab'
import { LitigesTab } from '../components/LitigesTab'
import { ScoreBar } from '../components/ScoreBar'
import { StatutActions } from '../components/StatutActions'
import { useFournisseur } from '../hooks/useFournisseursQueries'

const TABS = ['informations', 'evaluations', 'catalogue', 'litiges'] as const

/** Fiche fournisseur : informations, documents, evaluations, catalogue, litiges et statut. */
export function Component() {
  const { t } = useTranslation('fournisseurs')
  const { id = '' } = useParams()
  const [params, setParams] = useSearchParams()
  const [editing, setEditing] = useState(false)
  const canSeeLitiges = useHasRole(access.litigesLecture)
  const query = useFournisseur(id)
  const tabs = TABS.filter((tab) => tab !== 'litiges' || canSeeLitiges)
  const tab = tabs.find((item) => item === params.get('onglet')) ?? 'informations'

  if (query.isPending) {
    return (
      <div className="grid gap-4" aria-busy>
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    )
  }
  if (query.isError) {
    if (isApiError(query.error) && query.error.status === 404) {
      return <EmptyState title={t('detail.notFound')} />
    }
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  }
  const f = query.data
  const demande = f.blacklistageDemande

  return (
    <>
      <PageHeader
        title={f.raisonSociale}
        description={`NINEA ${f.ninea}${f.ville ? ` · ${f.ville}` : ''} · ${String(f.nombreEvaluations)} évaluation(s)`}
        meta={
          <>
            <StatusBadge kind="statutFournisseur" value={f.statut} />
            <ScoreBar score={f.scorePerformance} className="w-40" />
          </>
        }
        actions={
          <>
            <StatutActions fournisseur={f} />
            <Can roles={access.achatsGestion}>
              <FournisseurFormDialog
                fournisseur={f}
                open={editing}
                onOpenChange={setEditing}
                trigger={
                  <DialogTrigger asChild>
                    <Button variant="secondary" leftIcon={<Pencil />}>
                      Modifier
                    </Button>
                  </DialogTrigger>
                }
              />
            </Can>
          </>
        }
      />
      {demande && f.statut !== 'BLACKLISTE' && (
        <Card role="status" className="mb-4 flex gap-3 border-warning/40 bg-warning/5 p-4">
          <ShieldAlert className="mt-0.5 size-5 shrink-0 text-warning-emphasis" aria-hidden />
          <div className="grid gap-0.5 text-sm">
            <p className="font-medium">{t('detail.blacklistPending')}</p>
            <p className="text-muted-foreground">
              Demandée par {demande.demandeurNom} le {formatDate(demande.date)} : {demande.motif}
            </p>
          </div>
        </Card>
      )}
      {f.motifStatut && (f.statut === 'SUSPENDU' || f.statut === 'BLACKLISTE') && (
        <p className="mb-4 text-sm text-muted-foreground">
          <span className="font-medium text-foreground">{t('statut.motif')} :</span> {f.motifStatut}
        </p>
      )}
      <Tabs
        value={tab}
        onValueChange={(value) => {
          setParams(value === 'informations' ? {} : { onglet: value }, { replace: true })
        }}
      >
        <TabsList>
          {tabs.map((item) => (
            <TabsTrigger key={item} value={item}>
              {t(`detail.tabs.${item}`)}
            </TabsTrigger>
          ))}
        </TabsList>
        <TabsContent value="informations">
          <InformationsTab fournisseur={f} />
        </TabsContent>
        <TabsContent value="evaluations">
          <EvaluationsTab fournisseurId={f.id} statut={f.statut} />
        </TabsContent>
        <TabsContent value="catalogue">
          <CatalogueTab fournisseurId={f.id} specialites={f.specialites} />
        </TabsContent>
        {canSeeLitiges && (
          <TabsContent value="litiges">
            <LitigesTab fournisseurId={f.id} />
          </TabsContent>
        )}
      </Tabs>
    </>
  )
}
