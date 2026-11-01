import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { portailApi } from '@/api/portail.api'
import { InlineError } from '@/components/InlineError'
import { SpecialitesPicker } from '@/components/SpecialitesPicker'
import { messageErreur } from '@/domain/errors'
import type { InscriptionRecue } from '@/domain/types'
import {
  INSCRIPTION_VIDE,
  inscriptionSchema,
  type InscriptionInput,
  type InscriptionOutput,
} from '../schemas/inscription.schema'

type TextField = Exclude<keyof InscriptionInput, 'specialites'>

/** Formulaire public d'inscription ; `onDone` recoit l'accuse de reception (201). */
export function InscriptionForm({ onDone }: { onDone: (recue: InscriptionRecue) => void }) {
  const { t } = useTranslation('portail')
  const [erreur, setErreur] = useState<string | null>(null)
  const form = useForm<InscriptionInput, unknown, InscriptionOutput>({
    resolver: zodResolver(inscriptionSchema),
    defaultValues: INSCRIPTION_VIDE,
  })
  const inscrire = useMutation({ mutationFn: portailApi.inscrire, meta: { silentError: true } })

  const submit = form.handleSubmit(async (values) => {
    setErreur(null)
    try {
      onDone(await inscrire.mutateAsync(values))
    } catch (error) {
      if (!applyServerErrors(error, form.setError)) setErreur(messageErreur(error))
    }
  })

  const text = (name: TextField, options: { required?: boolean; wide?: boolean; type?: string } = {}) => (
    <FormField
      control={form.control}
      name={name}
      label={t(`inscription.fields.${name}`)}
      required={options.required}
      className={options.wide ? 'sm:col-span-2' : undefined}
    >
      {({ field, control }) => <Input {...control} {...field} type={options.type ?? 'text'} />}
    </FormField>
  )

  return (
    <form onSubmit={(event) => void submit(event)} noValidate className="grid gap-4 sm:grid-cols-2">
      <InlineError message={erreur} className="sm:col-span-2" />
      {text('raisonSociale', { required: true, wide: true })}
      {text('ninea', { required: true })}
      {text('rccm')}
      {text('adresse', { wide: true })}
      {text('ville')}
      {text('pays', { required: true })}
      {text('telephone', { type: 'tel' })}
      {text('email', { required: true, type: 'email' })}
      {text('contactNom', { required: true })}
      {text('contactFonction')}
      <FormField
        control={form.control}
        name="specialites"
        label={t('inscription.fields.specialites')}
        required
        className="sm:col-span-2"
      >
        {({ field, control }) => (
          <SpecialitesPicker
            id={control.id}
            invalid={control['aria-invalid']}
            describedBy={control['aria-describedby']}
            label={t('inscription.fields.specialites')}
            value={field.value}
            onChange={field.onChange}
          />
        )}
      </FormField>
      <Button type="submit" size="lg" className="mt-2 sm:col-span-2" loading={inscrire.isPending}>
        {t('inscription.submit')}
      </Button>
    </form>
  )
}
