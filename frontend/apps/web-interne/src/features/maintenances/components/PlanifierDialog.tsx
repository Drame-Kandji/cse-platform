import { zodResolver } from '@hookform/resolvers/zod'
import { startOfDay } from 'date-fns'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import type { z } from 'zod'
import { toast } from 'sonner'
import { getErrorMessage } from '@/shared/api'
import { optionsOf } from '@/shared/domain/labels'
import { applyServerErrors } from '@/shared/lib/forms'
import { toIsoDate } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { DatePicker } from '@/shared/ui/date-picker'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input, Textarea } from '@/shared/ui/input'
import { RadioGroup } from '@/shared/ui/radio-group'
import type { Panne } from '../api/types'
import { usePlanifierMaintenance } from '../hooks/useMaintenances'
import { planifierSchema } from '../schemas/maintenance.schema'
import { MaterielPicker } from './MaterielPicker'

type FormInput = z.input<typeof planifierSchema>
type Output = z.output<typeof planifierSchema>

function defaults(panne?: Panne | null): FormInput {
  return {
    materielId: panne?.materielId ?? '',
    type: panne ? 'CORRECTIVE' : 'PREVENTIVE',
    datePlanifiee: startOfDay(new Date()),
    technicien: '',
    description: panne ? panne.description : '',
  }
}

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Panne a l'origine de l'OT (pre-remplit materiel, type et description). */
  panne?: Panne | null
}

/** Planification d'un ordre de travail (RM), depuis zero ou depuis une panne. */
export function PlanifierDialog({ open, onOpenChange, panne }: Props) {
  const { t } = useTranslation('maintenances')
  const planifier = usePlanifierMaintenance()
  const form = useForm<FormInput, unknown, Output>({
    resolver: zodResolver(planifierSchema),
    defaultValues: defaults(panne),
  })
  useEffect(() => {
    if (open) form.reset(defaults(panne))
  }, [open, panne, form])

  const submit = form.handleSubmit(async (values) => {
    try {
      await planifier.mutateAsync({
        materielId: values.materielId,
        type: values.type,
        description: values.description,
        datePlanifiee: toIsoDate(values.datePlanifiee) ?? '',
        technicien: values.technicien || undefined,
        panneId: panne?.id,
      })
      onOpenChange(false)
    } catch (error) {
      if (!applyServerErrors(error, form.setError)) toast.error(getErrorMessage(error))
    }
  })
  const pending = form.formState.isSubmitting

  return (
    <Dialog open={open} onOpenChange={(next) => !pending && onOpenChange(next)}>
      <DialogContent
        size="lg"
        title={panne ? t('planifier.titlePanne') : t('planifier.title')}
        description={panne ? panne.materielCode : undefined}
      >
        <form id="planifier-form" onSubmit={(event) => void submit(event)} noValidate className="grid gap-4">
          <FormField control={form.control} name="materielId" label={t('planifier.materiel')} required>
            {({ field, control }) => (
              <MaterielPicker
                {...control}
                value={field.value}
                onChange={field.onChange}
                disabled={Boolean(panne)}
                placeholder={t('planifier.materielPlaceholder')}
                initialOption={
                  panne
                    ? {
                        value: panne.materielId,
                        label: `${panne.materielCode} · ${panne.materielDesignation}`,
                      }
                    : undefined
                }
              />
            )}
          </FormField>
          <FormField control={form.control} name="type" label={t('planifier.type')} required>
            {({ field, control }) => (
              <RadioGroup
                {...control}
                variant="card"
                orientation="horizontal"
                options={optionsOf('typeMaintenance')}
                value={field.value}
                onValueChange={field.onChange}
              />
            )}
          </FormField>
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField
              control={form.control}
              name="datePlanifiee"
              label={t('planifier.datePlanifiee')}
              required
            >
              {({ field, control }) => (
                <DatePicker
                  {...control}
                  value={field.value}
                  onChange={field.onChange}
                  min={startOfDay(new Date())}
                />
              )}
            </FormField>
            <FormField control={form.control} name="technicien" label={t('planifier.technicien')} optional>
              {({ field, control }) => (
                <Input {...control} {...field} placeholder={t('planifier.technicienPlaceholder')} />
              )}
            </FormField>
          </div>
          <FormField
            control={form.control}
            name="description"
            label={t('planifier.descriptionChamp')}
            required
          >
            {({ field, control }) => (
              <Textarea
                {...control}
                {...field}
                rows={3}
                placeholder={t('planifier.descriptionPlaceholder')}
              />
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
          <Button type="submit" form="planifier-form" loading={pending}>
            {t('planifier.submit')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
