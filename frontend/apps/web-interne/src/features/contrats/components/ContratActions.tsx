import { Archive, Info, PackageCheck, PenLine, Truck } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { formatMoney } from '@/shared/lib/format'
import { Button, ConfirmDialog } from '@/shared/ui'
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

/** Actions du Responsable des Achats selon l'etat du contrat (signature, livraison, cloture). */
export function ContratActions({ contrat }: { contrat: Contrat }) {
  const { t } = useTranslation('contrats')
  const signer = useSignerContrat(contrat.id)
  const demarrer = useDemarrerLivraison(contrat.id)
  const cloturer = useCloturerContrat(contrat.id)
  const [livraisonOpen, setLivraisonOpen] = useState(false)
  const { statut } = contrat
  const demarrable = statut === 'ENGAGEMENT_VALIDE'
  const showDemarrer = statut === 'SIGNE' || statut === 'ENGAGEMENT_VALIDE'

  return (
    <div className="grid gap-3">
      <div className="flex flex-wrap items-center gap-2">
        {statut === 'BROUILLON' && (
          /* Apres un refus DAF, le contrat revient en BROUILLON et peut etre signe a nouveau. */
          <ConfirmDialog
            title={t('actions.signerTitle', { numero: contrat.numero })}
            description={t('actions.signerDescription', { montant: formatMoney(contrat.montant) })}
            confirmLabel={t('actions.signer')}
            onConfirm={() => run(() => signer.mutateAsync())}
            trigger={<Button leftIcon={<PenLine />}>{t('actions.signer')}</Button>}
          />
        )}
        {showDemarrer &&
          (demarrable ? (
            <ConfirmDialog
              title={t('actions.demarrerTitle')}
              description={t('actions.demarrerDescription')}
              confirmLabel={t('actions.demarrer')}
              onConfirm={() => run(() => demarrer.mutateAsync())}
              trigger={<Button leftIcon={<Truck />}>{t('actions.demarrer')}</Button>}
            />
          ) : (
            <Button leftIcon={<Truck />} disabled aria-describedby="contrat-demarrer-aide">
              {t('actions.demarrer')}
            </Button>
          ))}
        {statut === 'EN_LIVRAISON' && (
          <Button
            leftIcon={<PackageCheck />}
            onClick={() => {
              setLivraisonOpen(true)
            }}
          >
            {t('actions.confirmer')}
          </Button>
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
      {showDemarrer && !demarrable && (
        <p
          id="contrat-demarrer-aide"
          className="flex max-w-xl items-start gap-2 rounded-lg border border-warning/30 bg-warning/10 p-3 text-sm text-warning-emphasis"
        >
          <Info className="mt-0.5 size-4 shrink-0" aria-hidden />
          {t('actions.demarrerBloque')}
        </p>
      )}
      {statut === 'EN_LIVRAISON' && (
        <LivraisonDialog contrat={contrat} open={livraisonOpen} onOpenChange={setLivraisonOpen} />
      )}
    </div>
  )
}
