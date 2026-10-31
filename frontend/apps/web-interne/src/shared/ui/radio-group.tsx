import * as RadioPrimitive from '@radix-ui/react-radio-group'
import { useId } from 'react'
import { cn } from '@/shared/lib/cn'

export interface RadioOption<V extends string = string> {
  readonly value: V
  readonly label: string
  readonly description?: string
  readonly disabled?: boolean
}

export interface RadioGroupProps<V extends string = string> {
  options: readonly RadioOption<V>[]
  value: V | undefined
  onValueChange: (value: V) => void
  orientation?: 'vertical' | 'horizontal'
  /** "card" : options encadrees (choix importants, ex. priorite). */
  variant?: 'default' | 'card'
  name?: string
  disabled?: boolean
  className?: string
  'aria-label'?: string
  'aria-labelledby'?: string
  'aria-describedby'?: string
  'aria-invalid'?: boolean
}

/** Groupe de boutons radio accessible (fleches clavier). */
export function RadioGroup<V extends string>({
  options,
  value,
  onValueChange,
  orientation = 'vertical',
  variant = 'default',
  className,
  ...props
}: RadioGroupProps<V>) {
  const baseId = useId()
  return (
    <RadioPrimitive.Root
      value={value ?? ''}
      onValueChange={(next) => {
        onValueChange(next as V)
      }}
      orientation={orientation}
      className={cn(
        'grid gap-2',
        orientation === 'horizontal' && 'sm:auto-cols-fr sm:grid-flow-col',
        className,
      )}
      {...props}
    >
      {options.map((option) => {
        const id = `${baseId}-${option.value}`
        return (
          <label
            key={option.value}
            htmlFor={id}
            className={cn(
              'flex cursor-pointer items-start gap-2.5',
              variant === 'card' &&
                'rounded-lg border border-border bg-surface p-3 transition-colors hover:border-border-strong has-[[data-state=checked]]:border-primary has-[[data-state=checked]]:bg-primary/5',
              option.disabled && 'cursor-not-allowed opacity-60',
            )}
          >
            <RadioPrimitive.Item
              id={id}
              value={option.value}
              disabled={option.disabled}
              className="mt-0.5 grid size-[18px] shrink-0 place-items-center rounded-full border border-input bg-surface shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background data-[state=checked]:border-primary"
            >
              <RadioPrimitive.Indicator className="size-2.5 rounded-full bg-primary" />
            </RadioPrimitive.Item>
            <span className="grid gap-0.5">
              <span className="text-sm font-medium leading-5">{option.label}</span>
              {option.description && (
                <span className="text-xs text-muted-foreground">{option.description}</span>
              )}
            </span>
          </label>
        )
      })}
    </RadioPrimitive.Root>
  )
}
