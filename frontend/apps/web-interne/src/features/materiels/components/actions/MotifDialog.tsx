import { useState, type ReactNode } from 'react'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { Field } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'

export interface MotifDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: ReactNode
  label: string
  placeholder?: string
  /** Motif obligatoire (longueur minimale) ; 0 = facultatif. */
  minLength?: number
  confirmLabel: string
  tone?: 'primary' | 'danger'
  /** Contenu additionnel au-dessus du champ (rappel, avertissement). */
  children?: ReactNode
  /** Appele avec le texte saisi (vide si facultatif) ; le dialogue se ferme si la promesse aboutit. */
  onSubmit: (texte: string) => Promise<unknown>
}

/** Dialogue de confirmation avec un champ texte (motif, observations, commentaire). */
export function MotifDialog({ open, onOpenChange, title, description, ...props }: MotifDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={title} description={description}>
        {open && (
          <MotifForm
            {...props}
            onClose={() => {
              onOpenChange(false)
            }}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function MotifForm({
  label,
  placeholder,
  minLength = 0,
  confirmLabel,
  tone = 'primary',
  children,
  onSubmit,
  onClose,
}: Omit<MotifDialogProps, 'open' | 'onOpenChange' | 'title' | 'description'> & { onClose: () => void }) {
  const [texte, setTexte] = useState('')
  const [error, setError] = useState<string>()
  const [pending, setPending] = useState(false)
  const submit = async () => {
    const value = texte.trim()
    if (minLength > 0 && value.length < minLength) {
      setError(`Au moins ${String(minLength)} caractères`)
      return
    }
    setPending(true)
    try {
      await onSubmit(value)
      onClose()
    } catch {
      // L'erreur est notifiee par le toast global des mutations ; le dialogue reste ouvert.
    } finally {
      setPending(false)
    }
  }
  return (
    <form
      noValidate
      className="grid gap-4"
      onSubmit={(event) => {
        event.preventDefault()
        void submit()
      }}
    >
      {children}
      <Field label={label} required={minLength > 0} optional={minLength === 0} error={error}>
        {(control) => (
          <Textarea
            {...control}
            value={texte}
            rows={3}
            placeholder={placeholder}
            onChange={(event) => {
              setTexte(event.target.value)
              setError(undefined)
            }}
          />
        )}
      </Field>
      <DialogFooter>
        <Button variant="secondary" onClick={onClose} disabled={pending}>
          Annuler
        </Button>
        <Button type="submit" variant={tone} loading={pending}>
          {confirmLabel}
        </Button>
      </DialogFooter>
    </form>
  )
}
