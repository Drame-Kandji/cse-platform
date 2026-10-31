import { Plus } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { access } from '@/app/access'
import { Can } from '@/shared/auth'
import { Button } from '@/shared/ui/button'
import { DialogTrigger } from '@/shared/ui/dialog'
import { PageHeader } from '@/shared/ui/page-header'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs'
import { ClassementTab } from '../components/ClassementTab'
import { ComparaisonPrixTab } from '../components/ComparaisonPrixTab'
import { FournisseurFormDialog } from '../components/FournisseurFormDialog'
import { FournisseursTable } from '../components/FournisseursTable'

const TABS = ['liste', 'classement', 'comparaison'] as const
type Tab = (typeof TABS)[number]

/** Ecran Fournisseurs : liste, classement et comparaison de prix. */
export function Component() {
  const { t } = useTranslation('fournisseurs')
  const [params, setParams] = useSearchParams()
  const [creating, setCreating] = useState(false)
  const tab: Tab = TABS.find((item) => item === params.get('onglet')) ?? 'liste'

  return (
    <>
      <PageHeader
        title={t('list.title')}
        actions={
          <Can roles={access.achatsGestion}>
            <FournisseurFormDialog
              open={creating}
              onOpenChange={setCreating}
              trigger={
                <DialogTrigger asChild>
                  <Button leftIcon={<Plus />}>{t('list.create')}</Button>
                </DialogTrigger>
              }
            />
          </Can>
        }
      />
      <Tabs
        value={tab}
        onValueChange={(value) => {
          setParams(value === 'liste' ? {} : { onglet: value }, { replace: true })
        }}
      >
        <TabsList>
          <TabsTrigger value="liste">{t('tabs.liste')}</TabsTrigger>
          <TabsTrigger value="classement">{t('tabs.classement')}</TabsTrigger>
          <TabsTrigger value="comparaison">{t('tabs.comparaison')}</TabsTrigger>
        </TabsList>
        <TabsContent value="liste">
          <FournisseursTable />
        </TabsContent>
        <TabsContent value="classement">
          <ClassementTab />
        </TabsContent>
        <TabsContent value="comparaison">
          <ComparaisonPrixTab />
        </TabsContent>
      </Tabs>
    </>
  )
}
