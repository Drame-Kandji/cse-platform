import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { optionsOf } from '@/shared/domain/labels'
import type { Materiel, Panne } from '@/shared/domain/types'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import { RadioGroup } from '@/shared/ui/radio-group'
import { useSignalerPanne } from './hooks'
import { panneSchema, type PanneFormInput, type PanneFormOutput } from './schemas'

const GRAVITE_HINTS = {
  MINEURE: 'Reste utilisable',
  MAJEURE: 'Usage dégradé',
  BLOQUANTE: 'Immobilisé',
} as const

export interface SignalerPanneDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  materiel: Pick<Materiel, 'id' | 'code' | 'designation'>
  onSignaled?: (panne: Panne) => void
}

/** Dialogue "Signaler une panne" (CHEF sur son chantier, RM) : gravite + description. */
export function SignalerPanneDialog({ open, onOpenChange, materiel, onSignaled }: SignalerPanneDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title="Signaler une panne" description={`${materiel.code} · ${materiel.designation}`}>
        {open && (
          <PanneForm
            materielId={materiel.id}
            onSignaled={onSignaled}
            onDone={() => {
              onOpenChange(false)
            }}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function PanneForm({
  materielId,
  onSignaled,
  onDone,
}: {
  materielId: string
  onSignaled?: (panne: Panne) => void
  onDone: () => void
}) {
  const signaler = useSignalerPanne()
  const form = useForm<PanneFormInput, unknown, PanneFormOutput>({
    resolver: zodResolver(panneSchema),
    defaultValues: { gravite: 'MINEURE', description: '' },
  })
  const options = optionsOf('gravitePanne').map((option) => ({
    ...option,
    description: GRAVITE_HINTS[option.value],
  }))
  const submit = form.handleSubmit(async (values) => {
    const panne = await signaler.mutateAsync({ materielId, ...values })
    onSignaled?.(panne)
    onDone()
  })
  return (
    <form onSubmit={(event) => void submit(event).catch(() => undefined)} noValidate className="grid gap-4">
      <FormField control={form.control} name="gravite" label="Gravité" required>
        {({ field, control }) => (
          <RadioGroup
            aria-label="Gravité"
            aria-describedby={control['aria-describedby']}
            aria-invalid={control['aria-invalid']}
            orientation="horizontal"
            variant="card"
            options={options}
            value={field.value}
            onValueChange={field.onChange}
          />
        )}
      </FormField>
      <FormField control={form.control} name="description" label="Description" required>
        {({ field, control }) => (
          <Textarea
            {...control}
            {...field}
            rows={4}
            placeholder="Symptômes constatés…"
          />
        )}
      </FormField>
      <DialogFooter>
        <Button variant="secondary" onClick={onDone} disabled={signaler.isPending}>
          Annuler
        </Button>
        <Button type="submit" loading={signaler.isPending}>
          Signaler la panne
        </Button>
      </DialogFooter>
    </form>
  )
}
