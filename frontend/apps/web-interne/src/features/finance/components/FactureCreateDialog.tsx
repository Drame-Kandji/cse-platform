import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { applyServerErrors } from '@/shared/lib/forms'
import { formatMoney, toIsoDate } from '@/shared/lib/format'
import { getErrorMessage, isApiError } from '@/shared/api'
import { toast } from 'sonner'
import { Button } from '@/shared/ui/button'
import { Combobox } from '@/shared/ui/combobox'
import { DatePicker } from '@/shared/ui/date-picker'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { useContratsFacturables } from '../hooks/useFinance'
import { useCreerFacture } from '../hooks/useFinanceMutations'
import { factureSchema, type FactureFormInput, type FactureFormOutput } from '../schemas/finance.schemas'

const TAUX_TVA = 0.18

/** Codes metier ms-achats rattaches a un champ du formulaire. */
const CHAMP_PAR_CODE: Partial<Record<string, 'montantHt' | 'numero' | 'contratId'>> = {
  DEPASSEMENT_MONTANT_CONTRAT: 'montantHt',
  FACTURE_EXISTANTE: 'numero',
  CONTRAT_NON_FACTURABLE: 'contratId',
}
const DEFAULTS: FactureFormInput = {
  contratId: '',
  numero: '',
  montantHt: '',
  tva: '',
  dateEmission: null,
  dateEcheance: null,
}

/** Saisie d'une facture fournisseur rattachee a un contrat (TTC calcule en direct). */
export function FactureCreateDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { t } = useTranslation('finance')
  const { t: tc } = useTranslation()
  const contrats = useContratsFacturables(open)
  const creer = useCreerFacture()
  const form = useForm<FactureFormInput, unknown, FactureFormOutput>({
    resolver: zodResolver(factureSchema),
    defaultValues: DEFAULTS,
  })
  const [ht, tva] = useWatch({ control: form.control, name: ['montantHt', 'tva'] })
  const ttc = (Number(ht) || 0) + (Number(tva) || 0)
  const options = (contrats.data?.content ?? []).map((c) => ({
    value: c.id,
    label: `${c.numero} · ${c.fournisseurNom}`,
    description: `${c.objet} · ${formatMoney(c.montant)}`,
  }))

  const close = (next: boolean) => {
    if (!next) form.reset(DEFAULTS)
    onOpenChange(next)
  }

  const submit = form.handleSubmit(async (values) => {
    try {
      await creer.mutateAsync({
        contratId: values.contratId,
        numero: values.numero,
        montantHt: values.montantHt,
        tva: values.tva,
        dateEmission: toIsoDate(values.dateEmission) ?? '',
        dateEcheance: toIsoDate(values.dateEcheance) ?? '',
      })
      close(false)
    } catch (error) {
      if (applyServerErrors(error, form.setError)) return
      const champ = isApiError(error) && error.code ? CHAMP_PAR_CODE[error.code] : undefined
      if (champ)
        form.setError(champ, { type: 'server', message: getErrorMessage(error) }, { shouldFocus: true })
      else toast.error(getErrorMessage(error))
    }
  })

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent title={t('form.title')} description={t('form.description')} size="lg">
        <form onSubmit={(event) => void submit(event)} className="grid gap-4 sm:grid-cols-2" noValidate>
          <FormField
            control={form.control}
            name="contratId"
            label={t('form.contrat')}
            required
            className="sm:col-span-2"
          >
            {({ field, control }) => (
              <Combobox
                {...control}
                options={options}
                loading={contrats.isFetching}
                value={field.value || null}
                onValueChange={(value) => {
                  field.onChange(value ?? '')
                }}
                placeholder={t('form.contratPlaceholder')}
              />
            )}
          </FormField>
          <FormField
            control={form.control}
            name="numero"
            label={t('form.numero')}
            required
            className="sm:col-span-2"
          >
            {({ field, control }) => <Input {...control} {...field} placeholder="FAC-2026-0001" />}
          </FormField>
          <FormField control={form.control} name="montantHt" label={t('form.montantHt')} required>
            {({ field, control }) => <Input {...control} {...field} inputMode="numeric" />}
          </FormField>
          <FormField
            control={form.control}
            name="tva"
            label={t('form.tva')}
            required
            labelAction={
              <button
                type="button"
                className="text-xs font-medium text-primary-emphasis hover:underline"
                onClick={() => {
                  form.setValue('tva', String(Math.round((Number(ht) || 0) * TAUX_TVA)), {
                    shouldValidate: true,
                  })
                }}
              >
                {t('form.tvaAuto')}
              </button>
            }
          >
            {({ field, control }) => <Input {...control} {...field} inputMode="numeric" />}
          </FormField>
          <FormField control={form.control} name="dateEmission" label={t('form.dateEmission')} required>
            {({ field, control }) => (
              <DatePicker {...control} value={field.value} onChange={field.onChange} />
            )}
          </FormField>
          <FormField control={form.control} name="dateEcheance" label={t('form.dateEcheance')} required>
            {({ field, control }) => (
              <DatePicker {...control} value={field.value} onChange={field.onChange} />
            )}
          </FormField>
          <p className="flex items-center justify-between rounded-lg border border-border bg-surface-sunken px-3 py-2 text-sm sm:col-span-2">
            <span className="text-muted-foreground">{t('form.ttc')}</span>
            <span className="font-semibold tabular-nums" aria-live="polite">
              {formatMoney(ttc)}
            </span>
          </p>
          <DialogFooter className="sm:col-span-2">
            <Button
              variant="secondary"
              onClick={() => {
                close(false)
              }}
              disabled={creer.isPending}
            >
              {tc('actions.cancel')}
            </Button>
            <Button type="submit" loading={creer.isPending}>
              {tc('actions.save')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
