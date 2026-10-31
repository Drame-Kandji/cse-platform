import { ChevronRight } from 'lucide-react'
import { cn } from '@/shared/lib/cn'
import { CONTRAT_STEPS } from './contrat-domain'

/** Cycle de vie compact : etapes en texte sur une ligne, etape courante en bleu. */
export function ContratLifecycle({ current, label }: { current: number; label: string }) {
  return (
    <ol aria-label={label} className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm">
      {CONTRAT_STEPS.map((step, index) => {
        const state = index < current ? 'done' : index === current ? 'current' : 'todo'
        return (
          <li
            key={step.id}
            aria-current={state === 'current' ? 'step' : undefined}
            className={cn(
              'flex items-center gap-2',
              state === 'current' && 'font-medium text-primary-emphasis',
              state === 'done' && 'text-foreground',
              state === 'todo' && 'text-muted-foreground',
            )}
          >
            {index > 0 && <ChevronRight aria-hidden className="size-3.5 text-muted-foreground" />}
            {step.label}
          </li>
        )
      })}
    </ol>
  )
}
