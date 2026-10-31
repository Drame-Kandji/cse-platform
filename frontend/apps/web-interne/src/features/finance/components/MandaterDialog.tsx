import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { formatMoney } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import type { Facture } from '../api/types'
import { useMandaterFacture } from '../hooks/useFinanceMutations'
import { mandatSchema, type MandatForm } from '../schemas/finance.schemas'

/** Dialogue de mandatement d'une facture recue (reference de mandat obligatoire). */
export function MandaterDialog({ facture, onClose }: { facture: Facture | null; onClose: () => void }) {
  const { t } = useTranslation('finance')
  const { t: tc } = useTranslation()
  const mandater = useMandaterFacture()
  const form = useForm<MandatForm>({
    resolver: zodResolver(mandatSchema),
    defaultValues: { referenceMandat: '' },
  })
  useEffect(() => {
    form.reset({ referenceMandat: '' })
  }, [facture, form])

  const submit = form.handleSubmit(async ({ referenceMandat }) => {
    if (!facture) return
    await mandater.mutateAsync({ id: facture.id, referenceMandat })
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
        title={t('factures.mandaterTitle', { numero: facture?.numero ?? '' })}
        description={t('factures.mandaterDescription', { montant: formatMoney(facture?.montantTtc) })}
      >
        <form onSubmit={(event) => void submit(event)} className="grid gap-4" noValidate>
          <FormField
            control={form.control}
            name="referenceMandat"
            label={t('factures.referenceMandat')}
            required
          >
            {({ field, control }) => <Input {...control} {...field} placeholder="MDT-2026-0001" />}
          </FormField>
          <DialogFooter>
            <Button variant="secondary" onClick={onClose} disabled={mandater.isPending}>
              {tc('actions.cancel')}
            </Button>
            <Button type="submit" loading={mandater.isPending}>
              {t('factures.mandater')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
