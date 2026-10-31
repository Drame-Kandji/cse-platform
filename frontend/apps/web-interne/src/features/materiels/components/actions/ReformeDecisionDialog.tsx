import { useState } from 'react'
import { toast } from 'sonner'
import { InlineAlert } from '@/shared/suivi'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { Field } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import { useValiderReforme } from '../../hooks/useMaterielActions'

/** Decision de la Direction Generale sur une demande de reforme (R13). */
export function ReformeDecisionDialog({
  open,
  onOpenChange,
  materielId,
  motif,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  materielId: string
  motif: string | null | undefined
}) {
  const valider = useValiderReforme(materielId)
  const [commentaire, setCommentaire] = useState('')
  const [decision, setDecision] = useState<boolean | null>(null)
  const close = () => {
    setCommentaire('')
    onOpenChange(false)
  }
  const decide = async (approuve: boolean) => {
    setDecision(approuve)
    try {
      await valider.mutateAsync({ approuve, commentaire: commentaire.trim() || undefined })
      toast.success(
        approuve ? 'Réforme approuvée : le matériel est retiré du parc' : 'Demande de réforme refusée',
      )
      close()
    } catch {
      // Erreur notifiee par le toast global des mutations.
    }
  }
  return (
    <Dialog open={open} onOpenChange={(next) => (next ? onOpenChange(true) : close())}>
      <DialogContent title="Décision sur la réforme" description="Demande du Responsable Matériel.">
        <div className="grid gap-4">
          <div className="rounded-lg border border-border bg-surface-sunken/60 px-3.5 py-3 text-sm">
            <p className="text-xs font-medium text-muted-foreground">Motif de la demande</p>
            <p className="mt-1 whitespace-pre-line">{motif ?? 'Aucun motif renseigné.'}</p>
          </div>
          <InlineAlert tone="warning" title="Décision définitive">
            Un matériel réformé ne peut plus être affecté ni remis en service.
          </InlineAlert>
          <Field label="Commentaire" optional>
            {(control) => (
              <Textarea
                {...control}
                rows={3}
                value={commentaire}
                onChange={(event) => {
                  setCommentaire(event.target.value)
                }}
              />
            )}
          </Field>
          <DialogFooter>
            <Button variant="secondary" onClick={close} disabled={valider.isPending}>
              Annuler
            </Button>
            <Button
              variant="secondary"
              loading={valider.isPending && decision === false}
              disabled={valider.isPending}
              onClick={() => void decide(false)}
            >
              Refuser
            </Button>
            <Button
              variant="danger"
              loading={valider.isPending && decision === true}
              disabled={valider.isPending}
              onClick={() => void decide(true)}
            >
              Approuver la réforme
            </Button>
          </DialogFooter>
        </div>
      </DialogContent>
    </Dialog>
  )
}
