import { useState } from 'react'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { Field } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import type { DecisionApprobation } from '../api/types'

interface Props {
  /** Decision a confirmer (null = ferme). */
  approuve: boolean | null
  titre: string
  onClose: () => void
  onSubmit: (decision: DecisionApprobation) => Promise<unknown>
  pending: boolean
}

/** Confirmation d'une decision d'approbation ; le commentaire est obligatoire en cas de refus. */
export function DecisionDialog({ approuve, titre, onClose, onSubmit, pending }: Props) {
  const [commentaire, setCommentaire] = useState('')
  const [erreur, setErreur] = useState<string>()
  const fermer = () => {
    setCommentaire('')
    setErreur(undefined)
    onClose()
  }
  const valider = async () => {
    const texte = commentaire.trim()
    if (approuve === false && texte.length < 5) {
      setErreur('Motivez le refus (5 caractères minimum).')
      return
    }
    try {
      await onSubmit({ approuve: approuve === true, commentaire: texte || undefined })
      fermer()
    } catch {
      /* erreur notifiee par le toast global */
    }
  }
  return (
    <Dialog open={approuve !== null} onOpenChange={(open) => !open && fermer()}>
      <DialogContent
        title={approuve ? `Approuver · ${titre}` : `Refuser · ${titre}`}
        description={
          approuve
            ? 'Décision tracée.'
            : "Le refus annule la proposition d'attribution."
        }
      >
        <Field label="Commentaire" required={approuve === false} optional={approuve === true} error={erreur}>
          {(control) => (
            <Textarea
              {...control}
              rows={3}
              value={commentaire}
              onChange={(event) => {
                setCommentaire(event.target.value)
                if (erreur) setErreur(undefined)
              }}
            />
          )}
        </Field>
        <DialogFooter>
          <Button variant="secondary" onClick={fermer} disabled={pending}>
            Annuler
          </Button>
          <Button variant={approuve ? 'primary' : 'danger'} loading={pending} onClick={() => void valider()}>
            {approuve ? 'Confirmer l’approbation' : 'Confirmer le refus'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
