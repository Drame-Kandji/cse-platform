import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import type { Facture } from '../api/types'
import { useRejeterFacture } from '../hooks/useFinanceMutations'
import { rejetSchema, type RejetForm } from '../schemas/finance.schemas'

/** Dialogue de rejet motive d'une facture recue. */
export function RejeterFactureDialog({ facture, onClose }: { facture: Facture | null; onClose: () => void }) {
  const { t } = useTranslation('finance')
  const { t: tc } = useTranslation()
  const rejeter = useRejeterFacture()
  const form = useForm<RejetForm>({ resolver: zodResolver(rejetSchema), defaultValues: { motif: '' } })
  useEffect(() => {
    form.reset({ motif: '' })
  }, [facture, form])

  const submit = form.handleSubmit(async ({ motif }) => {
    if (!facture) return
    await rejeter.mutateAsync({ id: facture.id, motif })
    onClose()
  })

  return (
    <Dialog
      open={facture !== null}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
    >
      <DialogContent
        title={t('factures.rejeterTitle', { numero: facture?.numero ?? '' })}
        description={t('factures.rejeterDescription')}
      >
        <form onSubmit={(event) => void submit(event)} className="grid gap-4" noValidate>
          <FormField control={form.control} name="motif" label={t('factures.motif')} required>
            {({ field, control }) => <Textarea {...control} {...field} rows={3} />}
          </FormField>
          <DialogFooter>
            <Button variant="secondary" onClick={onClose} disabled={rejeter.isPending}>
              {tc('actions.cancel')}
            </Button>
            <Button type="submit" variant="danger" loading={rejeter.isPending}>
              {t('factures.rejeter')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
