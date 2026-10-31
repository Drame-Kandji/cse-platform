import * as CheckboxPrimitive from '@radix-ui/react-checkbox'
import { Check, Minus } from 'lucide-react'
import { forwardRef, useId, type ComponentPropsWithoutRef, type ElementRef, type ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'

export interface CheckboxProps extends ComponentPropsWithoutRef<typeof CheckboxPrimitive.Root> {
  /** Libelle cliquable affiche a droite. */
  label?: ReactNode
  description?: ReactNode
}

/** Case a cocher (supporte l'etat "indetermine"). */
export const Checkbox = forwardRef<ElementRef<typeof CheckboxPrimitive.Root>, CheckboxProps>(
  function Checkbox({ className, label, description, id, ...props }, ref) {
    const generated = useId()
    const inputId = id ?? generated
    const box = (
      <CheckboxPrimitive.Root
        ref={ref}
        id={inputId}
        className={cn(
          'peer grid size-[18px] shrink-0 place-items-center rounded-[5px] border border-input bg-surface shadow-xs transition-colors hover:border-border-strong focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:border-primary data-[state=indeterminate]:border-primary data-[state=checked]:bg-primary data-[state=indeterminate]:bg-primary data-[state=checked]:text-primary-foreground data-[state=indeterminate]:text-primary-foreground',
          className,
        )}
        {...props}
      >
        <CheckboxPrimitive.Indicator>
          {props.checked === 'indeterminate' ? (
            <Minus className="size-3.5" strokeWidth={3} aria-hidden />
          ) : (
            <Check className="size-3.5" strokeWidth={3} aria-hidden />
          )}
        </CheckboxPrimitive.Indicator>
      </CheckboxPrimitive.Root>
    )
    if (!label) return box
    return (
      <div className="flex items-start gap-2.5">
        <div className="pt-0.5">{box}</div>
        <div className="grid gap-0.5">
          <label
            htmlFor={inputId}
            className="cursor-pointer text-sm font-medium leading-5 peer-disabled:opacity-60"
          >
            {label}
          </label>
          {description && <p className="text-xs text-muted-foreground">{description}</p>}
        </div>
      </div>
    )
  },
)
