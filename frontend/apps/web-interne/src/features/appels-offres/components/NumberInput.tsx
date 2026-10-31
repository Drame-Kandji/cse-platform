import { forwardRef } from 'react'
import { Input, type InputProps } from '@/shared/ui/input'

export interface NumberInputProps extends Omit<InputProps, 'value' | 'onChange' | 'type'> {
  value: number | undefined
  onChange: (value: number | undefined) => void
}

/** Champ numerique relie a RHF : chaine vide => undefined, sinon nombre. */
export const NumberInput = forwardRef<HTMLInputElement, NumberInputProps>(function NumberInput(
  { value, onChange, ...props },
  ref,
) {
  return (
    <Input
      ref={ref}
      type="number"
      inputMode="numeric"
      value={value === undefined || Number.isNaN(value) ? '' : value}
      onChange={(event) => {
        const raw = event.target.value
        onChange(raw === '' ? undefined : Number(raw))
      }}
      className="tabular-nums"
      {...props}
    />
  )
})
