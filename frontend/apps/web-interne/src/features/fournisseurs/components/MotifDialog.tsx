import { useState, type ReactNode } from 'react'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogTrigger } from '@/shared/ui/dialog'
import { Field } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import { motifSchema } from '../schemas/fournisseur.schema'

interface Props {
  title: string
  description: ReactNode
  confirmLabel: string
  /** Libelle du champ (Motif, Commentaire). */
  label?: string
  /** Faux : texte facultatif (commentaire de decision). */
  required?: boolean
  tone?: 'primary' | 'danger'
  trigger: ReactNode
  onConfirm: (texte: string) => Promise<unknown>
}

/** Confirmation d'une action de statut exigeant un motif (ou un commentaire facultatif). */
export function MotifDialog({
  title,
  description,
  confirmLabel,
  label = 'Motif',
  required = true,
  tone = 'primary',
  trigger,
  onConfirm,
}: Props) {
  const [open, setOpen] = useState(false)
  const [texte, setTexte] = useState('')
  const [error, setError] = useState<string>()
  const [pending, setPending] = useState(false)

  const confirm = async () => {
    const parsed = motifSchema.safeParse(texte)
    if (required && !parsed.success) {
      setError(parsed.error.issues[0]?.message)
      return
    }
    setPending(true)
    try {
      await onConfirm(texte.trim())
      setOpen(false)
      setTexte('')
    } catch {
      // erreur deja notifiee par le toast global des mutations
    } finally {
      setPending(false)
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (pending) return
        setOpen(next)
        setError(undefined)
      }}
    >
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent title={title} description={description}>
        <Field label={label} required={required} optional={!required} error={error}>
          {(control) => (
            <Textarea
              {...control}
              value={texte}
              onChange={(event) => {
                setTexte(event.target.value)
                setError(undefined)
              }}
            />
          )}
        </Field>
        <DialogFooter>
          <Button
            variant="secondary"
            disabled={pending}
            onClick={() => {
              setOpen(false)
            }}
          >
            Annuler
          </Button>
          <Button
            variant={tone === 'danger' ? 'danger' : 'primary'}
            loading={pending}
            onClick={() => void confirm()}
          >
            {confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
