import { zodResolver } from '@hookform/resolvers/zod'
import { Plus, Trash2 } from 'lucide-react'
import { useFieldArray, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { applyServerErrors } from '@/shared/lib/forms'
import { toDate, toIsoDate } from '@/shared/lib/format'
import {
  Button,
  DatePicker,
  Dialog,
  DialogContent,
  DialogFooter,
  FormField,
  IconButton,
  Input,
  Textarea,
} from '@/shared/ui'
import type { Contrat } from '../api/types'
import { useEnregistrerClauses } from '../hooks/useContratActions'
import { clausesSchema, type ClausesFormInput, type ClausesFormOutput } from '../schemas/contrat.schema'

interface Props {
  contrat: Contrat
  open: boolean
  onOpenChange: (open: boolean) => void
}

function defaults(contrat: Contrat): ClausesFormInput {
  const c = contrat.clauses
  return {
    garantieMois: c.garantieMois ?? '',
    conditionsMaintenance: c.conditionsMaintenance ?? '',
    conditionsLivraison: c.conditionsLivraison ?? '',
    penaliteRetardPctParJour: c.penaliteRetardPctParJour ?? '',
    delaiLivraisonJours: contrat.delaiLivraisonJours,
    jalons: contrat.jalons.map((j) => ({ libelle: j.libelle, datePrevue: toDate(j.datePrevue) })),
  }
}

/** Editeur des clauses et jalons d'un contrat BROUILLON (RA). */
export function ClausesDialog({ contrat, open, onOpenChange }: Props) {
  const { t } = useTranslation('contrats')
  const mutation = useEnregistrerClauses(contrat.id)
  const form = useForm<ClausesFormInput, unknown, ClausesFormOutput>({
    resolver: zodResolver(clausesSchema),
    defaultValues: defaults(contrat),
  })
  const jalons = useFieldArray({ control: form.control, name: 'jalons' })

  const submit = form.handleSubmit(async (values) => {
    try {
      await mutation.mutateAsync({
        ...values,
        jalons: values.jalons.map((j) => ({ libelle: j.libelle, datePrevue: toIsoDate(j.datePrevue) ?? '' })),
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
        if (next) form.reset(defaults(contrat))
        onOpenChange(next)
      }}
    >
      <DialogContent title={t('clauses.edit')} description={contrat.numero} size="lg">
        <form onSubmit={(e) => void submit(e)} className="grid gap-4" noValidate>
          <div className="grid gap-4 sm:grid-cols-3">
            <FormField control={form.control} name="delaiLivraisonJours" label={t('clauses.delai')} required>
              {({ field, control }) => (
                <Input {...control} {...field} type="number" min={1} inputMode="numeric" />
              )}
            </FormField>
            <FormField control={form.control} name="garantieMois" label={t('clauses.garantie')} optional>
              {({ field, control }) => <Input {...control} {...field} type="number" min={0} />}
            </FormField>
            <FormField
              control={form.control}
              name="penaliteRetardPctParJour"
              label={t('clauses.penalite')}
              optional
            >
              {({ field, control }) => <Input {...control} {...field} type="number" min={0} step="0.01" />}
            </FormField>
          </div>
          <FormField
            control={form.control}
            name="conditionsLivraison"
            label={t('clauses.livraison')}
            optional
          >
            {({ field, control }) => <Textarea {...control} {...field} rows={3} />}
          </FormField>
          <FormField
            control={form.control}
            name="conditionsMaintenance"
            label={t('clauses.maintenance')}
            optional
          >
            {({ field, control }) => <Textarea {...control} {...field} rows={3} />}
          </FormField>
          <fieldset className="grid gap-3">
            <legend className="mb-2 text-sm font-medium">{t('clauses.jalons')}</legend>
            {jalons.fields.length === 0 && (
              <p className="text-sm text-muted-foreground">{t('clauses.noJalon')}</p>
            )}
            {jalons.fields.map((item, index) => (
              <div key={item.id} className="grid items-start gap-3 sm:grid-cols-[1fr_12rem_auto]">
                <FormField
                  control={form.control}
                  name={`jalons.${index}.libelle`}
                  label={t('clauses.libelle')}
                >
                  {({ field, control }) => <Input {...control} {...field} />}
                </FormField>
                <FormField
                  control={form.control}
                  name={`jalons.${index}.datePrevue`}
                  label={t('clauses.datePrevue')}
                >
                  {({ field, control }) => (
                    <DatePicker {...control} value={field.value} onChange={field.onChange} />
                  )}
                </FormField>
                <IconButton
                  className="sm:mt-6"
                  label={t('clauses.removeJalon')}
                  icon={<Trash2 />}
                  onClick={() => {
                    jalons.remove(index)
                  }}
                />
              </div>
            ))}
            <div>
              <Button
                variant="secondary"
                size="sm"
                leftIcon={<Plus />}
                onClick={() => {
                  jalons.append({ libelle: '', datePrevue: null })
                }}
              >
                {t('clauses.addJalon')}
              </Button>
            </div>
          </fieldset>
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
              Enregistrer
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
