import { Archive, MoreHorizontal, PackageCheck, PenLine, Truck } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { formatMoney } from '@/shared/lib/format'
import {
  Button,
  ConfirmDialog,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/shared/ui'
import type { Contrat } from '../api/types'
import { useCloturerContrat, useDemarrerLivraison, useSignerContrat } from '../hooks/useContratActions'
import { LivraisonDialog } from './LivraisonDialog'

/** Ignore l'erreur deja notifiee par toast (evite une promesse rejetee non geree). */
async function run(action: () => Promise<unknown>) {
  try {
    await action()
  } catch {
    /* erreur deja affichee par le toast global ou local */
  }
}

/**
 * Actions du Responsable des Achats selon l'etat du contrat : signature CSE (puis contresignature du
 * fournisseur sur le portail), livraison, cloture. Le demarrage de livraison n'est qu'un secours
 * « hors portail » : le cas nominal est l'expedition declaree par le fournisseur.
 */
export function ContratActions({ contrat }: { contrat: Contrat }) {
  const { t } = useTranslation('contrats')
  const signer = useSignerContrat(contrat.id)
  const demarrer = useDemarrerLivraison(contrat.id)
  const cloturer = useCloturerContrat(contrat.id)
  const [livraisonOpen, setLivraisonOpen] = useState(false)
  const [demarrerOpen, setDemarrerOpen] = useState(false)
  const { statut } = contrat

  return (
    <div className="flex flex-wrap items-center gap-2 sm:justify-end">
      {statut === 'BROUILLON' && (
        /* Apres un refus (fournisseur ou DAF), le contrat revient en BROUILLON et peut etre re-signe. */
        <ConfirmDialog
          title={t('actions.signerTitle', { numero: contrat.numero })}
          description={t('actions.signerDescription', { montant: formatMoney(contrat.montant) })}
          confirmLabel={t('actions.signer')}
          onConfirm={() => run(() => signer.mutateAsync())}
          trigger={<Button leftIcon={<PenLine />}>{t('actions.signer')}</Button>}
        />
      )}
      {statut === 'ENGAGEMENT_VALIDE' && (
        <>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="secondary" size="icon" aria-label={t('actions.more')} title={t('actions.more')}>
                <MoreHorizontal aria-hidden />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-72">
              <DropdownMenuItem
                icon={<Truck />}
                onSelect={() => {
                  setDemarrerOpen(true)
                }}
              >
                {t('actions.demarrer')}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
          <ConfirmDialog
            open={demarrerOpen}
            onOpenChange={setDemarrerOpen}
            title={t('actions.demarrerTitle')}
            description={t('actions.demarrerDescription')}
            confirmLabel={t('actions.demarrerConfirm')}
            onConfirm={() => run(() => demarrer.mutateAsync())}
          />
        </>
      )}
      {statut === 'EN_LIVRAISON' && (
        <>
          <Button
            leftIcon={<PackageCheck />}
            onClick={() => {
              setLivraisonOpen(true)
            }}
          >
            {t('actions.confirmer')}
          </Button>
          <LivraisonDialog contrat={contrat} open={livraisonOpen} onOpenChange={setLivraisonOpen} />
        </>
      )}
      {statut === 'LIVRE' && (
        <ConfirmDialog
          tone="danger"
          title={t('actions.cloturerTitle', { numero: contrat.numero })}
          description={t('actions.cloturerDescription')}
          confirmLabel={t('actions.cloturer')}
          onConfirm={() => run(() => cloturer.mutateAsync())}
          trigger={
            <Button variant="secondary" leftIcon={<Archive />}>
              {t('actions.cloturer')}
            </Button>
          }
        />
      )}
    </div>
  )
}
