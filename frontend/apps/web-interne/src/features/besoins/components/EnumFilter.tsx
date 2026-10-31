import { Select } from '@/shared/ui/select'

const TOUS = '__TOUS__'

interface Props<V extends string> {
  /** Libelle du filtre (affiche quand aucune valeur n'est choisie). */
  label: string
  options: readonly { value: V; label: string }[]
  value: V | undefined
  onChange: (value: V | undefined) => void
  className?: string
}

/** Filtre mono-valeur (l'API n'accepte qu'une valeur par parametre) : une valeur ou "Tous". */
export function EnumFilter<V extends string>({ label, options, value, onChange, className }: Props<V>) {
  return (
    <Select
      aria-label={label}
      className={className ?? 'sm:w-48'}
      value={value ?? TOUS}
      onValueChange={(next) => {
        onChange(next === TOUS ? undefined : next)
      }}
      options={[{ value: TOUS, label: `${label} : tous` }, ...options]}
    />
  )
}
