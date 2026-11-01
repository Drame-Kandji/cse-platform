import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { toIsoDate } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { DatePicker } from '@/shared/ui/date-picker'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import { useExpedierContrat } from '@/api/mutations'
import type { Contrat } from '@/domain/types'
import { expeditionSchema, type ExpeditionInput, type ExpeditionOutput } from '../schemas/contrat.schema'

/** Declaration d'expedition : date (aujourd'hui par defaut) et commentaire. */
export function ExpedierDialog({
  contrat,
  open,
  onOpenChange,
}: {
  contrat: Contrat
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { t } = useTranslation('portail')
  const { t: tc } = useTranslation()
  const expedier = useExpedierContrat(contrat.id)
  const form = useForm<ExpeditionInput, unknown, ExpeditionOutput>({
    resolver: zodResolver(expeditionSchema),
    defaultValues: { dateExpedition: new Date(), commentaire: '' },
  })
  const close = (next: boolean) => {
    if (!next) form.reset()
    onOpenChange(next)
  }
  const submit = form.handleSubmit(async (values) => {
    await expedier.mutateAsync({
      dateExpedition: toIsoDate(values.dateExpedition) ?? '',
      commentaire: values.commentaire,
    })
    close(false)
  })
  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent title={t('contrats.expedier.title')} description={contrat.numero}>
        <form
          onSubmit={(event) => void submit(event).catch(() => undefined)}
          noValidate
          className="grid gap-4"
        >
          <FormField
            control={form.control}
            name="dateExpedition"
            label={t('contrats.expedier.date')}
            required
          >
            {({ field, control }) => (
              <DatePicker {...control} value={field.value} onChange={field.onChange} max={new Date()} />
            )}
          </FormField>
          <FormField
            control={form.control}
            name="commentaire"
            label={t('contrats.expedier.commentaire')}
            optional
          >
            {({ field, control }) => <Textarea {...control} {...field} rows={3} />}
          </FormField>
          <DialogFooter>
            <Button
              variant="secondary"
              onClick={() => {
                close(false)
              }}
              disabled={expedier.isPending}
            >
              {tc('actions.cancel')}
            </Button>
            <Button type="submit" loading={expedier.isPending}>
              {t('contrats.expedier.confirm')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
