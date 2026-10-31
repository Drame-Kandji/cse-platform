import { useState } from 'react'
import { getErrorMessage } from '@/shared/api/errors'
import { applyServerErrors } from '@/shared/lib/forms'
import { formatNumber } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { Field } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { useReleverHeures } from '../../hooks/useMaterielActions'
import { normaliserEntier, validerReleve } from '../../lib/compteurs'

/** Releve du compteur horaire (CHEF sur son chantier, RM). */
export function HeuresDialog({
  open,
  onOpenChange,
  materielId,
  compteurActuel,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  materielId: string
  compteurActuel: number
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        size="sm"
        title="Relevé d'heures"
        description={`Compteur actuel : ${formatNumber(compteurActuel)} h`}
      >
        {open && (
          <HeuresForm
            materielId={materielId}
            compteurActuel={compteurActuel}
            onClose={() => {
              onOpenChange(false)
            }}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function HeuresForm({
  materielId,
  compteurActuel,
  onClose,
}: {
  materielId: string
  compteurActuel: number
  onClose: () => void
}) {
  const [saisie, setSaisie] = useState(String(compteurActuel))
  const [error, setError] = useState<string | null>(null)
  const relever = useReleverHeures(materielId)
  const submit = async () => {
    const invalid = validerReleve(saisie, compteurActuel)
    if (invalid) {
      setError(invalid)
      return
    }
    try {
      await relever.mutateAsync(Number(normaliserEntier(saisie)))
      onClose()
    } catch (cause) {
      const applied = applyServerErrors<{ heuresFonctionnement: string }>(cause, (_name, value) => {
        setError(value.message ?? null)
      })
      if (!applied) setError(getErrorMessage(cause))
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
      <Field label="Nouveau compteur" required error={error ?? undefined}>
        {(control) => (
          <Input
            {...control}
            inputMode="numeric"
            value={saisie}
            onChange={(event) => {
              setSaisie(event.target.value)
              setError(null)
            }}
            rightSlot={<span className="px-1.5 text-xs text-muted-foreground">h</span>}
          />
        )}
      </Field>
      <DialogFooter>
        <Button variant="secondary" onClick={onClose} disabled={relever.isPending}>
          Annuler
        </Button>
        <Button type="submit" loading={relever.isPending}>
          Enregistrer le relevé
        </Button>
      </DialogFooter>
    </form>
  )
}
