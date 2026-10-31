import { zodResolver } from '@hookform/resolvers/zod'
import { Search } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'
import { CATEGORIE_MATERIEL, optionsOf, PRIORITE } from '@/shared/domain/labels'
import { toIsoDate } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import type { DateRange } from '@/shared/ui/calendar'
import { Checkbox } from '@/shared/ui/checkbox'
import { Combobox } from '@/shared/ui/combobox'
import { DatePicker, DateRangePicker } from '@/shared/ui/date-picker'
import { Field, FormField } from '@/shared/ui/field'
import { Input, Textarea } from '@/shared/ui/input'
import { RadioGroup } from '@/shared/ui/radio-group'
import { Select } from '@/shared/ui/select'
import { Switch } from '@/shared/ui/switch'
import { Demo, Section } from './Section'

const CHANTIERS = [
  { value: 'diamniadio', label: 'Autoroute Diamniadio-AIBD', description: 'Lot 3 · Thiès' },
  { value: 'ter', label: 'TER phase 2', description: 'Dakar' },
  { value: 'saint-louis', label: 'Port de Saint-Louis', description: 'Saint-Louis' },
  { value: 'kaolack', label: 'Pont de Kaolack', description: 'Kaolack' },
]

const schema = z.object({
  designation: z.string().trim().min(3, 'Au moins 3 caractères'),
  quantite: z.coerce
    .number({ invalid_type_error: 'Nombre attendu' })
    .int('Nombre entier')
    .positive('Doit être positif'),
  categorie: z.enum(CATEGORIE_MATERIEL, { required_error: 'Choisissez une catégorie' }),
  priorite: z.enum(PRIORITE),
  chantierId: z.string({ required_error: 'Choisissez un chantier' }).min(1, 'Choisissez un chantier'),
  dateBesoin: z
    .date()
    .nullable()
    .refine((value) => value !== null, 'Date obligatoire'),
  justification: z.string().max(500, '500 caractères maximum').optional(),
})
type FormInput = z.input<typeof schema>
type FormOutput = z.output<typeof schema>

function DemoForm() {
  const form = useForm<FormInput, unknown, FormOutput>({
    resolver: zodResolver(schema),
    defaultValues: { designation: '', quantite: 1, priorite: 'NORMAL', dateBesoin: null, justification: '' },
  })
  const submit = form.handleSubmit((values) => {
    toast.success('Besoin enregistré', {
      description: `${values.designation} · ${toIsoDate(values.dateBesoin) ?? ''}`,
    })
  })
  return (
    <form
      onSubmit={(event) => {
        void submit(event)
      }}
      noValidate
      className="grid gap-5 sm:grid-cols-2"
    >
      <FormField
        control={form.control}
        name="designation"
        label="Désignation"
        required
        className="sm:col-span-2"
      >
        {({ field, control }) => <Input {...control} {...field} placeholder="Ex. Pelle hydraulique 20 t" />}
      </FormField>
      <FormField control={form.control} name="quantite" label="Quantité" required hint="Nombre d'unités">
        {({ field, control }) => <Input {...control} {...field} value={field.value} inputMode="numeric" />}
      </FormField>
      <FormField control={form.control} name="categorie" label="Catégorie" required>
        {({ field, control }) => (
          <Select
            {...control}
            options={optionsOf('categorieMateriel')}
            value={field.value}
            onValueChange={field.onChange}
          />
        )}
      </FormField>
      <FormField control={form.control} name="chantierId" label="Chantier" required>
        {({ field, control }) => (
          <Combobox
            {...control}
            options={CHANTIERS}
            value={field.value}
            onValueChange={(value) => {
              field.onChange(value ?? undefined)
            }}
            placeholder="Rechercher un chantier"
          />
        )}
      </FormField>
      <FormField control={form.control} name="dateBesoin" label="Date de besoin" required>
        {({ field, control }) => (
          <DatePicker {...control} value={field.value} onChange={field.onChange} min={new Date()} />
        )}
      </FormField>
      <FormField control={form.control} name="priorite" label="Priorité" className="sm:col-span-2">
        {({ field }) => (
          <RadioGroup
            aria-label="Priorité"
            variant="card"
            orientation="horizontal"
            options={optionsOf('priorite')}
            value={field.value}
            onValueChange={field.onChange}
          />
        )}
      </FormField>
      <FormField
        control={form.control}
        name="justification"
        label="Justification"
        optional
        className="sm:col-span-2"
      >
        {({ field, control }) => (
          <Textarea {...control} {...field} placeholder="Contexte, contraintes de site…" />
        )}
      </FormField>
      <div className="flex justify-end gap-2 sm:col-span-2">
        <Button
          variant="secondary"
          onClick={() => {
            form.reset()
          }}
        >
          Réinitialiser
        </Button>
        <Button type="submit" loading={form.formState.isSubmitting}>
          Enregistrer
        </Button>
      </div>
    </form>
  )
}

/** Champs de saisie et formulaire react-hook-form + zod. */
export function FormsSection() {
  const [select, setSelect] = useState<string>()
  const [chantier, setChantier] = useState<string | null>('ter')
  const [date, setDate] = useState<Date | null>(null)
  const [range, setRange] = useState<DateRange>({ from: null, to: null })
  const [radio, setRadio] = useState('PREVENTIVE')
  return (
    <Section
      id="formulaires"
      title="Formulaires"
      description="Champs accessibles, libellés reliés, erreurs zod en français."
    >
      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-2">
        <Demo title="Champs">
          <div className="grid gap-4">
            <Field label="Recherche">
              {(p) => <Input {...p} leftIcon={<Search />} placeholder="Code, désignation…" />}
            </Field>
            <Field label="Montant" hint="En francs CFA, sans décimales">
              {(p) => (
                <Input
                  {...p}
                  inputMode="numeric"
                  defaultValue="2 500 000"
                  rightSlot={<span className="pr-2 text-xs text-muted-foreground">FCFA</span>}
                />
              )}
            </Field>
            <Field label="Champ en erreur" error="Ce matricule existe déjà">
              {(p) => <Input {...p} defaultValue="CSE-PEL-001" />}
            </Field>
            <Field label="État du matériel">
              {(p) => (
                <Select {...p} options={optionsOf('etatMateriel')} value={select} onValueChange={setSelect} />
              )}
            </Field>
            <Field label="Chantier (Combobox)">
              {(p) => <Combobox {...p} options={CHANTIERS} value={chantier} onValueChange={setChantier} />}
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Date">{(p) => <DatePicker {...p} value={date} onChange={setDate} />}</Field>
              <Field label="Période">
                {(p) => <DateRangePicker {...p} value={range} onChange={setRange} />}
              </Field>
            </div>
            <Field label="Désactivé">{(p) => <Input {...p} disabled value="Lecture seule" />}</Field>
          </div>
        </Demo>
        <Demo title="Choix">
          <div className="grid gap-5">
            <Checkbox
              label="Notifier le chef de chantier"
              description="Un e-mail est envoyé à la validation."
              defaultChecked
            />
            <Checkbox label="Sélection partielle" checked="indeterminate" />
            <Switch
              label="Géolocalisation active"
              description="Remonte la position GPS toutes les 15 minutes."
              defaultChecked
            />
            <Switch label="Mode hors ligne" />
            <RadioGroup
              aria-label="Type de maintenance"
              options={optionsOf('typeMaintenance')}
              value={radio}
              onValueChange={setRadio}
            />
          </div>
        </Demo>
      </div>
      <Demo title="Formulaire complet (react-hook-form + zod + FormField) · soumettre vide pour voir les erreurs">
        <DemoForm />
      </Demo>
    </Section>
  )
}
