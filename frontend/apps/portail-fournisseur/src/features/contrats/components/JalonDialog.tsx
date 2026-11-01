import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { toIsoDate } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { DatePicker } from '@/shared/ui/date-picker'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { useJalonAtteint } from '@/api/mutations'
import type { Jalon } from '@/domain/types'
import { jalonSchema, type JalonInput, type JalonOutput } from '../schemas/contrat.schema'

/** Jalon atteint : date reelle (aujourd'hui par defaut). */
export function JalonDialog({
  contratId,
  jalon,
  onClose,
}: {
  contratId: string
  jalon: Jalon | null
  onClose: () => void
}) {
  const { t } = useTranslation('portail')
  const { t: tc } = useTranslation()
  const atteint = useJalonAtteint(contratId)
  const form = useForm<JalonInput, unknown, JalonOutput>({
    resolver: zodResolver(jalonSchema),
    defaultValues: { dateReelle: new Date() },
  })
  const close = () => {
    form.reset({ dateReelle: new Date() })
    onClose()
  }
  const submit = form.handleSubmit(async (values) => {
    if (!jalon) return
    await atteint.mutateAsync({ jalonId: jalon.id, dateReelle: toIsoDate(values.dateReelle) ?? '' })
    close()
  })
  return (
    <Dialog
      open={jalon !== null}
      onOpenChange={(next) => {
        if (!next) close()
      }}
    >
      <DialogContent title={t('contrats.jalon.title')} description={jalon?.libelle} size="sm">
        <form
          onSubmit={(event) => void submit(event).catch(() => undefined)}
          noValidate
          className="grid gap-4"
        >
          <FormField control={form.control} name="dateReelle" label={t('contrats.jalon.date')} required>
            {({ field, control }) => (
              <DatePicker {...control} value={field.value} onChange={field.onChange} max={new Date()} />
            )}
          </FormField>
          <DialogFooter>
            <Button variant="secondary" onClick={close} disabled={atteint.isPending}>
              {tc('actions.cancel')}
            </Button>
            <Button type="submit" loading={atteint.isPending}>
              {tc('actions.save')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
