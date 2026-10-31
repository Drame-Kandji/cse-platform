import { Select } from '@/shared/ui/select'

const TOUS = '__TOUS__'

export interface SingleFilterProps<V extends string> {
  /** Libelle accessible du filtre (ex. "État"). */
  label: string
  /** Libelle de l'option neutre (ex. "Tous les états"). */
  allLabel: string
  options: readonly { value: V; label: string }[]
  value: V | null
  onChange: (value: V | null) => void
}

/** Filtre a valeur unique : une valeur ou "Tous" (parametre mono-valeur cote API). */
export function SingleFilter<V extends string>({
  label,
  allLabel,
  options,
  value,
  onChange,
}: SingleFilterProps<V>) {
  return (
    <Select<V | typeof TOUS>
      aria-label={label}
      className="w-auto min-w-40"
      options={[{ value: TOUS, label: allLabel }, ...options]}
      value={value ?? TOUS}
      onValueChange={(next) => {
        onChange(next === TOUS ? null : next)
      }}
    />
  )
}
