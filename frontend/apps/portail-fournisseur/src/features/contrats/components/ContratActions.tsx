import { MoreHorizontal, PenLine, Receipt, Truck } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Button, IconButton } from '@/shared/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/shared/ui/dropdown-menu'
import { to } from '@/app/routes'
import type { ActionContrat } from '@/domain/contrats'
import type { Contrat } from '@/domain/types'
import { ExpedierDialog } from './ExpedierDialog'
import { RefuserDialog } from './RefuserDialog'
import { SignerDialog } from './SignerDialog'

/** Action principale du statut (bouton) et actions secondaires (menu « ... »). */
export function ContratActions({
  contrat,
  actions,
}: {
  contrat: Contrat
  actions: readonly ActionContrat[]
}) {
  const { t } = useTranslation('portail')
  const navigate = useNavigate()
  const [dialog, setDialog] = useState<'signer' | 'refuser' | 'expedier' | null>(null)
  const onOpenChange = (open: boolean) => {
    if (!open) setDialog(null)
  }
  return (
    <>
      {actions.includes('SIGNER') && (
        <Button
          leftIcon={<PenLine />}
          onClick={() => {
            setDialog('signer')
          }}
        >
          {t('contrats.signer.action')}
        </Button>
      )}
      {actions.includes('EXPEDIER') && (
        <Button
          leftIcon={<Truck />}
          onClick={() => {
            setDialog('expedier')
          }}
        >
          {t('contrats.expedier.title')}
        </Button>
      )}
      {actions.includes('FACTURER') && (
        <Button
          leftIcon={<Receipt />}
          onClick={() => {
            navigate(to.nouvelleFacture(contrat.id))
          }}
        >
          {t('contrats.facturer')}
        </Button>
      )}
      {actions.includes('REFUSER') && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <IconButton
              variant="secondary"
              tooltip={false}
              label={t('contrats.plus')}
              icon={<MoreHorizontal />}
            />
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem
              destructive
              onSelect={() => {
                setDialog('refuser')
              }}
            >
              {t('contrats.refuser.title')}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      )}
      <SignerDialog contrat={contrat} open={dialog === 'signer'} onOpenChange={onOpenChange} />
      <RefuserDialog contrat={contrat} open={dialog === 'refuser'} onOpenChange={onOpenChange} />
      <ExpedierDialog contrat={contrat} open={dialog === 'expedier'} onOpenChange={onOpenChange} />
    </>
  )
}
