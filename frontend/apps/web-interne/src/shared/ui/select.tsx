import * as SelectPrimitive from '@radix-ui/react-select'
import { Check, ChevronDown } from 'lucide-react'
import { forwardRef } from 'react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'
import { fieldClasses } from './input'

export interface SelectOption<V extends string = string> {
  readonly value: V
  readonly label: string
  readonly disabled?: boolean
}

export interface SelectProps<V extends string = string> {
  options: readonly SelectOption<V>[]
  value: V | undefined
  onValueChange: (value: V) => void
  placeholder?: string
  disabled?: boolean
  id?: string
  name?: string
  className?: string
  'aria-invalid'?: boolean
  'aria-describedby'?: string
  'aria-label'?: string
}

/**
 * Liste deroulante simple (valeurs non vides). Pour une liste longue avec recherche : Combobox.
 * @example <Select options={optionsOf('priorite')} value={v} onValueChange={setV} />
 */
function SelectInner<V extends string>(
  { options, value, onValueChange, placeholder, disabled, id, name, className, ...aria }: SelectProps<V>,
  ref: React.ForwardedRef<HTMLButtonElement>,
) {
  const { t } = useTranslation()
  return (
    <SelectPrimitive.Root
      value={value ?? ''}
      onValueChange={(next) => {
        onValueChange(next as V)
      }}
      disabled={disabled}
      name={name}
    >
      <SelectPrimitive.Trigger
        ref={ref}
        id={id}
        className={cn(
          fieldClasses,
          'flex h-10 items-center justify-between gap-2 px-3 text-left data-[placeholder]:text-muted-foreground sm:h-9',
          className,
        )}
        {...aria}
      >
        <span className="truncate">
          <SelectPrimitive.Value placeholder={placeholder ?? t('select.placeholder')} />
        </span>
        <SelectPrimitive.Icon asChild>
          <ChevronDown className="size-4 shrink-0 text-muted-foreground" aria-hidden />
        </SelectPrimitive.Icon>
      </SelectPrimitive.Trigger>
      <SelectPrimitive.Portal>
        <SelectPrimitive.Content
          position="popper"
          sideOffset={4}
          className="z-50 max-h-[var(--radix-select-content-available-height)] min-w-[var(--radix-select-trigger-width)] animate-pop-in overflow-hidden rounded-lg border border-border bg-surface-raised p-1 shadow-overlay outline-none focus-visible:ring-0 focus-visible:ring-offset-0"
        >
          <SelectPrimitive.Viewport>
            {options.map((option) => (
              <SelectPrimitive.Item
                key={option.value}
                value={option.value}
                disabled={option.disabled}
                className="relative flex h-9 cursor-pointer select-none items-center rounded-md pl-8 pr-3 text-sm outline-none data-[disabled]:pointer-events-none data-[highlighted]:bg-muted data-[disabled]:opacity-50"
              >
                <SelectPrimitive.ItemIndicator className="absolute left-2.5">
                  <Check className="size-4 text-primary-emphasis" aria-hidden />
                </SelectPrimitive.ItemIndicator>
                <SelectPrimitive.ItemText>{option.label}</SelectPrimitive.ItemText>
              </SelectPrimitive.Item>
            ))}
          </SelectPrimitive.Viewport>
        </SelectPrimitive.Content>
      </SelectPrimitive.Portal>
    </SelectPrimitive.Root>
  )
}

export const Select = forwardRef(SelectInner) as <V extends string = string>(
  props: SelectProps<V> & { ref?: React.ForwardedRef<HTMLButtonElement> },
) => React.ReactElement
