import { Command } from 'cmdk'
import { Check, PlusCircle, Search, SlidersHorizontal, X } from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'
import { useDebouncedValue } from '@/shared/lib/hooks'
import { Button } from './button'
import { Input } from './input'
import { Popover, PopoverContent, PopoverTrigger } from './popover'

export interface FilterBarProps {
  /** Valeur de recherche appliquee (source de verite, ex. URL). */
  search?: string
  /** Appele apres 300 ms d'inactivite de saisie. */
  onSearchChange?: (value: string) => void
  searchPlaceholder?: string
  /** Filtres essentiels, toujours visibles (1 ou 2 au plus). */
  children?: ReactNode
  /** Filtres secondaires, regroupes dans le menu "Filtres". */
  moreFilters?: ReactNode
  /** Nombre de filtres secondaires actifs (affiche sur le bouton "Filtres"). */
  moreCount?: number
  /** Affiche "Reinitialiser" si des filtres sont actifs. */
  onReset?: () => void
  canReset?: boolean
  /** Actions a droite (export, colonnes...). */
  actions?: ReactNode
  className?: string
}

/** Barre de filtres standard au-dessus d'un tableau : recherche + puces + reinitialisation. */
export function FilterBar({
  search = '',
  onSearchChange,
  searchPlaceholder,
  children,
  moreFilters,
  moreCount = 0,
  onReset,
  canReset,
  actions,
  className,
}: FilterBarProps) {
  const { t } = useTranslation()
  const [draft, setDraft] = useState(search)
  const debounced = useDebouncedValue(draft)
  useEffect(() => {
    setDraft(search)
  }, [search])
  useEffect(() => {
    if (debounced !== search) onSearchChange?.(debounced)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- on ne reagit qu'a la saisie stabilisee
  }, [debounced])

  return (
    <div className={cn('flex flex-col gap-3 lg:flex-row lg:items-center', className)}>
      <div className="flex flex-1 flex-wrap items-center gap-2">
        {onSearchChange && (
          <Input
            type="search"
            value={draft}
            onChange={(event) => {
              setDraft(event.target.value)
            }}
            placeholder={searchPlaceholder ?? t('filters.search')}
            aria-label={searchPlaceholder ?? t('filters.search')}
            leftIcon={<Search />}
            containerClassName="sm:w-72"
          />
        )}
        {children}
        {moreFilters && <FilterMore count={moreCount}>{moreFilters}</FilterMore>}
        {canReset && onReset && (
          <Button variant="ghost" size="sm" onClick={onReset} rightIcon={<X />}>
            {t('filters.reset')}
          </Button>
        )}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  )
}

/** Bouton "Filtres" ouvrant un panneau des filtres secondaires (empiles verticalement). */
export function FilterMore({ count = 0, children }: { count?: number; children: ReactNode }) {
  const { t } = useTranslation()
  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          type="button"
          aria-label={count > 0 ? t('filters.moreActive', { count }) : t('filters.more')}
          className={cn(
            'inline-flex h-10 items-center gap-2 rounded-lg border px-3 text-sm font-medium transition-colors sm:h-9',
            count > 0
              ? 'border-primary/30 bg-primary/5 text-primary-emphasis hover:bg-primary/10'
              : 'border-border-strong bg-surface text-foreground hover:bg-muted',
          )}
        >
          <SlidersHorizontal className="size-4" aria-hidden />
          {t('filters.more')}
          {count > 0 && (
            <span className="grid min-w-5 place-items-center rounded-full bg-primary px-1.5 text-2xs font-semibold leading-5 text-primary-foreground">
              {count}
            </span>
          )}
        </button>
      </PopoverTrigger>
      <PopoverContent className="grid w-[min(20rem,calc(100vw-2rem))] gap-3 p-4">
        {children}
      </PopoverContent>
    </Popover>
  )
}

export interface FilterChipProps<V extends string> {
  label: string
  options: readonly { value: V; label: string }[]
  value: readonly V[]
  onChange: (value: V[]) => void
  /** Recherche dans les options (vrai si plus de 7 options). */
  searchable?: boolean
}

/** Filtre multi-valeurs compact (statut, priorite, categorie). */
export function FilterChip<V extends string>({
  label,
  options,
  value,
  onChange,
  searchable,
}: FilterChipProps<V>) {
  const { t } = useTranslation()
  const selected = options.filter((option) => value.includes(option.value))
  const toggle = (option: V) => {
    onChange(value.includes(option) ? value.filter((item) => item !== option) : [...value, option])
  }
  const summary =
    selected.length === 0
      ? null
      : selected.length <= 2
        ? selected.map((option) => option.label).join(', ')
        : t('filters.selected', { count: selected.length })
  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          type="button"
          className={cn(
            'inline-flex h-10 items-center gap-1.5 rounded-lg border px-3 text-sm font-medium transition-colors sm:h-9',
            selected.length
              ? 'border-primary/30 bg-primary/5 text-foreground hover:bg-primary/10'
              : 'border-dashed border-border-strong text-muted-foreground hover:bg-muted hover:text-foreground',
          )}
        >
          <PlusCircle className={cn('size-4', selected.length && 'hidden')} aria-hidden />
          {label}
          {summary && (
            <>
              <span className="mx-0.5 h-4 w-px bg-border-strong" aria-hidden />
              <span className="max-w-40 truncate font-normal text-primary-emphasis">{summary}</span>
            </>
          )}
        </button>
      </PopoverTrigger>
      <PopoverContent className="w-60">
        <Command loop>
          {(searchable ?? options.length > 7) && (
            <Command.Input
              placeholder={t('filters.search')}
              className="h-10 w-full border-b border-border bg-transparent px-3 text-sm outline-none placeholder:text-muted-foreground focus-visible:ring-0 focus-visible:ring-offset-0"
            />
          )}
          <Command.List className="max-h-64 overflow-y-auto p-1">
            <Command.Empty className="px-3 py-4 text-center text-sm text-muted-foreground">
              {t('filters.noOption')}
            </Command.Empty>
            {options.map((option) => {
              const checked = value.includes(option.value)
              return (
                <Command.Item
                  key={option.value}
                  value={option.label}
                  onSelect={() => {
                    toggle(option.value)
                  }}
                  className="flex cursor-pointer items-center gap-2.5 rounded-md px-2.5 py-2 text-sm outline-none data-[selected=true]:bg-muted"
                >
                  <span
                    className={cn(
                      'grid size-4 place-items-center rounded border',
                      checked ? 'border-primary bg-primary text-primary-foreground' : 'border-input',
                    )}
                    aria-hidden
                  >
                    {checked && <Check className="size-3" strokeWidth={3} />}
                  </span>
                  {option.label}
                </Command.Item>
              )
            })}
          </Command.List>
          {selected.length > 0 && (
            <div className="border-t border-border p-1">
              <button
                type="button"
                onClick={() => {
                  onChange([])
                }}
                className="h-8 w-full rounded-md text-sm text-muted-foreground hover:bg-muted hover:text-foreground"
              >
                {t('filters.clearFilter')}
              </button>
            </div>
          )}
        </Command>
      </PopoverContent>
    </Popover>
  )
}
