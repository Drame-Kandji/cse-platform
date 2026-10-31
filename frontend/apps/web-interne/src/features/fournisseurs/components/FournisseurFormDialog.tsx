import { zodResolver } from '@hookform/resolvers/zod'
import type { ReactNode } from 'react'
import { useForm, type FieldPath } from 'react-hook-form'
import { getErrorMessage } from '@/shared/api'
import { optionsOf } from '@/shared/domain/labels'
import { applyServerErrors } from '@/shared/lib/forms'
import { Button } from '@/shared/ui/button'
import { Checkbox } from '@/shared/ui/checkbox'
import { Dialog, DialogContent, DialogFooter } from '@/shared/ui/dialog'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { toast } from 'sonner'
import type { FournisseurDetail, FournisseurSaisie } from '../api/types'
import { useCreerFournisseur, useModifierFournisseur } from '../hooks/useFournisseurMutations'
import {
  fournisseurSchema,
  type FournisseurFormInput,
  type FournisseurFormOutput,
} from '../schemas/fournisseur.schema'

function defaults(f?: FournisseurDetail): FournisseurFormInput {
  return {
    raisonSociale: f?.raisonSociale ?? '',
    ninea: f?.ninea ?? '',
    rccm: f?.rccm ?? '',
    adresse: f?.adresse ?? '',
    ville: f?.ville ?? '',
    pays: f?.pays ?? 'Sénégal',
    telephone: f?.telephone ?? '',
    email: f?.email ?? '',
    contactNom: f?.contactNom ?? '',
    contactFonction: f?.contactFonction ?? '',
    specialites: [...(f?.specialites ?? [])],
    certifications: (f?.certifications ?? []).join(', '),
    banque: f?.coordonneesBancaires?.banque ?? '',
    iban: f?.coordonneesBancaires?.iban ?? '',
  }
}

/** Convertit les valeurs du formulaire en corps d'API. */
function toSaisie(values: FournisseurFormOutput): FournisseurSaisie {
  const { banque, iban, certifications, ...rest } = values
  return {
    ...rest,
    certifications: certifications
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean),
    coordonneesBancaires: banque && iban ? { banque, iban } : undefined,
  }
}

type TextField = Exclude<FieldPath<FournisseurFormInput>, 'specialites' | `specialites.${number}`>
const TEXT_FIELDS: readonly { name: TextField; label: string; required?: boolean; type?: string }[] = [
  { name: 'raisonSociale', label: 'Raison sociale', required: true },
  { name: 'ninea', label: 'NINEA', required: true },
  { name: 'rccm', label: 'RCCM' },
  { name: 'email', label: 'E-mail', required: true, type: 'email' },
  { name: 'telephone', label: 'Téléphone', type: 'tel' },
  { name: 'adresse', label: 'Adresse' },
  { name: 'ville', label: 'Ville' },
  { name: 'pays', label: 'Pays', required: true },
  { name: 'contactNom', label: 'Nom du contact' },
  { name: 'contactFonction', label: 'Fonction du contact' },
  { name: 'certifications', label: 'Certifications (séparées par des virgules)' },
  { name: 'banque', label: 'Banque' },
  { name: 'iban', label: 'IBAN' },
]

interface Props {
  /** Fournisseur a modifier (absent = creation). */
  fournisseur?: FournisseurDetail
  trigger: ReactNode
  open: boolean
  onOpenChange: (open: boolean) => void
}

/** Dialogue de creation / modification d'un fournisseur (RA). */
export function FournisseurFormDialog({ fournisseur, trigger, open, onOpenChange }: Props) {
  const creer = useCreerFournisseur()
  const modifier = useModifierFournisseur(fournisseur?.id ?? '')
  const form = useForm<FournisseurFormInput, unknown, FournisseurFormOutput>({
    resolver: zodResolver(fournisseurSchema),
    defaultValues: defaults(fournisseur),
  })
  const pending = creer.isPending || modifier.isPending
  const submit = form.handleSubmit(async (values) => {
    const body = toSaisie(values)
    try {
      if (fournisseur) await modifier.mutateAsync({ ...body, version: fournisseur.version })
      else await creer.mutateAsync(body)
      onOpenChange(false)
    } catch (error) {
      if (!applyServerErrors(error, form.setError)) toast.error(getErrorMessage(error))
    }
  })
  const specialites = form.watch('specialites')
  const specialitesError = form.formState.errors.specialites?.message

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (next) form.reset(defaults(fournisseur))
        onOpenChange(next)
      }}
    >
      {trigger}
      <DialogContent
        size="lg"
        title={fournisseur ? 'Modifier le fournisseur' : 'Nouveau fournisseur'}
        description="Un fournisseur créé par le Responsable Achats est directement actif."
      >
        <form
          noValidate
          onSubmit={(event) => {
            void submit(event)
          }}
          className="grid gap-4 sm:grid-cols-2"
        >
          {TEXT_FIELDS.map(({ name, label, required, type }) => (
            <FormField key={name} control={form.control} name={name} label={label} required={required}>
              {({ field, control }) => (
                <Input
                  {...control}
                  name={field.name}
                  ref={field.ref}
                  onBlur={field.onBlur}
                  type={type}
                  value={field.value ?? ''}
                  onChange={field.onChange}
                />
              )}
            </FormField>
          ))}
          <fieldset className="grid gap-2 sm:col-span-2" aria-describedby="specialites-error">
            <legend className="mb-1 text-sm font-medium">
              Spécialités
              <span className="ml-0.5 text-danger-emphasis" aria-hidden>
                *
              </span>
            </legend>
            <div className="grid gap-2 sm:grid-cols-2">
              {optionsOf('categorieMateriel').map((option) => {
                const current = specialites
                return (
                  <Checkbox
                    key={option.value}
                    label={option.label}
                    checked={current.includes(option.value)}
                    onCheckedChange={(checked) => {
                      form.setValue(
                        'specialites',
                        checked === true
                          ? [...current, option.value]
                          : current.filter((value) => value !== option.value),
                        { shouldValidate: form.formState.isSubmitted },
                      )
                    }}
                  />
                )
              })}
            </div>
            {specialitesError && (
              <p id="specialites-error" role="alert" className="text-xs font-medium text-danger-emphasis">
                {specialitesError}
              </p>
            )}
          </fieldset>
          <DialogFooter className="sm:col-span-2">
            <Button
              variant="secondary"
              onClick={() => {
                onOpenChange(false)
              }}
            >
              Annuler
            </Button>
            <Button type="submit" loading={pending}>
              Enregistrer
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
