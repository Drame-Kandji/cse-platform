import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { labelOf } from '@/shared/domain/labels'
import { useDebouncedValue } from '@/shared/lib/hooks'
import { Combobox, type ComboboxOption } from '@/shared/ui/combobox'
import { useRechercheMateriels } from '../hooks/useMaintenances'

interface Props {
  value: string
  onChange: (id: string) => void
  /** Option a afficher sans recherche (pre-remplissage depuis une panne). */
  initialOption?: ComboboxOption
  id?: string
  disabled?: boolean
  placeholder?: string
  'aria-invalid'?: boolean
  'aria-describedby'?: string
}

/** Selecteur de materiel avec recherche serveur (code, designation) ; exclut les materiels reformes. */
export function MaterielPicker({ value, onChange, initialOption, placeholder, ...props }: Props) {
  const { t } = useTranslation('maintenances')
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState<ComboboxOption | undefined>(initialOption)
  const debounced = useDebouncedValue(search)
  const query = useRechercheMateriels(debounced, !props.disabled)
  const results: ComboboxOption[] = (query.data?.content ?? [])
    .filter((materiel) => materiel.etat !== 'REFORME')
    .map((materiel) => ({
      value: materiel.id,
      label: `${materiel.code} · ${materiel.designation}`,
      description: [labelOf('etatMateriel', materiel.etat), materiel.chantierActuelNom]
        .filter(Boolean)
        .join(' · '),
    }))
  const options =
    selected && !results.some((option) => option.value === selected.value) ? [selected, ...results] : results
  return (
    <Combobox
      {...props}
      options={options}
      value={value || null}
      loading={query.isFetching}
      onSearchChange={setSearch}
      placeholder={placeholder}
      searchPlaceholder={t('materielPicker.search')}
      emptyText={t('materielPicker.empty')}
      onValueChange={(id) => {
        setSelected(options.find((option) => option.value === id))
        onChange(id ?? '')
      }}
    />
  )
}
