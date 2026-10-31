import { Plus } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { Can, useHasRole } from '@/shared/auth'
import { Button } from '@/shared/ui/button'
import { PageHeader } from '@/shared/ui/page-header'
import type { BesoinAAcheter } from '../api/types'
import { AppelsOffresTable } from '../components/AppelsOffresTable'
import { BesoinsAAcheterPanel } from '../components/BesoinsAAcheterPanel'
import { AppelOffreWizard } from '../components/wizard/AppelOffreWizard'

/** Ecran "Appels d'offres" : besoins a acheter, liste et creation. */
export function Component() {
  const { t } = useTranslation('appelsOffres')
  const navigate = useNavigate()
  const voitBesoins = useHasRole(access.besoinsAAcheter)
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
      <div className="grid gap-6">
        {voitBesoins && (
          <BesoinsAAcheterPanel
            onLancer={(besoin) => {
              setWizard({ open: true, besoin })
            }}
          />
        )}
        <AppelsOffresTable />
      </div>
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
