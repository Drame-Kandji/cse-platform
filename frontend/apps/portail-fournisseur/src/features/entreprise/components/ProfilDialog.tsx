import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input, Textarea } from '@/shared/ui/input'
import { useModifierProfil } from '@/api/mutations'
import { InlineError } from '@/components/InlineError'
import { SpecialitesPicker } from '@/components/SpecialitesPicker'
import { messageErreur } from '@/domain/errors'
import type { ProfilFournisseur } from '@/domain/types'
import { formulaireVersModification, profilVersFormulaire } from '../lib/profil'
import { profilSchema, type ProfilInput, type ProfilOutput } from '../schemas/entreprise.schema'

type TextField = Exclude<keyof ProfilInput, 'specialites' | 'certifications'>

/** Modification des coordonnees de l'entreprise (champs de ModifierFournisseurRequest). */
export function ProfilDialog({
  profil,
  open,
  onOpenChange,
}: {
  profil: ProfilFournisseur
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const { t } = useTranslation('portail')
  const { t: tc } = useTranslation()
  const [erreur, setErreur] = useState<string | null>(null)
  const modifier = useModifierProfil()
  const form = useForm<ProfilInput, unknown, ProfilOutput>({
    resolver: zodResolver(profilSchema),
    defaultValues: profilVersFormulaire(profil),
  })
  const { reset } = form
  useEffect(() => {
    if (open) reset(profilVersFormulaire(profil))
  }, [open, profil, reset])

  const submit = form.handleSubmit(async (values) => {
    setErreur(null)
    try {
      await modifier.mutateAsync(formulaireVersModification(values, profil.version))
      onOpenChange(false)
    } catch (error) {
      if (!applyServerErrors(error, form.setError)) setErreur(messageErreur(error))
    }
  })

  const text = (name: TextField, options: { required?: boolean; wide?: boolean; type?: string } = {}) => (
    <FormField
      control={form.control}
      name={name}
      label={t(`entreprise.fields.${name}`)}
      required={options.required}
      className={options.wide ? 'sm:col-span-2' : undefined}
    >
      {({ field, control }) => <Input {...control} {...field} type={options.type ?? 'text'} />}
    </FormField>
  )

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={t('entreprise.edit')} size="lg">
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
          {text('contactNom')}
          {text('contactFonction')}
          <FormField
            control={form.control}
            name="specialites"
            label={t('entreprise.fields.specialites')}
            required
            className="sm:col-span-2"
          >
            {({ field, control }) => (
              <SpecialitesPicker
                id={control.id}
                invalid={control['aria-invalid']}
                describedBy={control['aria-describedby']}
                label={t('entreprise.fields.specialites')}
                value={field.value}
                onChange={field.onChange}
              />
            )}
          </FormField>
          <FormField
            control={form.control}
            name="certifications"
            label={t('entreprise.fields.certifications')}
            hint={t('entreprise.certificationsHint')}
            optional
            className="sm:col-span-2"
          >
            {({ field, control }) => <Textarea {...control} {...field} rows={2} />}
          </FormField>
          {text('banque')}
          {text('iban')}
          <DialogFooter className="sm:col-span-2">
            <Button
              variant="secondary"
              onClick={() => {
                onOpenChange(false)
              }}
              disabled={modifier.isPending}
            >
              {tc('actions.cancel')}
            </Button>
            <Button type="submit" loading={modifier.isPending}>
              {tc('actions.save')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
