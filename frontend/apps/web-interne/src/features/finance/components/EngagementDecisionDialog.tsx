import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { formatMoney } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Textarea } from '@/shared/ui/input'
import type { Engagement } from '../api/types'
import { useDeciderEngagement } from '../hooks/useFinanceMutations'
import { decisionEngagementSchema, type DecisionEngagementForm } from '../schemas/finance.schemas'

interface Props {
  engagement: Engagement | null
  approuve: boolean
  onClose: () => void
}

/** Dialogue de decision DAF sur un engagement (commentaire obligatoire si refus). */
export function EngagementDecisionDialog({ engagement, approuve, onClose }: Props) {
  const { t } = useTranslation('finance')
  const { t: tc } = useTranslation()
  const decider = useDeciderEngagement()
  const form = useForm<DecisionEngagementForm>({
    resolver: zodResolver(decisionEngagementSchema),
    defaultValues: { approuve, commentaire: '' },
  })
  useEffect(() => {
    form.reset({ approuve, commentaire: '' })
  }, [engagement, approuve, form])

  const submit = form.handleSubmit(async (values) => {
    if (!engagement) return
    await decider.mutateAsync({
      contratId: engagement.contratId,
      approuve: values.approuve,
      commentaire: values.commentaire || undefined,
    })
    toast.success(values.approuve ? t('engagements.approved') : t('engagements.refused'))
    onClose()
  })

  const numero = engagement?.contratNumero ?? ''
  return (
    <Dialog
      open={engagement !== null}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
    >
      <DialogContent
        title={
          approuve ? t('engagements.approveTitle', { numero }) : t('engagements.refuseTitle', { numero })
        }
        description={approuve ? t('engagements.approveDescription') : t('engagements.refuseDescription')}
      >
        {engagement && (
          <form onSubmit={(event) => void submit(event)} className="grid gap-4" noValidate>
            <dl className="grid grid-cols-2 gap-3 rounded-lg border border-border bg-surface-sunken p-3 text-sm">
              <div>
                <dt className="text-xs text-muted-foreground">{t('columns.fournisseur')}</dt>
                <dd className="font-medium">{engagement.fournisseurNom}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">{t('columns.montant')}</dt>
                <dd className="font-medium tabular-nums">{formatMoney(engagement.montant)}</dd>
              </div>
              <div className="col-span-2">
                <dt className="text-xs text-muted-foreground">{t('columns.objet')}</dt>
                <dd>{engagement.objet}</dd>
              </div>
            </dl>
            <FormField
              control={form.control}
              name="commentaire"
              label={approuve ? t('engagements.comment') : t('engagements.refuseComment')}
              required={!approuve}
              optional={approuve}
            >
              {({ field, control }) => <Textarea {...control} {...field} rows={3} />}
            </FormField>
            <DialogFooter>
              <Button variant="secondary" onClick={onClose} disabled={decider.isPending}>
                {tc('actions.cancel')}
              </Button>
              <Button type="submit" variant={approuve ? 'primary' : 'danger'} loading={decider.isPending}>
                {approuve ? t('engagements.approve') : t('engagements.refuse')}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  )
}
