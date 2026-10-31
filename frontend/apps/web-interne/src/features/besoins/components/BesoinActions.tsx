import { CheckCircle2, Pencil, XCircle } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { useCurrentUser, useHasRole } from '@/shared/auth'
import { Button } from '@/shared/ui/button'
import type { Besoin } from '../api/types'
import { RejeterDialog } from './RejeterDialog'
import { ValiderDialog } from './ValiderDialog'

/**
 * Actions disponibles sur un besoin selon le role et le statut :
 * - demandeur : modifier tant que EN_ATTENTE (R05) ;
 * - Responsable Matériel : valider / rejeter un besoin EN_ATTENTE (R01).
 */
export function BesoinActions({ besoin, size = 'md' }: { besoin: Besoin; size?: 'sm' | 'md' }) {
  const { t } = useTranslation('besoins')
  const user = useCurrentUser()
  const peutValider = useHasRole(access.validation)
  const [dialog, setDialog] = useState<'valider' | 'rejeter' | null>(null)
  const enAttente = besoin.statut === 'EN_ATTENTE'
  const peutModifier = enAttente && besoin.demandeurId === user.id

  if (!enAttente) return null
  return (
    <>
      {peutModifier && (
        <Button asChild variant="secondary" size={size}>
          <Link to={to.besoinModifier(besoin.id)}>
            <Pencil aria-hidden />
            {t('detail.modifier')}
          </Link>
        </Button>
      )}
      {peutValider && (
        <>
          <Button
            variant="secondary"
            size={size}
            leftIcon={<XCircle />}
            onClick={() => {
              setDialog('rejeter')
            }}
          >
            {t('detail.rejeter')}
          </Button>
          <Button
            size={size}
            leftIcon={<CheckCircle2 />}
            onClick={() => {
              setDialog('valider')
            }}
          >
            {t('detail.valider')}
          </Button>
          <ValiderDialog
            besoin={besoin}
            open={dialog === 'valider'}
            onOpenChange={(open) => {
              setDialog(open ? 'valider' : null)
            }}
          />
          <RejeterDialog
            besoin={besoin}
            open={dialog === 'rejeter'}
            onOpenChange={(open) => {
              setDialog(open ? 'rejeter' : null)
            }}
          />
        </>
      )}
    </>
  )
}
