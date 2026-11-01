import { cn } from '@/shared/lib/cn'

/**
 * Progression horizontale simple : segments + libelles (libelle courant seul sous 640 px).
 * `current` = index de l'etape en cours ; `steps.length` = tout est termine.
 */
export function ProgressSteps({
  steps,
  current,
  label,
}: {
  steps: readonly string[]
  current: number
  label: string
}) {
  const termine = current >= steps.length
  return (
    <div className="grid gap-3">
      <ol
        aria-label={label}
        className="grid gap-2"
        style={{ gridTemplateColumns: `repeat(${steps.length}, minmax(0, 1fr))` }}
      >
        {steps.map((step, index) => {
          const state = index < current ? 'done' : index === current ? 'current' : 'todo'
          return (
            <li key={step} aria-current={state === 'current' ? 'step' : undefined} className="grid gap-2">
              <span
                aria-hidden
                className={cn(
                  'h-1.5 rounded-full',
                  state === 'todo' ? 'bg-border' : state === 'current' ? 'bg-primary/40' : 'bg-primary',
                )}
              />
              <span
                className={cn(
                  'hidden truncate text-xs sm:block',
                  state === 'current'
                    ? 'font-medium text-primary-emphasis'
                    : state === 'done'
                      ? 'text-foreground'
                      : 'text-muted-foreground',
                )}
              >
                <span className="sr-only">
                  {state === 'done' ? 'Terminé : ' : state === 'current' ? 'En cours : ' : 'À venir : '}
                </span>
                {step}
              </span>
            </li>
          )
        })}
      </ol>
      <p className="text-sm font-medium text-primary-emphasis sm:hidden" aria-hidden>
        {termine ? steps.at(-1) : `${current + 1}/${steps.length} · ${steps[current] ?? ''}`}
      </p>
    </div>
  )
}
