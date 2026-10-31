import { useEffect, useState } from 'react'
import { useDebouncedValue } from '@/shared/lib/hooks'
import { Input, type InputProps } from '@/shared/ui/input'

export interface DebouncedInputProps extends Omit<InputProps, 'value' | 'onChange'> {
  value: string
  /** Appele 300 ms apres la derniere frappe. */
  onValueChange: (value: string) => void
}

/** Champ texte de filtre a propagation retardee (une requete par saisie stabilisee). */
export function DebouncedInput({ value, onValueChange, ...props }: DebouncedInputProps) {
  const [draft, setDraft] = useState(value)
  const debounced = useDebouncedValue(draft)
  useEffect(() => {
    setDraft(value)
  }, [value])
  useEffect(() => {
    if (debounced !== value) onValueChange(debounced)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- on ne reagit qu'a la saisie stabilisee
  }, [debounced])
  return (
    <Input
      {...props}
      value={draft}
      onChange={(event) => {
        setDraft(event.target.value)
      }}
    />
  )
}
