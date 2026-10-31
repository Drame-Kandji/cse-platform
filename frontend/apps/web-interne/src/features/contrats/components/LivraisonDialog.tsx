import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { applyServerErrors } from '@/shared/lib/forms'
import { toIsoDate } from '@/shared/lib/format'
import {
  Button,
  DatePicker,
  Dialog,
  DialogContent,
  DialogFooter,
  FormField,
  Input,
  Textarea,
} from '@/shared/ui'
import type { Contrat } from '../api/types'
import { useConfirmerLivraison } from '../hooks/useContratActions'
import { livraisonSchema, type LivraisonFormInput, type LivraisonFormOutput } from '../schemas/contrat.schema'

interface Props {
  contrat: Contrat
  open: boolean
  onOpenChange: (open: boolean) => void
}

const EMPTY: LivraisonFormInput = { dateLivraisonReelle: null, numeroBonLivraison: '', observations: '' }

/** Confirmation de livraison : date de reception, bon de livraison et observations. */
export function LivraisonDialog({ contrat, open, onOpenChange }: Props) {
  const { t } = useTranslation('contrats')
  const mutation = useConfirmerLivraison(contrat.id)
  const form = useForm<LivraisonFormInput, unknown, LivraisonFormOutput>({
    resolver: zodResolver(livraisonSchema),
    defaultValues: EMPTY,
  })

  const submit = form.handleSubmit(async (values) => {
    try {
      await mutation.mutateAsync({
        dateLivraisonReelle: toIsoDate(values.dateLivraisonReelle) ?? '',
        numeroBonLivraison: values.numeroBonLivraison,
        observations: values.observations,
      })
      onOpenChange(false)
    } catch (error) {
      applyServerErrors(error, form.setError)
    }
  })

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (next) form.reset(EMPTY)
        onOpenChange(next)
      }}
    >
      <DialogContent title={t('actions.confirmer')} description={t('actions.confirmerDescription')}>
        <form onSubmit={(e) => void submit(e)} className="grid gap-4" noValidate>
          <FormField
            control={form.control}
            name="dateLivraisonReelle"
            label={t('fields.dateReelle')}
            required
          >
            {({ field, control }) => (
              <DatePicker {...control} value={field.value} onChange={field.onChange} max={new Date()} />
            )}
          </FormField>
          <FormField
            control={form.control}
            name="numeroBonLivraison"
            label={t('fields.bonLivraison')}
            required
          >
            {({ field, control }) => <Input {...control} {...field} placeholder="BL-2026-0001" />}
          </FormField>
          <FormField control={form.control} name="observations" label={t('actions.observations')} optional>
            {({ field, control }) => <Textarea {...control} {...field} rows={3} />}
          </FormField>
          <DialogFooter>
            <Button
              variant="secondary"
              onClick={() => {
                onOpenChange(false)
              }}
            >
              Annuler
            </Button>
            <Button type="submit" loading={mutation.isPending}>
              {t('actions.confirmer')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
