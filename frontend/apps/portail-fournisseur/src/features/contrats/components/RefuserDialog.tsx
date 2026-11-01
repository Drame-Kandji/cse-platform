import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import { useRefuserContrat } from '@/api/mutations'
import type { Contrat } from '@/domain/types'
import { refusSchema, type RefusForm } from '../schemas/contrat.schema'

/** Refus de signature : motif obligatoire, renvoye au service achats. */
export function RefuserDialog({
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
  const refuser = useRefuserContrat(contrat.id)
  const form = useForm<RefusForm>({ resolver: zodResolver(refusSchema), defaultValues: { motif: '' } })
  const close = (next: boolean) => {
    if (!next) form.reset()
    onOpenChange(next)
  }
  const submit = form.handleSubmit(async ({ motif }) => {
    await refuser.mutateAsync(motif)
    close(false)
  })
  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent title={t('contrats.refuser.title')} description={contrat.numero}>
        <form
          onSubmit={(event) => void submit(event).catch(() => undefined)}
          noValidate
          className="grid gap-4"
        >
          <FormField control={form.control} name="motif" label={t('contrats.refuser.motif')} required>
            {({ field, control }) => <Textarea {...control} {...field} rows={4} />}
          </FormField>
          <DialogFooter>
            <Button
              variant="secondary"
              onClick={() => {
                close(false)
              }}
              disabled={refuser.isPending}
            >
              {tc('actions.cancel')}
            </Button>
            <Button type="submit" variant="danger" loading={refuser.isPending}>
              {t('contrats.refuser.confirm')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
