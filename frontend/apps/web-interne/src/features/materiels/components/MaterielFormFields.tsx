import type { Control } from 'react-hook-form'
import { optionsOf } from '@/shared/domain/labels'
import { Combobox } from '@/shared/ui/combobox'
import { DatePicker } from '@/shared/ui/date-picker'
import { FormField } from '@/shared/ui/field'
import { Input } from '@/shared/ui/input'
import { Select } from '@/shared/ui/select'
import { useTypesEngins } from '../hooks/useParcQueries'
import type { MaterielFormInput, MaterielFormOutput } from '../schemas/materiel.schema'

type FormControl = Control<MaterielFormInput, unknown, MaterielFormOutput>
type TextName = 'code' | 'immatriculation' | 'designation' | 'marque' | 'modele'
type NumberName = 'tarifJournalier' | 'valeurAcquisition' | 'heuresFonctionnement'

function TextField(props: {
  control: FormControl
  name: TextName | NumberName
  label: string
  required?: boolean
  optional?: boolean
  numeric?: boolean
  suffix?: string
  placeholder?: string
}) {
  const { control, name, numeric, suffix, placeholder, ...field } = props
  return (
    <FormField control={control} name={name} {...field}>
      {({ field: f, control: aria }) => (
        <Input
          {...aria}
          {...f}
          value={f.value}
          inputMode={numeric ? 'numeric' : undefined}
          placeholder={placeholder}
          rightSlot={
            suffix ? <span className="px-1.5 text-xs text-muted-foreground">{suffix}</span> : undefined
          }
        />
      )}
    </FormField>
  )
}

/** Champs de la fiche materiel (grille 2 colonnes, empilee sur mobile). */
export function MaterielFormFields({ control, creation }: { control: FormControl; creation: boolean }) {
  const types = useTypesEngins()
  const typeOptions = (types.data ?? [])
    .filter((type) => type.actif)
    .map((type) => ({ value: type.id, label: type.libelle, description: type.code }))
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <TextField control={control} name="code" label="Code" required placeholder="ENG-PEL-012" />
      <TextField
        control={control}
        name="immatriculation"
        label="Immatriculation"
        optional
        placeholder="DK-1234-AB"
      />
      <TextField
        control={control}
        name="designation"
        label="Désignation"
        required
        placeholder="Pelle hydraulique 20 t"
      />
      <FormField control={control} name="categorie" label="Catégorie" required>
        {({ field, control: aria }) => (
          <Select
            {...aria}
            options={optionsOf('categorieMateriel')}
            value={field.value}
            onValueChange={field.onChange}
          />
        )}
      </FormField>
      <FormField
        control={control}
        name="typeEnginId"
        label="Type d'engin"
        optional
        hint="Fixe le seuil de maintenance préventive"
      >
        {({ field, control: aria }) => (
          <Combobox
            {...aria}
            options={typeOptions}
            value={field.value}
            onValueChange={field.onChange}
            loading={types.isPending}
          />
        )}
      </FormField>
      <div className="grid grid-cols-2 gap-3">
        <TextField control={control} name="marque" label="Marque" optional />
        <TextField control={control} name="modele" label="Modèle" optional />
      </div>
      <TextField
        control={control}
        name="tarifJournalier"
        label="Tarif journalier"
        required
        numeric
        suffix="FCFA"
      />
      <TextField
        control={control}
        name="valeurAcquisition"
        label="Valeur d'acquisition"
        optional
        numeric
        suffix="FCFA"
      />
      <FormField control={control} name="dateAcquisition" label="Date d'acquisition" optional>
        {({ field, control: aria }) => (
          <DatePicker {...aria} value={field.value} onChange={field.onChange} max={new Date()} />
        )}
      </FormField>
      {creation && (
        <TextField
          control={control}
          name="heuresFonctionnement"
          label="Compteur initial"
          optional
          numeric
          suffix="h"
        />
      )}
    </div>
  )
}
