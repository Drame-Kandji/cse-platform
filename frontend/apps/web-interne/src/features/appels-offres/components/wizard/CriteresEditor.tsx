import { CheckCircle2, AlertCircle, RotateCcw } from 'lucide-react'
import { useWatch } from 'react-hook-form'
import { describeStatus } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { Button } from '@/shared/ui/button'
import { CRITERES_PAR_DEFAUT, sommePoids } from '../../schemas/appel-offre.schema'
import type { WizardForm } from './StepsBesoin'

/** Etape 3 : ponderation des criteres, somme verifiee en direct (= 100). */
export function CriteresEditor({ form }: { form: WizardForm }) {
  const criteres = useWatch({ control: form.control, name: 'criteres' })
  const total = sommePoids(criteres)
  const ok = total === 100
  const erreur = form.formState.errors.criteres
  const message = erreur?.message ?? erreur?.root?.message

  const setPoids = (index: number, poids: number) => {
    const borne = Number.isFinite(poids) ? Math.min(100, Math.max(0, Math.round(poids))) : 0
    form.setValue(`criteres.${index}.poids`, borne, {
      shouldDirty: true,
      shouldValidate: form.formState.isSubmitted,
    })
    if (message) void form.trigger('criteres')
  }

  return (
    <div className="grid gap-4">
      <p className="text-sm text-muted-foreground">
        Pondérez chaque critère de 0 à 100. Un poids nul exclut le critère. Le prix et le délai sont notés
        automatiquement à partir des offres ; les autres critères sont notés par la commission.
      </p>
      <ul className="grid gap-3">
        {criteres.map((critere, index) => {
          const descriptor = describeStatus('critereEvaluation', critere.code)
          const id = `critere-${critere.code}`
          return (
            <li
              key={critere.code}
              className="grid gap-2 rounded-lg border border-border p-3 sm:grid-cols-[12rem_1fr_5.5rem] sm:items-center"
            >
              <div>
                <label htmlFor={id} className="text-sm font-medium">
                  {descriptor.label}
                </label>
                {descriptor.description && (
                  <p className="text-xs text-muted-foreground">{descriptor.description}</p>
                )}
              </div>
              <input
                type="range"
                min={0}
                max={100}
                step={5}
                value={critere.poids}
                aria-label={`Poids ${descriptor.label} (curseur)`}
                onChange={(event) => {
                  setPoids(index, Number(event.target.value))
                }}
                className="h-2 w-full cursor-pointer accent-[rgb(var(--primary))]"
              />
              <div className="flex items-center gap-1">
                <input
                  id={id}
                  type="number"
                  min={0}
                  max={100}
                  value={critere.poids}
                  onChange={(event) => {
                    setPoids(index, event.target.value === '' ? 0 : Number(event.target.value))
                  }}
                  className="h-9 w-16 rounded-lg border border-input bg-surface px-2 text-right text-sm tabular-nums focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30"
                />
                <span className="text-xs text-muted-foreground">%</span>
              </div>
            </li>
          )
        })}
      </ul>
      <div
        className={cn(
          'flex flex-wrap items-center justify-between gap-2 rounded-lg border px-3 py-2 text-sm',
          ok
            ? 'border-success/30 bg-success/10 text-success-emphasis'
            : 'border-warning/30 bg-warning/10 text-warning-emphasis',
        )}
        aria-live="polite"
        data-testid="somme-criteres"
      >
        <span className="flex items-center gap-2 font-medium tabular-nums">
          {ok ? (
            <CheckCircle2 className="size-4" aria-hidden />
          ) : (
            <AlertCircle className="size-4" aria-hidden />
          )}
          Total : {total} / 100
          {!ok && (
            <span className="font-normal">
              ({total > 100 ? `${total - 100} en trop` : `${100 - total} à répartir`})
            </span>
          )}
        </span>
        <Button
          variant="ghost"
          size="sm"
          leftIcon={<RotateCcw />}
          onClick={() => {
            form.setValue('criteres', CRITERES_PAR_DEFAUT, { shouldDirty: true })
            if (message) void form.trigger('criteres')
          }}
        >
          Pondération par défaut
        </Button>
      </div>
      {message && (
        <p role="alert" className="flex items-center gap-1 text-sm font-medium text-danger-emphasis">
          <AlertCircle className="size-4" aria-hidden />
          {message}
        </p>
      )}
    </div>
  )
}
