import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs'
import { useCatalogue, useProfil } from '@/api/queries'
import { CatalogueTab } from '../components/CatalogueTab'
import { DocumentsTab } from '../components/DocumentsTab'
import { ProfilTab } from '../components/ProfilTab'

const ONGLETS = ['profil', 'documents', 'catalogue'] as const
type Onglet = (typeof ONGLETS)[number]

/** Mon entreprise : profil, documents administratifs et catalogue de prix. */
export function Component() {
  const { t } = useTranslation('portail')
  const [params, setParams] = useSearchParams()
  const onglet: Onglet = ONGLETS.find((o) => o === params.get('onglet')) ?? 'profil'
  const profil = useProfil()
  const catalogue = useCatalogue()

  if (profil.isPending) {
    return (
      <div className="grid gap-4">
        <Skeleton className="h-9 w-1/2" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }
  if (profil.isError) return <ErrorState error={profil.error} onRetry={() => void profil.refetch()} />
  const p = profil.data
  const expires = p.documents.filter((d) => d.expire).length
  return (
    <>
      <PageHeader
        title={p.raisonSociale}
        subtitle={t('entreprise.ninea', { ninea: p.ninea })}
        meta={<StatusBadge kind="statutFournisseur" value={p.statut} />}
      />
      <Tabs
        value={onglet}
        onValueChange={(value) => {
          setParams(value === 'profil' ? {} : { onglet: value }, { replace: true })
        }}
      >
        <TabsList>
          <TabsTrigger value="profil">{t('entreprise.tabs.profil')}</TabsTrigger>
          <TabsTrigger value="documents">
            {t('entreprise.tabs.documents')}
            <span
              className={
                expires > 0 ? 'tabular-nums text-danger-emphasis' : 'tabular-nums text-muted-foreground'
              }
            >
              {p.documents.length}
            </span>
          </TabsTrigger>
          <TabsTrigger value="catalogue">
            {t('entreprise.tabs.catalogue')}
            {catalogue.data && (
              <span className="tabular-nums text-muted-foreground">{catalogue.data.length}</span>
            )}
          </TabsTrigger>
        </TabsList>
        <TabsContent value="profil">
          <ProfilTab profil={p} />
        </TabsContent>
        <TabsContent value="documents">
          <DocumentsTab documents={p.documents} />
        </TabsContent>
        <TabsContent value="catalogue">
          <CatalogueTab />
        </TabsContent>
      </Tabs>
    </>
  )
}
