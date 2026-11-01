import { Check } from 'lucide-react'
import { CATEGORIE_MATERIEL, labelOf, type CategorieMateriel } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'

/** Choix des specialites en puces cochables (CategorieMateriel). */
export function SpecialitesPicker({
  value,
  onChange,
  id,
  invalid,
  describedBy,
  label,
}: {
  value: readonly CategorieMateriel[]
  onChange: (value: CategorieMateriel[]) => void
  id?: string
  invalid?: boolean
  describedBy?: string
  /** Nom accessible du groupe (libelle du champ). */
  label: string
}) {
  const toggle = (categorie: CategorieMateriel) => {
    onChange(value.includes(categorie) ? value.filter((c) => c !== categorie) : [...value, categorie])
  }
  return (
    <div
      id={id}
      role="group"
      aria-label={label}
      data-invalid={invalid || undefined}
      aria-describedby={describedBy}
      className="flex flex-wrap gap-2"
    >
      {CATEGORIE_MATERIEL.map((categorie) => {
        const active = value.includes(categorie)
        return (
          <button
            key={categorie}
            type="button"
            aria-pressed={active}
            onClick={() => {
              toggle(categorie)
            }}
            className={cn(
              'inline-flex h-9 items-center gap-1.5 rounded-full border px-3.5 text-sm transition-colors',
              active
                ? 'border-primary bg-primary/[0.08] font-medium text-primary-emphasis'
                : 'border-border-strong text-muted-foreground hover:border-primary/50 hover:text-foreground',
            )}
          >
            {active && <Check className="size-3.5" aria-hidden />}
            {labelOf('categorieMateriel', categorie)}
          </button>
        )
      })}
    </div>
  )
}
