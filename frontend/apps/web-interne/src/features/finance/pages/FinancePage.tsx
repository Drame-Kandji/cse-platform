import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { access } from '@/app/access'
import { useHasRole } from '@/shared/auth'
import { Badge } from '@/shared/ui/badge'
import { EmptyState } from '@/shared/ui/empty-state'
import { PageHeader } from '@/shared/ui/page-header'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs'
import { BudgetChart } from '../components/BudgetChart'
import { EngagementsPanel } from '../components/EngagementsPanel'
import { FacturesPanel } from '../components/FacturesPanel'
import { SyntheseKpis } from '../components/SyntheseKpis'
import { useSyntheseFinance } from '../hooks/useFinance'

type Onglet = 'engagements' | 'factures'

/** Ecran Finance (DAF ; DG en lecture) : synthese, engagements a valider, factures. */
export function Component() {
  const { t } = useTranslation('finance')
  const synthese = useSyntheseFinance()
  const gestion = useHasRole(access.financeGestion)
  const lectureFactures = useHasRole(access.facturesLecture)
  const [params, setParams] = useSearchParams()
  const onglet: Onglet = params.get('onglet') === 'factures' ? 'factures' : 'engagements'

  return (
    <>
      <PageHeader
        title={t('page.title')}
        meta={
          !gestion && (
            <Badge tone="neutral" title={t('page.readOnly')}>
              {t('page.readOnlyShort')}
            </Badge>
          )
        }
      />
      <div className="grid gap-6">
        <SyntheseKpis
          synthese={synthese.data}
          loading={synthese.isPending}
          error={synthese.error}
          onRetry={() => void synthese.refetch()}
        />
        <BudgetChart synthese={synthese.data} loading={synthese.isPending && !synthese.error} />
        <Tabs
          value={onglet}
          onValueChange={(value) => {
            setParams(
              (current) => {
                const next = new URLSearchParams(current)
                if (value === 'factures') next.set('onglet', value)
                else next.delete('onglet')
                return next
              },
              { replace: true },
            )
          }}
        >
          <TabsList>
            <TabsTrigger value="engagements">
              {t('tabs.engagements')}
              {synthese.data && synthese.data.engagementsEnAttente > 0 && (
                <span className="tabular-nums text-muted-foreground">
                  {synthese.data.engagementsEnAttente}
                </span>
              )}
            </TabsTrigger>
            <TabsTrigger value="factures">
              {t('tabs.factures')}
            </TabsTrigger>
          </TabsList>
          <TabsContent value="engagements">
            <EngagementsPanel />
          </TabsContent>
          <TabsContent value="factures">
            {lectureFactures ? (
              <FacturesPanel />
            ) : (
              <EmptyState
                title={t('factures.noAccess')}
                description={t('factures.noAccessDescription')}
              />
            )}
          </TabsContent>
        </Tabs>
      </div>
    </>
  )
}
