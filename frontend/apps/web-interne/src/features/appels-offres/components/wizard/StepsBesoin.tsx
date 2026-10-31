import type { UseFormReturn } from 'react-hook-form'
import { optionsOf } from '@/shared/domain/labels'
import { FormField } from '@/shared/ui/field'
import { Input, Textarea } from '@/shared/ui/input'
import { RadioGroup } from '@/shared/ui/radio-group'
import { Select } from '@/shared/ui/select'
import type { AppelOffreFormInput, AppelOffreFormOutput } from '../../schemas/appel-offre.schema'
import { NumberInput } from '../NumberInput'

export type WizardForm = UseFormReturn<AppelOffreFormInput, unknown, AppelOffreFormOutput>

const TYPES = [
  {
    value: 'APPEL_OFFRES' as const,
    label: "Appel d'offres",
    description: 'Consultation ouverte et formalisée, pour les montants importants',
  },
  {
    value: 'DEMANDE_PRIX' as const,
    label: 'Demande de prix',
    description: 'Consultation simplifiée auprès de quelques fournisseurs',
  },
]

/** Etape 1 : objet de la consultation et besoin d'origine. */
export function StepObjet({ form, besoinReference }: { form: WizardForm; besoinReference?: string }) {
  return (
    <div className="grid gap-4">
      {besoinReference && (
        <p className="rounded-lg border border-border bg-surface-sunken px-3 py-2 text-sm text-muted-foreground">
          Lancé à partir du besoin <span className="font-mono text-foreground">{besoinReference}</span>
        </p>
      )}
      <FormField control={form.control} name="objet" label="Objet" required>
        {({ field, control }) => (
          <Input {...control} {...field} placeholder="Ex. Fourniture de deux pelles hydrauliques" />
        )}
      </FormField>
      <FormField control={form.control} name="type" label="Type de consultation" required>
        {({ field, control }) => (
          <RadioGroup
            {...control}
            variant="card"
            orientation="horizontal"
            options={TYPES}
            value={field.value}
            onValueChange={field.onChange}
          />
        )}
      </FormField>
      <FormField control={form.control} name="description" label="Description" optional>
        {({ field, control }) => <Textarea {...control} {...field} value={field.value ?? ''} rows={3} />}
      </FormField>
    </div>
  )
}

/** Etape 2 : specifications techniques, quantite et budget. */
export function StepSpecifications({ form }: { form: WizardForm }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <FormField control={form.control} name="categorie" label="Catégorie" required>
        {({ field, control }) => (
          <Select
            {...control}
            options={optionsOf('categorieMateriel')}
            value={field.value}
            onValueChange={(value) => {
              field.onChange(value)
              form.setValue('fournisseurIdsInvites', [])
            }}
          />
        )}
      </FormField>
      <FormField control={form.control} name="quantite" label="Quantité" required>
        {({ field, control }) => (
          <NumberInput {...control} min={1} value={field.value} onChange={field.onChange} />
        )}
      </FormField>
      <FormField
        control={form.control}
        name="budgetEstime"
        label="Budget estimé (FCFA)"
        optional
        hint="Sert de repère pour l'évaluation ; non communiqué aux fournisseurs."
        className="sm:col-span-2"
      >
        {({ field, control }) => (
          <NumberInput {...control} min={0} step={100000} value={field.value} onChange={field.onChange} />
        )}
      </FormField>
      <FormField
        control={form.control}
        name="specifications"
        label="Spécifications techniques"
        optional
        className="sm:col-span-2"
      >
        {({ field, control }) => <Textarea {...control} {...field} value={field.value ?? ''} rows={5} />}
      </FormField>
    </div>
  )
}
