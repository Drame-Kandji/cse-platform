import { Check } from 'lucide-react'
import { cn } from '@/shared/lib/cn'

export interface Step {
  readonly id: string
  readonly label: string
  readonly description?: string
}

export interface StepperProps {
  steps: readonly Step[]
  /** Index de l'etape courante (0-base). Les precedentes sont "terminees". */
  current: number
  /** Etape en erreur (ex. besoin rejete). */
  errorIndex?: number
  className?: string
}

/** Progression d'un workflow (besoin, contrat). Horizontal sur desktop, vertical sur mobile. */
export function Stepper({ steps, current, errorIndex, className }: StepperProps) {
  return (
    <ol className={cn('flex flex-col gap-4 md:flex-row md:gap-0', className)}>
      {steps.map((step, index) => {
        const state =
          index === errorIndex ? 'error' : index < current ? 'done' : index === current ? 'current' : 'todo'
        return (
          <li
            key={step.id}
            className="relative flex flex-1 gap-3 md:flex-col md:gap-2"
            aria-current={state === 'current' ? 'step' : undefined}
          >
            {index < steps.length - 1 && (
              <span
                aria-hidden
                className={cn(
                  'absolute left-[13px] top-8 h-[calc(100%-1rem)] w-px md:left-8 md:top-[13px] md:h-px md:w-[calc(100%-2rem)]',
                  index < current ? 'bg-primary' : 'bg-border',
                )}
              />
            )}
            <span
              className={cn(
                'relative z-10 grid size-7 shrink-0 place-items-center rounded-full border text-xs font-semibold tabular-nums',
                state === 'done' && 'border-primary bg-primary text-primary-foreground',
                state === 'current' &&
                  'border-primary bg-surface text-primary-emphasis ring-4 ring-primary/15',
                state === 'todo' && 'border-border-strong bg-surface text-muted-foreground',
                state === 'error' && 'border-danger bg-danger text-danger-foreground',
              )}
            >
              {state === 'done' ? <Check className="size-3.5" strokeWidth={3} aria-hidden /> : index + 1}
            </span>
            <div className="min-w-0 md:pr-4">
              <p
                className={cn(
                  'text-sm font-medium',
                  state === 'todo' ? 'text-muted-foreground' : 'text-foreground',
                )}
              >
                {step.label}
              </p>
              {step.description && <p className="text-xs text-muted-foreground">{step.description}</p>}
            </div>
          </li>
        )
      })}
    </ol>
  )
}
