import { Ban, CheckCircle2, PauseCircle, PlayCircle, ShieldAlert, ShieldX } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { access } from '@/app/access'
import { useCurrentUser, useHasRole } from '@/shared/auth'
import { Button } from '@/shared/ui/button'
import { ConfirmDialog } from '@/shared/ui/alert-dialog'
import type { FournisseurDetail } from '../api/types'
import { useActionStatutFournisseur } from '../hooks/useFournisseurMutations'
import { MotifDialog } from './MotifDialog'

/**
 * Actions de cycle de vie d'un fournisseur selon le role :
 * RA valide, suspend, reactive et demande le blacklistage ; la DG tranche la demande (R09).
 */
export function StatutActions({ fournisseur }: { fournisseur: FournisseurDetail }) {
  const { t } = useTranslation('fournisseurs')
  const isRa = useHasRole(access.achatsGestion)
  const isDg = useHasRole(access.blacklistageDecision)
  const action = useActionStatutFournisseur(fournisseur.id)
  const { statut, raisonSociale } = fournisseur
  const demande = fournisseur.blacklistageDemande
  const enCours = Boolean(demande) && statut !== 'BLACKLISTE'
  const user = useCurrentUser()
  /* Une demande ne peut etre approuvee par son propre auteur (422). */
  const propreDemande = demande?.demandeurNom === user.name

  return (
    <>
      {isRa && statut === 'EN_ATTENTE_VALIDATION' && (
        <ConfirmDialog
          title="Valider l'inscription ?"
          description={`${raisonSociale} : un compte portail sera créé et un e-mail envoyé pour définir le mot de passe.`}
          confirmLabel={t('statut.valider')}
          onConfirm={() => action.mutateAsync({ type: 'valider' }).then(() => undefined)}
          trigger={<Button leftIcon={<CheckCircle2 />}>{t('statut.valider')}</Button>}
        />
      )}
      {isRa && statut === 'ACTIF' && (
        <MotifDialog
          title="Suspendre le fournisseur ?"
          description="Plus invitable aux appels d'offres (R14)."
          confirmLabel={t('statut.suspendre')}
          tone="danger"
          onConfirm={(motif) => action.mutateAsync({ type: 'suspendre', motif })}
          trigger={
            <Button variant="secondary" leftIcon={<PauseCircle />}>
              {t('statut.suspendre')}
            </Button>
          }
        />
      )}
      {isRa && statut === 'SUSPENDU' && (
        <ConfirmDialog
          title="Réactiver le fournisseur ?"
          description={raisonSociale}
          confirmLabel={t('statut.reactiver')}
          onConfirm={() => action.mutateAsync({ type: 'reactiver' }).then(() => undefined)}
          trigger={
            <Button variant="secondary" leftIcon={<PlayCircle />}>
              {t('statut.reactiver')}
            </Button>
          }
        />
      )}
      {isRa && statut === 'ACTIF' && !enCours && (
        <span title="Suspendre d'abord le fournisseur">
          <Button variant="ghost" disabled aria-describedby="blacklist-hint">
            {t('statut.demanderBlacklistage')}
          </Button>
          <span id="blacklist-hint" className="sr-only">
            Suspendre d'abord le fournisseur pour demander son blacklistage.
          </span>
        </span>
      )}
      {isRa && statut === 'SUSPENDU' && !enCours && (
        <MotifDialog
          title="Demander le blacklistage ?"
          description="Soumise à la Direction Générale (R09)."
          confirmLabel={t('statut.demanderBlacklistage')}
          tone="danger"
          onConfirm={(motif) => action.mutateAsync({ type: 'demanderBlacklistage', motif })}
          trigger={
            <Button variant="danger" leftIcon={<ShieldAlert />}>
              {t('statut.demanderBlacklistage')}
            </Button>
          }
        />
      )}
      {isDg && enCours && (
        <>
          <MotifDialog
            title="Refuser le blacklistage ?"
            description="Le fournisseur conserve son statut actuel."
            label="Commentaire"
            required={false}
            confirmLabel={t('statut.refuserBlacklistage')}
            onConfirm={(commentaire) =>
              action.mutateAsync({
                type: 'deciderBlacklistage',
                decision: { approuve: false, commentaire: commentaire || undefined },
              })
            }
            trigger={
              <Button variant="secondary" leftIcon={<ShieldX />}>
                {t('statut.refuserBlacklistage')}
              </Button>
            }
          />
          <MotifDialog
            title="Approuver le blacklistage ?"
            description="Exclusion définitive des consultations."
            label="Commentaire"
            required={false}
            tone="danger"
            confirmLabel={t('statut.approuverBlacklistage')}
            onConfirm={(commentaire) =>
              action.mutateAsync({
                type: 'deciderBlacklistage',
                decision: { approuve: true, commentaire: commentaire || undefined },
              })
            }
            trigger={
              <Button
                variant="danger"
                leftIcon={<Ban />}
                disabled={propreDemande}
                aria-describedby={propreDemande ? 'own-request-hint' : undefined}
              >
                {t('statut.approuverBlacklistage')}
              </Button>
            }
          />
          {propreDemande && (
            <span id="own-request-hint" className="max-w-56 text-xs text-muted-foreground">
              Vous êtes l'auteur de cette demande : un autre membre de la Direction Générale doit l'approuver.
            </span>
          )}
        </>
      )}
    </>
  )
}
