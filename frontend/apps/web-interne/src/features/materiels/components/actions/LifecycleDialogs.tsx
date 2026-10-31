import { labelOf } from '@/shared/domain/labels'
import type { MaterielDetail } from '@/shared/domain/types'
import { formatDate, toIsoDate } from '@/shared/lib/format'
import { InlineAlert } from '@/shared/suivi'
import {
  useChangerEtat,
  useConfirmerReception,
  useDemanderReforme,
  useLibererMateriel,
} from '../../hooks/useMaterielActions'
import { affectationEnAttente, affectationEnCours } from '../../lib/permissions'
import { MotifDialog } from './MotifDialog'

export type LifecycleDialog = 'reception' | 'liberer' | 'etat' | 'reforme'

/** Dialogues du cycle de vie : reception, liberation, changement d'etat, demande de reforme. */
export function LifecycleDialogs({
  materiel,
  open,
  onClose,
}: {
  materiel: MaterielDetail
  open: LifecycleDialog | null
  onClose: () => void
}) {
  const reception = useConfirmerReception(materiel.id)
  const liberer = useLibererMateriel(materiel.id)
  const changerEtat = useChangerEtat(materiel.id)
  const reforme = useDemanderReforme(materiel.id)
  const enAttente = affectationEnAttente(materiel)
  const enCours = affectationEnCours(materiel) ?? enAttente
  const cible = materiel.etat === 'EN_MAINTENANCE' ? 'DISPONIBLE' : 'EN_MAINTENANCE'
  const onOpenChange = (next: boolean) => {
    if (!next) onClose()
  }

  return (
    <>
      <MotifDialog
        open={open === 'reception'}
        onOpenChange={onOpenChange}
        title="Confirmer la réception"
        description={
          enAttente
            ? `Arrivée sur « ${enAttente.chantierNom} » — affectation du ${formatDate(enAttente.dateDebut)} au ${formatDate(enAttente.dateFinPrevue)}.`
            : undefined
        }
        label="Observations à la réception"
        placeholder="État constaté, accessoires, documents remis…"
        confirmLabel="Confirmer la réception"
        onSubmit={(observations) =>
          reception.mutateAsync({ affectationId: enAttente?.id, observations: observations || undefined })
        }
      />
      <MotifDialog
        open={open === 'liberer'}
        onOpenChange={onOpenChange}
        title="Libérer le matériel"
        description={
          enCours
            ? `Fin de l'affectation sur « ${enCours.chantierNom} » à la date du jour ; le matériel redevient disponible.`
            : 'Le matériel redevient disponible.'
        }
        label="Commentaire"
        confirmLabel="Libérer"
        onSubmit={(commentaire) =>
          liberer.mutateAsync({ dateFinReelle: toIsoDate(new Date()), commentaire: commentaire || undefined })
        }
      />
      <MotifDialog
        open={open === 'etat'}
        onOpenChange={onOpenChange}
        title={cible === 'DISPONIBLE' ? 'Remettre en service' : 'Mettre en maintenance'}
        description={`Nouvel état : « ${labelOf('etatMateriel', cible)} ».`}
        label="Motif"
        minLength={5}
        placeholder={
          cible === 'DISPONIBLE' ? 'Intervention terminée, essais concluants…' : 'Contrôle, réparation…'
        }
        confirmLabel="Changer l'état"
        onSubmit={(motif) => changerEtat.mutateAsync({ etat: cible, motif })}
      />
      <MotifDialog
        open={open === 'reforme'}
        onOpenChange={onOpenChange}
        title="Demander la réforme"
        description="La demande sera soumise à la Direction Générale pour décision."
        label="Motif de la réforme"
        minLength={10}
        placeholder="Vétusté, coût de réparation supérieur à la valeur, sinistre…"
        confirmLabel="Soumettre la demande"
        tone="danger"
        onSubmit={(motif) => reforme.mutateAsync(motif)}
      >
        <InlineAlert tone="warning" title="Retrait définitif du parc si la réforme est approuvée">
          Le matériel ne pourra plus être affecté. Un matériel encore affecté doit d&apos;abord être libéré.
        </InlineAlert>
      </MotifDialog>
    </>
  )
}
