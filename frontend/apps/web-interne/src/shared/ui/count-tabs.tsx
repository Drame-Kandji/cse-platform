import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { Skeleton } from './skeleton'

export interface CountTabItem<V extends string> {
  /** Valeur du filtre ; `null` = "Tous". */
  readonly value: V | null
  readonly label: string
  /** Compteur affiche a cote du libelle (masque si undefined). */
  readonly count?: number
}

export interface CountTabsProps<V extends string> {
  /** Libelle accessible du groupe. */
  label: string
  items: readonly CountTabItem<V>[]
  /** Valeur active ; `null` = "Tous". */
  value: V | null
  /** Re-cliquer l'element actif revient a `null`. */
  onChange: (value: V | null) => void
  loading?: boolean
  className?: string
}

/**
 * Filtre segmente texte avec compteurs (remplace les rangees de cartes KPI colorees au-dessus des
 * listes) : "Tous 79  En attente 9  Validés 50". Boutons `aria-pressed`, defilement horizontal mobile.
 */
export function CountTabs<V extends string>({
  label,
  items,
  value,
  onChange,
  loading,
  className,
}: CountTabsProps<V>) {
  return (
    <div
      role="group"
      aria-label={label}
      className={cn('scrollbar-thin -mx-1 mb-4 flex gap-1 overflow-x-auto px-1', className)}
    >
      {items.map((item) => {
        const active = item.value === value
        return (
          <button
            key={item.value ?? '__tous__'}
            type="button"
            aria-pressed={active}
            onClick={() => {
              onChange(active || item.value === null ? null : item.value)
            }}
            className={cn(
              'inline-flex h-9 shrink-0 items-center gap-2 rounded-lg px-3 text-sm transition-colors',
              active
                ? 'bg-primary/[0.08] font-medium text-primary-emphasis'
                : 'text-muted-foreground hover:bg-muted hover:text-foreground',
            )}
          >
            {item.label}
            {loading ? (
              <Skeleton className="h-3.5 w-5" />
            ) : (
              item.count !== undefined && <span className="tabular-nums">{formatNumber(item.count)}</span>
            )}
          </button>
        )
      })}
    </div>
  )
}
