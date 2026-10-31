import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm, type FieldPath } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { toast } from 'sonner'
import { useQueryClient } from '@tanstack/react-query'
import { getErrorMessage, isApiError } from '@/shared/api'
import { useHasRole } from '@/shared/auth'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Combobox } from '@/shared/ui/combobox'
import { DatePicker } from '@/shared/ui/date-picker'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { Select } from '@/shared/ui/select'
import { chantierKeys } from '../api/chantiers.keys'
import type { Chantier } from '../api/types'
import { useChefsChantier, useCreerChantier, useModifierChantier } from '../hooks/useChantiers'
import {
  REGIONS,
  chantierDefaults,
  chantierSchema,
  toChantierPayload,
  type ChantierFormInput,
  type ChantierFormValues,
} from '../schemas/chantier.schema'

interface Props {
  /** Chantier a modifier ; absent = creation. */
  chantier?: Chantier | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

const REGION_OPTIONS = REGIONS.map((region) => ({ value: region, label: region }))

/** Creation / modification d'un chantier (RM, ADMIN). L'ADMIN choisit le chef parmi les comptes. */
export function ChantierFormDialog({ chantier, open, onOpenChange }: Props) {
  const { t } = useTranslation('chantiers')
  const queryClient = useQueryClient()
  const estAdmin = useHasRole(['ADMIN_SYSTEME'])
  const chefs = useChefsChantier(open && estAdmin)
  const creer = useCreerChantier()
  const modifier = useModifierChantier()
  const form = useForm<ChantierFormInput, unknown, ChantierFormValues>({
    resolver: zodResolver(chantierSchema),
    defaultValues: chantierDefaults(chantier),
  })
  useEffect(() => {
    if (open) form.reset(chantierDefaults(chantier))
  }, [open, chantier, form])

  const chefOptions = (chefs.data?.content ?? []).map((chef) => ({
    value: chef.id,
    label: `${chef.prenom} ${chef.nom}`,
    description: chef.email,
  }))

  const submit = form.handleSubmit(async (values) => {
    const payload = toChantierPayload(values)
    try {
      if (chantier) await modifier.mutateAsync({ ...payload, id: chantier.id, version: chantier.version })
      else await creer.mutateAsync(payload)
      onOpenChange(false)
    } catch (error) {
      if (applyServerErrors(error, form.setError)) return
      if (isApiError(error) && error.status === 409 && chantier) {
        toast.error(getErrorMessage(error), {
          action: {
            label: t('form.recharger'),
            onClick: () => {
              void queryClient.invalidateQueries({ queryKey: chantierKeys.all })
              onOpenChange(false)
            },
          },
        })
        return
      }
      toast.error(getErrorMessage(error))
    }
  })

  const numberField = (name: FieldPath<ChantierFormInput>, label: string, step?: string) => (
    <FormField control={form.control} name={name} label={label} required>
      {({ field, control }) => (
        <Input
          {...control}
          name={field.name}
          ref={field.ref}
          onBlur={field.onBlur}
          value={String(field.value ?? '')}
          onChange={(event) => {
            field.onChange(event.target.value)
          }}
          type="number"
          inputMode="decimal"
          step={step}
          className="tabular-nums"
        />
      )}
    </FormField>
  )

  const pending = form.formState.isSubmitting
  return (
    <Dialog open={open} onOpenChange={(next) => !pending && onOpenChange(next)}>
      <DialogContent
        size="lg"
        title={chantier ? t('form.editTitle', { code: chantier.code }) : t('form.createTitle')}
        description={t('form.description')}
      >
        <form
          id="chantier-form"
          onSubmit={(event) => void submit(event)}
          noValidate
          className="grid gap-4 sm:grid-cols-2"
        >
          <FormField control={form.control} name="code" label={t('form.code')} required>
            {({ field, control }) => (
              <Input
                {...control}
                {...field}
                placeholder={t('form.codePlaceholder')}
                className="font-mono uppercase"
              />
            )}
          </FormField>
          <FormField control={form.control} name="nom" label={t('form.nom')} required>
            {({ field, control }) => <Input {...control} {...field} placeholder={t('form.nomPlaceholder')} />}
          </FormField>
          <FormField control={form.control} name="localisation" label={t('form.localisation')} required>
            {({ field, control }) => (
              <Input {...control} {...field} placeholder={t('form.localisationPlaceholder')} />
            )}
          </FormField>
          <FormField control={form.control} name="region" label={t('form.region')} required>
            {({ field, control }) => (
              <Select
                {...control}
                options={REGION_OPTIONS}
                value={field.value || undefined}
                onValueChange={field.onChange}
              />
            )}
          </FormField>
          {numberField('latitude', t('form.latitude'), 'any')}
          {numberField('longitude', t('form.longitude'), 'any')}
          {numberField('budget', t('form.budget'), '1')}
          {numberField('montantMarche', t('form.montantMarche'), '1')}
          <FormField control={form.control} name="dateDebut" label={t('form.dateDebut')} required>
            {({ field, control }) => (
              <DatePicker {...control} value={field.value} onChange={field.onChange} />
            )}
          </FormField>
          <FormField control={form.control} name="dateFinPrevue" label={t('form.dateFinPrevue')} optional>
            {({ field, control }) => (
              <DatePicker {...control} value={field.value} onChange={field.onChange} />
            )}
          </FormField>
          {estAdmin ? (
            <FormField
              control={form.control}
              name="chefChantierId"
              label={t('form.chefChantierNom')}
              optional
              className="sm:col-span-2"
            >
              {({ field, control }) => (
                <Combobox
                  {...control}
                  options={chefOptions}
                  loading={chefs.isFetching}
                  value={field.value || null}
                  onValueChange={(id) => {
                    field.onChange(id ?? '')
                    form.setValue('chefChantierNom', chefOptions.find((o) => o.value === id)?.label ?? '')
                  }}
                  placeholder={t('form.chefChantierNomPlaceholder')}
                />
              )}
            </FormField>
          ) : (
            <>
              <FormField
                control={form.control}
                name="chefChantierNom"
                label={t('form.chefChantierNom')}
                optional
              >
                {({ field, control }) => (
                  <Input {...control} {...field} placeholder={t('form.chefChantierNomPlaceholder')} />
                )}
              </FormField>
              <FormField
                control={form.control}
                name="chefChantierId"
                label={t('form.chefChantierId')}
                hint={t('form.chefChantierIdHint')}
                optional
              >
                {({ field, control }) => <Input {...control} {...field} className="font-mono text-xs" />}
              </FormField>
            </>
          )}
        </form>
        <DialogFooter>
          <Button
            variant="secondary"
            disabled={pending}
            onClick={() => {
              onOpenChange(false)
            }}
          >
            {t('form.cancel')}
          </Button>
          <Button type="submit" form="chantier-form" loading={pending}>
            {chantier ? t('form.submitEdit') : t('form.submitCreate')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
