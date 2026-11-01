import { zodResolver } from '@hookform/resolvers/zod'
import { addDays } from 'date-fns'
import { useEffect, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { isApiError } from '@/shared/api/errors'
import { applyServerErrors } from '@/shared/lib/forms'
import { formatMoney, formatNumber, toIsoDate } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { DatePicker } from '@/shared/ui/date-picker'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { Select } from '@/shared/ui/select'
import { useDeposerFacture } from '@/api/mutations'
import { InlineError } from '@/components/InlineError'
import { messageErreur } from '@/domain/errors'
import { calculerTtc, TAUX_TVA_DEFAUT } from '@/domain/factures'
import type { Contrat } from '@/domain/types'
import { factureSchema, type FactureInput, type FactureOutput } from '../schemas/facture.schema'

/** Codes metier rattaches a un champ du formulaire. */
const CHAMP_PAR_CODE: Partial<
  Record<string, 'numero' | 'contratId' | 'montantHt' | 'dateEmission' | 'dateEcheance'>
> = {
  FACTURE_EXISTANTE: 'numero',
  CONTRAT_NON_LIVRE: 'contratId',
  CONTRAT_NON_FACTURABLE: 'contratId',
  DEPASSEMENT_MONTANT_CONTRAT: 'montantHt',
  ECHEANCE_INVALIDE: 'dateEcheance',
  DATE_FUTURE: 'dateEmission',
}

const valeursInitiales = (contratId: string): FactureInput => ({
  contratId,
  numero: '',
  montantHt: '',
  tauxTva: String(TAUX_TVA_DEFAUT),
  dateEmission: new Date(),
  dateEcheance: addDays(new Date(), 30),
})

/** Nouvelle facture : contrat livre, numero, HT, taux de TVA, dates ; TTC calcule en direct. */
export function NouvelleFactureDialog({
  open,
  onOpenChange,
  contrats,
  contratId = '',
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Contrats facturables (LIVRE). */
  contrats: readonly Contrat[]
  contratId?: string
}) {
  const { t } = useTranslation('portail')
  const { t: tc } = useTranslation()
  const [erreur, setErreur] = useState<string | null>(null)
  const deposer = useDeposerFacture()
  const form = useForm<FactureInput, unknown, FactureOutput>({
    resolver: zodResolver(factureSchema),
    defaultValues: valeursInitiales(contratId),
  })
  const { reset } = form
  // Formulaire neuf a chaque ouverture (contrat eventuellement preselectionne).
  useEffect(() => {
    if (open) reset(valeursInitiales(contratId))
  }, [open, contratId, reset])
  const [ht, taux] = useWatch({ control: form.control, name: ['montantHt', 'tauxTva'] })
  const montants = calculerTtc(Number(ht.replace(/\s/g, '')), Number(taux.replace(',', '.')))

  const close = (next: boolean) => {
    if (!next) setErreur(null)
    onOpenChange(next)
  }

  const submit = form.handleSubmit(async (values) => {
    setErreur(null)
    const { tva } = calculerTtc(values.montantHt, values.tauxTva)
    try {
      await deposer.mutateAsync({
        contratId: values.contratId,
        numero: values.numero,
        montantHt: values.montantHt,
        tva,
        dateEmission: toIsoDate(values.dateEmission) ?? '',
        dateEcheance: toIsoDate(values.dateEcheance) ?? '',
      })
      close(false)
    } catch (error) {
      if (applyServerErrors(error, form.setError)) return
      const champ = isApiError(error) && error.code ? CHAMP_PAR_CODE[error.code] : undefined
      if (champ)
        form.setError(champ, { type: 'server', message: messageErreur(error) }, { shouldFocus: true })
      else setErreur(messageErreur(error))
    }
  })

  const options = contrats.map((c) => ({ value: c.id, label: `${c.numero} · ${c.objet}` }))
  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent title={t('factures.dialog.title')} size="lg">
        <form onSubmit={(event) => void submit(event)} noValidate className="grid gap-4 sm:grid-cols-2">
          <InlineError message={erreur} className="sm:col-span-2" />
          <FormField
            control={form.control}
            name="contratId"
            label={t('factures.dialog.contrat')}
            required
            className="sm:col-span-2"
          >
            {({ field, control }) => (
              <Select
                {...control}
                options={options}
                value={field.value || undefined}
                onValueChange={field.onChange}
                placeholder={options.length === 0 ? t('factures.dialog.aucunContrat') : undefined}
                disabled={options.length === 0}
              />
            )}
          </FormField>
          <FormField
            control={form.control}
            name="numero"
            label={t('factures.dialog.numero')}
            required
            className="sm:col-span-2"
          >
            {({ field, control }) => <Input {...control} {...field} placeholder="FA-2026-001" />}
          </FormField>
          <FormField control={form.control} name="montantHt" label={t('factures.dialog.ht')} required>
            {({ field, control }) => (
              <Input
                {...control}
                {...field}
                inputMode="numeric"
                rightSlot={<span className="pr-2 text-xs text-muted-foreground">FCFA</span>}
              />
            )}
          </FormField>
          <FormField control={form.control} name="tauxTva" label={t('factures.dialog.tva')} required>
            {({ field, control }) => (
              <Input
                {...control}
                {...field}
                inputMode="decimal"
                rightSlot={<span className="pr-2 text-xs text-muted-foreground">%</span>}
              />
            )}
          </FormField>
          <FormField
            control={form.control}
            name="dateEmission"
            label={t('factures.dialog.emission')}
            required
          >
            {({ field, control }) => (
              <DatePicker {...control} value={field.value} onChange={field.onChange} />
            )}
          </FormField>
          <FormField
            control={form.control}
            name="dateEcheance"
            label={t('factures.dialog.echeance')}
            required
          >
            {({ field, control }) => (
              <DatePicker {...control} value={field.value} onChange={field.onChange} />
            )}
          </FormField>
          <dl
            className="grid gap-1.5 rounded-lg border border-border bg-surface-sunken px-3 py-2.5 text-sm sm:col-span-2"
            aria-live="polite"
          >
            <div className="flex justify-between gap-4">
              <dt className="text-muted-foreground">
                {t('factures.dialog.montantTva', {
                  taux: formatNumber(Number(taux.replace(',', '.')), { decimals: 2 }),
                })}
              </dt>
              <dd className="tabular-nums">{formatMoney(montants.tva)}</dd>
            </div>
            <div className="flex justify-between gap-4 font-semibold">
              <dt>{t('factures.dialog.ttc')}</dt>
              <dd className="tabular-nums" data-testid="ttc">
                {formatMoney(montants.ttc)}
              </dd>
            </div>
          </dl>
          <DialogFooter className="sm:col-span-2">
            <Button
              variant="secondary"
              onClick={() => {
                close(false)
              }}
              disabled={deposer.isPending}
            >
              {tc('actions.cancel')}
            </Button>
            <Button type="submit" loading={deposer.isPending} disabled={options.length === 0}>
              {t('factures.dialog.submit')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
