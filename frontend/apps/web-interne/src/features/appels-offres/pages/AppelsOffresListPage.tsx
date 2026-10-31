import { Plus } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { Can, useHasRole } from '@/shared/auth'
import { Button } from '@/shared/ui/button'
import { CountTabs } from '@/shared/ui/count-tabs'
import { PageHeader } from '@/shared/ui/page-header'
import type { BesoinAAcheter } from '../api/types'
import { AppelsOffresTable } from '../components/AppelsOffresTable'
import { BesoinsAAcheterPanel } from '../components/BesoinsAAcheterPanel'
import { AppelOffreWizard } from '../components/wizard/AppelOffreWizard'
import { useBesoinsALancer } from '../hooks/useAppelsOffres'

type Vue = 'a-lancer'

/** Ecran "Appels d'offres" : liste, besoins a lancer (onglet), creation. */
export function Component() {
  const { t } = useTranslation('appelsOffres')
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const voitBesoins = useHasRole(access.besoinsAAcheter)
  const { query: besoinsQuery, besoins } = useBesoinsALancer(voitBesoins)
  const vue: Vue | null = voitBesoins && params.get('vue') === 'a-lancer' ? 'a-lancer' : null
  const [wizard, setWizard] = useState<{ open: boolean; besoin?: BesoinAAcheter }>({ open: false })
  return (
    <>
      <PageHeader
        title={t('liste.titre')}
        actions={
          <Can roles={access.achatsGestion}>
            <Button
              leftIcon={<Plus />}
              onClick={() => {
                setWizard({ open: true })
              }}
            >
              {t('liste.lancer')}
            </Button>
          </Can>
        }
      />
      {voitBesoins && (
        <CountTabs<Vue>
          label="Vue"
          value={vue}
          loading={besoinsQuery.isPending}
          items={[
            { value: null, label: t('liste.titre') },
            { value: 'a-lancer', label: 'Besoins à lancer', count: besoins.length },
          ]}
          onChange={(value) => {
            setParams(
              (current) => {
                const next = new URLSearchParams(current)
                if (value) next.set('vue', value)
                else next.delete('vue')
                return next
              },
              { replace: true },
            )
          }}
        />
      )}
      {vue === 'a-lancer' ? (
        <BesoinsAAcheterPanel
          onLancer={(besoin) => {
            setWizard({ open: true, besoin })
          }}
        />
      ) : (
        <AppelsOffresTable />
      )}
      <AppelOffreWizard
        open={wizard.open}
        besoin={wizard.besoin}
        onOpenChange={(open) => {
          setWizard((current) => ({ ...current, open }))
        }}
        onCreated={(ao) => {
          setWizard({ open: false })
          navigate(to.appelOffreDetail(ao.id))
        }}
      />
    </>
  )
}
