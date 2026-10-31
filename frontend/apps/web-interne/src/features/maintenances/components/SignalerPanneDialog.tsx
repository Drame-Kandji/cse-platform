import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import type { z } from 'zod'
import { toast } from 'sonner'
import { getErrorMessage, isApiError } from '@/shared/api'
import { optionsOf } from '@/shared/domain/labels'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import { RadioGroup } from '@/shared/ui/radio-group'
import { useSignalerPanne } from '../hooks/useMaintenances'
import { panneSchema } from '../schemas/maintenance.schema'
import { MaterielPicker } from './MaterielPicker'

const DEFAULTS = { materielId: '', gravite: 'MAJEURE', description: '' } as const

/** Signalement d'une panne par un chef de chantier ou le RM. */
export function SignalerPanneDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { t } = useTranslation('maintenances')
  const signaler = useSignalerPanne()
  const form = useForm<z.input<typeof panneSchema>, unknown, z.output<typeof panneSchema>>({
    resolver: zodResolver(panneSchema),
    defaultValues: DEFAULTS,
  })
  useEffect(() => {
    if (open) form.reset(DEFAULTS)
  }, [open, form])

  const graviteOptions = optionsOf('gravitePanne').map((option) => ({
    ...option,
    description:
      option.value === 'MINEURE'
        ? t('signaler.graviteMineure')
        : option.value === 'MAJEURE'
          ? t('signaler.graviteMajeure')
          : t('signaler.graviteBloquante'),
  }))

  const submit = form.handleSubmit(async (values) => {
    try {
      await signaler.mutateAsync(values)
      onOpenChange(false)
    } catch (error) {
      if (applyServerErrors(error, form.setError)) return
      if (isApiError(error) && error.code === 'CHANTIER_NON_AUTORISE') {
        form.setError('materielId', { type: 'server', message: error.userMessage }, { shouldFocus: true })
        return
      }
      toast.error(getErrorMessage(error))
    }
  })
  const pending = form.formState.isSubmitting

  return (
    <Dialog open={open} onOpenChange={(next) => !pending && onOpenChange(next)}>
      <DialogContent size="lg" title={t('signaler.title')} description={t('signaler.description')}>
        <form id="panne-form" onSubmit={(event) => void submit(event)} noValidate className="grid gap-4">
          <FormField control={form.control} name="materielId" label={t('signaler.materiel')} required>
            {({ field, control }) => (
              <MaterielPicker
                {...control}
                value={field.value}
                onChange={field.onChange}
                placeholder={t('planifier.materielPlaceholder')}
              />
            )}
          </FormField>
          <FormField control={form.control} name="gravite" label={t('signaler.gravite')} required>
            {({ field, control }) => (
              <RadioGroup
                {...control}
                variant="card"
                orientation="horizontal"
                options={graviteOptions}
                value={field.value}
                onValueChange={field.onChange}
              />
            )}
          </FormField>
          <FormField
            control={form.control}
            name="description"
            label={t('signaler.descriptionChamp')}
            required
          >
            {({ field, control }) => (
              <Textarea {...control} {...field} rows={4} placeholder={t('signaler.descriptionPlaceholder')} />
            )}
          </FormField>
        </form>
        <DialogFooter>
          <Button
            variant="secondary"
            disabled={pending}
            onClick={() => {
              onOpenChange(false)
            }}
          >
            {t('cloturer.cancel')}
          </Button>
          <Button type="submit" form="panne-form" variant="danger" loading={pending}>
            {t('signaler.submit')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
