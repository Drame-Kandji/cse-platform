import { AlertOctagon, CalendarPlus } from 'lucide-react'
import { useCallback, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { access } from '@/app/access'
import { Can } from '@/shared/auth'
import { Button } from '@/shared/ui/button'
import { PageHeader } from '@/shared/ui/page-header'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs'
import type { Panne } from '../api/types'
import { MaintenancesTab } from '../components/MaintenancesTab'
import { PannesTab } from '../components/PannesTab'
import { PlanifierDialog } from '../components/PlanifierDialog'
import { SignalerPanneDialog } from '../components/SignalerPanneDialog'

type Onglet = 'maintenances' | 'pannes'

/** Maintenance & pannes : ordres de travail (RM) et signalements (CHEF, RM). Onglet dans l'URL. */
export function Component() {
  const { t } = useTranslation('maintenances')
  const [params, setParams] = useSearchParams()
  const onglet: Onglet = params.get('onglet') === 'pannes' ? 'pannes' : 'maintenances'
  const [planification, setPlanification] = useState<{ panne: Panne | null } | null>(null)
  const [signalement, setSignalement] = useState(false)
  const signaler = useCallback(() => {
    setSignalement(true)
  }, [])
  const creerOt = useCallback((panne: Panne) => {
    setPlanification({ panne })
  }, [])

  return (
    <>
      <PageHeader
        title={t('page.title')}
        description={t('page.description')}
        actions={
          <>
            <Can roles={access.panneSignalement}>
              <Button variant="secondary" size="lg" leftIcon={<AlertOctagon />} onClick={signaler}>
                {t('page.signaler')}
              </Button>
            </Can>
            <Can roles={access.maintenanceGestion}>
              <Button
                size="lg"
                leftIcon={<CalendarPlus />}
                onClick={() => {
                  setPlanification({ panne: null })
                }}
              >
                {t('page.planifier')}
              </Button>
            </Can>
          </>
        }
      />
      <Tabs
        value={onglet}
        onValueChange={(value) => {
          setParams(value === 'pannes' ? { onglet: 'pannes' } : {}, { replace: true })
        }}
      >
        <TabsList>
          <TabsTrigger value="maintenances">{t('page.tabMaintenances')}</TabsTrigger>
          <TabsTrigger value="pannes">{t('page.tabPannes')}</TabsTrigger>
        </TabsList>
        <TabsContent value="maintenances">
          <MaintenancesTab
            onPlanifier={() => {
              setPlanification({ panne: null })
            }}
          />
        </TabsContent>
        <TabsContent value="pannes">
          <PannesTab onSignaler={signaler} onCreerOt={creerOt} />
        </TabsContent>
      </Tabs>
      <PlanifierDialog
        open={planification !== null}
        panne={planification?.panne}
        onOpenChange={(open) => {
          if (!open) setPlanification(null)
        }}
      />
      <SignalerPanneDialog open={signalement} onOpenChange={setSignalement} />
    </>
  )
}
