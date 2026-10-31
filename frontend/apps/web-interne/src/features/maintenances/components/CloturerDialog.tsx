import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import type { z } from 'zod'
import { toast } from 'sonner'
import { getErrorMessage } from '@/shared/api'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input, Textarea } from '@/shared/ui/input'
import type { Maintenance } from '../api/types'
import { useCloturerMaintenance } from '../hooks/useMaintenances'
import { cloturerSchema } from '../schemas/maintenance.schema'

const DEFAULTS = { cout: '', heuresCompteur: '', observations: '' }

interface Props {
  maintenance: Pick<Maintenance, 'id' | 'numeroOt' | 'materielCode'> | null
  onClose: () => void
}

/** Cloture d'un ordre de travail EN_COURS : cout (obligatoire), compteur et observations. */
export function CloturerDialog({ maintenance, onClose }: Props) {
  const { t } = useTranslation('maintenances')
  const cloturer = useCloturerMaintenance()
  const form = useForm<z.input<typeof cloturerSchema>, unknown, z.output<typeof cloturerSchema>>({
    resolver: zodResolver(cloturerSchema),
    defaultValues: DEFAULTS,
  })
  useEffect(() => {
    if (maintenance) form.reset(DEFAULTS)
  }, [maintenance, form])

  const submit = form.handleSubmit(async (values) => {
    if (!maintenance) return
    try {
      await cloturer.mutateAsync({
        id: maintenance.id,
        cout: values.cout,
        heuresCompteur: values.heuresCompteur,
        observations: values.observations || undefined,
      })
      onClose()
    } catch (error) {
      if (!applyServerErrors(error, form.setError)) toast.error(getErrorMessage(error))
    }
  })
  const pending = form.formState.isSubmitting

  return (
    <Dialog open={maintenance !== null} onOpenChange={(open) => !open && !pending && onClose()}>
      <DialogContent
        title={t('cloturer.title', { numero: maintenance?.numeroOt ?? '' })}
        description={t('cloturer.description', { materiel: maintenance?.materielCode ?? '' })}
      >
        <form id="cloturer-form" onSubmit={(event) => void submit(event)} noValidate className="grid gap-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField control={form.control} name="cout" label={t('cloturer.cout')} required>
              {({ field, control }) => (
                <Input
                  {...control}
                  {...field}
                  type="number"
                  inputMode="numeric"
                  min={0}
                  className="tabular-nums"
                />
              )}
            </FormField>
            <FormField
              control={form.control}
              name="heuresCompteur"
              label={t('cloturer.heuresCompteur')}
              optional
            >
              {({ field, control }) => (
                <Input
                  {...control}
                  {...field}
                  type="number"
                  inputMode="decimal"
                  min={0}
                  className="tabular-nums"
                />
              )}
            </FormField>
          </div>
          <FormField control={form.control} name="observations" label={t('cloturer.observations')} optional>
            {({ field, control }) => (
              <Textarea
                {...control}
                {...field}
                rows={3}
                placeholder={t('cloturer.observationsPlaceholder')}
              />
            )}
          </FormField>
        </form>
        <DialogFooter>
          <Button variant="secondary" disabled={pending} onClick={onClose}>
            {t('cloturer.cancel')}
          </Button>
          <Button type="submit" form="cloturer-form" loading={pending}>
            {t('cloturer.submit')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
