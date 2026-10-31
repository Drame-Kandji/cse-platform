import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { PageHeader } from '@/shared/ui/page-header'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs'
import { ConsolidationList } from '../components/ConsolidationList'
import { ValidationQueue } from '../components/ValidationQueue'

type Onglet = 'file' | 'consolidation'

/** Ecran du Responsable Matériel : file de validation et consolidation des besoins. */
export function Component() {
  const { t } = useTranslation('besoins')
  const [params, setParams] = useSearchParams()
  const onglet: Onglet = params.get('onglet') === 'consolidation' ? 'consolidation' : 'file'
  return (
    <>
      <PageHeader title={t('validation.title')} description={t('validation.description')} />
      <Tabs
        value={onglet}
        onValueChange={(value) => {
          setParams(value === 'file' ? {} : { onglet: value }, { replace: true })
        }}
      >
        <TabsList>
          <TabsTrigger value="file">{t('validation.fileTab')}</TabsTrigger>
          <TabsTrigger value="consolidation">{t('validation.consolidationTab')}</TabsTrigger>
        </TabsList>
        <TabsContent value="file">
          <ValidationQueue />
        </TabsContent>
        <TabsContent value="consolidation">
          <ConsolidationList />
        </TabsContent>
      </Tabs>
    </>
  )
}
