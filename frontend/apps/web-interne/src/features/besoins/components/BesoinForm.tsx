import { zodResolver } from '@hookform/resolvers/zod'
import { startOfDay } from 'date-fns'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { getErrorMessage, isApiError } from '@/shared/api'
import { optionsOf } from '@/shared/domain/labels'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { DatePicker } from '@/shared/ui/date-picker'
import { FormField } from '@/shared/ui/field'
import { Input, Textarea } from '@/shared/ui/input'
import { RadioGroup } from '@/shared/ui/radio-group'
import { Select } from '@/shared/ui/select'
import { Skeleton } from '@/shared/ui/skeleton'
import type { BesoinPayload } from '../api/types'
import { useChantiersSelectionnables } from '../hooks/useBesoins'
import {
  besoinSchema,
  justificationRequise,
  toBesoinPayload,
  type BesoinFormInput,
  type BesoinFormValues,
} from '../schemas/besoin.schema'

/** Codes metier 422/403 de ms-besoins rattaches a un champ du formulaire. */
const CODE_VERS_CHAMP: Readonly<Record<string, 'justification' | 'chantierId'>> = {
  JUSTIFICATION_OBLIGATOIRE: 'justification',
  CHANTIER_INTROUVABLE: 'chantierId',
  CHANTIER_INACTIF: 'chantierId',
  CHANTIER_NON_AUTORISE: 'chantierId',
}

export interface BesoinFormProps {
  defaultValues: BesoinFormInput
  submitLabel: string
  /** Envoi a l'API ; une erreur rejetee est reportee sur les champs (ProblemDetail `errors[]`). */
  onSubmit: (payload: BesoinPayload) => Promise<unknown>
  onCancel: () => void
}

/** Formulaire structure d'expression d'un besoin (mobile d'abord, R08 visible en direct). */
export function BesoinForm({ defaultValues, submitLabel, onSubmit, onCancel }: BesoinFormProps) {
  const { t } = useTranslation('besoins')
  const chantiers = useChantiersSelectionnables()
  const form = useForm<BesoinFormInput, unknown, BesoinFormValues>({
    resolver: zodResolver(besoinSchema),
    defaultValues,
  })
  const priorite = form.watch('priorite')
  const justificationObligatoire = justificationRequise(priorite)
  const chantierOptions = (chantiers.data?.content ?? []).map((c) => ({
    value: c.id,
    label: `${c.code} · ${c.nom}`,
  }))

  // Un chef rattache a un seul chantier n'a rien a choisir.
  const seulChantier = chantierOptions.length === 1 ? chantierOptions[0]?.value : undefined
  useEffect(() => {
    if (seulChantier && !form.getValues('chantierId')) form.setValue('chantierId', seulChantier)
  }, [seulChantier, form])

  const submit = form.handleSubmit(async (values) => {
    try {
      await onSubmit(toBesoinPayload(values))
    } catch (error) {
      if (applyServerErrors(error, form.setError)) return
      const champ = isApiError(error) && error.code ? CODE_VERS_CHAMP[error.code] : undefined
      if (champ && isApiError(error)) {
        form.setError(champ, { type: 'server', message: error.userMessage }, { shouldFocus: true })
        return
      }
      toast.error(getErrorMessage(error))
    }
  })

  const prioriteOptions = optionsOf('priorite').map((option) => ({
    ...option,
    description:
      option.value === 'NORMAL'
        ? t('form.prioriteNormal')
        : option.value === 'URGENT'
          ? t('form.prioriteUrgent')
          : t('form.prioriteCritique'),
  }))

  return (
    <form onSubmit={(event) => void submit(event)} noValidate className="grid gap-5 pb-24 sm:pb-0">
      <Card>
        <CardHeader title={t('form.sectionQuoi')} />
        <CardContent className="grid gap-4">
          <FormField control={form.control} name="categorie" label={t('form.categorie')} required>
            {({ field, control }) => (
              <RadioGroup
                {...control}
                variant="card"
                className="grid-cols-1 sm:grid-cols-2"
                options={optionsOf('categorieMateriel')}
                value={field.value ?? undefined}
                onValueChange={field.onChange}
              />
            )}
          </FormField>
          <FormField control={form.control} name="designation" label={t('form.designation')} required>
            {({ field, control }) => (
              <Input
                {...control}
                {...field}
                placeholder={t('form.designationPlaceholder')}
                autoComplete="off"
              />
            )}
          </FormField>
          <div className="grid grid-cols-2 gap-4">
            <FormField control={form.control} name="quantite" label={t('form.quantite')} required>
              {({ field, control }) => (
                <Input
                  {...control}
                  name={field.name}
                  ref={field.ref}
                  onBlur={field.onBlur}
                  value={String(field.value)}
                  onChange={(event) => {
                    field.onChange(event.target.value)
                  }}
                  type="number"
                  inputMode="numeric"
                  min={1}
                />
              )}
            </FormField>
            <FormField control={form.control} name="dureeUtilisationJours" label={t('form.duree')} required>
              {({ field, control }) => (
                <Input
                  {...control}
                  name={field.name}
                  ref={field.ref}
                  onBlur={field.onBlur}
                  value={String(field.value)}
                  onChange={(event) => {
                    field.onChange(event.target.value)
                  }}
                  type="number"
                  inputMode="numeric"
                  min={1}
                  rightSlot={
                    <span className="pr-2 text-xs text-muted-foreground">{t('form.dureeUnite')}</span>
                  }
                />
              )}
            </FormField>
          </div>
          <FormField control={form.control} name="specifications" label={t('form.specifications')} optional>
            {({ field, control }) => (
              <Textarea {...control} {...field} rows={3} placeholder={t('form.specificationsPlaceholder')} />
            )}
          </FormField>
        </CardContent>
      </Card>

      <Card>
        <CardHeader title={t('form.sectionQuand')} />
        <CardContent className="grid gap-4 sm:grid-cols-2">
          <FormField
            control={form.control}
            name="chantierId"
            label={t('form.chantier')}
            required
            hint={chantiers.isSuccess && chantierOptions.length === 0 ? t('form.chantierEmpty') : undefined}
          >
            {({ field, control }) =>
              chantiers.isPending ? (
                <Skeleton className="h-10 w-full" />
              ) : (
                <Select
                  {...control}
                  options={chantierOptions}
                  value={field.value || undefined}
                  onValueChange={field.onChange}
                  placeholder={t('form.chantierPlaceholder')}
                />
              )
            }
          </FormField>
          <FormField control={form.control} name="dateBesoin" label={t('form.dateBesoin')} required>
            {({ field, control }) => (
              <DatePicker
                {...control}
                value={field.value}
                onChange={field.onChange}
                min={startOfDay(new Date())}
              />
            )}
          </FormField>
        </CardContent>
      </Card>

      <Card>
        <CardHeader title={t('form.sectionUrgence')} />
        <CardContent className="grid gap-4">
          <FormField control={form.control} name="priorite" label={t('form.priorite')} required>
            {({ field, control }) => (
              <RadioGroup
                {...control}
                variant="card"
                orientation="horizontal"
                options={prioriteOptions}
                value={field.value}
                onValueChange={(value) => {
                  field.onChange(value)
                  if (!justificationRequise(value)) form.clearErrors('justification')
                }}
              />
            )}
          </FormField>
          <FormField
            control={form.control}
            name="justification"
            label={t('form.justification')}
            required={justificationObligatoire}
            optional={!justificationObligatoire}
            hint={t('form.justificationHint')}
          >
            {({ field, control }) => (
              <Textarea {...control} {...field} rows={3} placeholder={t('form.justificationPlaceholder')} />
            )}
          </FormField>
        </CardContent>
      </Card>

      <div
        data-mobile-action-bar
        className="pb-safe fixed inset-x-0 bottom-0 z-20 flex gap-2 border-t border-border bg-surface/95 px-4 py-3 backdrop-blur sm:static sm:justify-end sm:border-0 sm:bg-transparent sm:p-0 sm:backdrop-blur-none"
      >
        <Button variant="secondary" onClick={onCancel} className="flex-1 sm:flex-none">
          {t('form.cancel')}
        </Button>
        <Button
          type="submit"
          size="lg"
          loading={form.formState.isSubmitting}
          className="flex-[2] sm:flex-none"
        >
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}
