import type { MaterielDetail } from '@/shared/domain/types'
import { formatDate, toIsoDate } from '@/shared/lib/format'
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
            ? `${enAttente.chantierNom} · ${formatDate(enAttente.dateDebut)} → ${formatDate(enAttente.dateFinPrevue)}`
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
        description={enCours?.chantierNom}
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
        description="Soumise à la DG. Retrait définitif du parc si approuvée."
        label="Motif de la réforme"
        minLength={10}
        placeholder="Vétusté, coût de réparation supérieur à la valeur, sinistre…"
        confirmLabel="Soumettre la demande"
        tone="danger"
        onSubmit={(motif) => reforme.mutateAsync(motif)}
      />
    </>
  )
}
