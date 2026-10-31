import { Command } from 'cmdk'
import { Check, ChevronsUpDown, Search, X } from 'lucide-react'
import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'
import { fieldClasses } from './input'
import { Popover, PopoverContent, PopoverTrigger } from './popover'
import { Spinner } from './skeleton'

export interface ComboboxOption<V extends string = string> {
  readonly value: V
  readonly label: string
  readonly description?: string
}

export interface ComboboxProps<V extends string = string> {
  options: readonly ComboboxOption<V>[]
  value: V | null | undefined
  onValueChange: (value: V | null) => void
  placeholder?: string
  searchPlaceholder?: string
  emptyText?: string
  /** Recherche serveur : desactive le filtre local et remonte la saisie. */
  onSearchChange?: (search: string) => void
  loading?: boolean
  clearable?: boolean
  disabled?: boolean
  id?: string
  className?: string
  'aria-invalid'?: boolean
  'aria-describedby'?: string
}

/**
 * Liste de choix avec recherche (chantiers, materiels, fournisseurs).
 * Filtrage local par defaut ; fournir `onSearchChange` pour une recherche serveur.
 */
export function Combobox<V extends string>({
  options,
  value,
  onValueChange,
  placeholder,
  searchPlaceholder,
  emptyText,
  onSearchChange,
  loading,
  clearable = true,
  disabled,
  id,
  className,
  ...aria
}: ComboboxProps<V>) {
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const listId = useId()
  const selected = options.find((option) => option.value === value)
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <div className={cn('relative w-full', className)}>
        <PopoverTrigger asChild>
          <button
            type="button"
            id={id}
            role="combobox"
            aria-controls={listId}
            aria-expanded={open}
            disabled={disabled}
            className={cn(
              fieldClasses,
              'flex h-10 items-center justify-between gap-2 px-3 text-left sm:h-9',
              clearable && selected && 'pr-14',
            )}
            {...aria}
          >
            <span className={cn('truncate', !selected && 'text-muted-foreground')}>
              {selected?.label ?? placeholder ?? t('select.placeholder')}
            </span>
            <ChevronsUpDown className="size-4 shrink-0 text-muted-foreground" aria-hidden />
          </button>
        </PopoverTrigger>
        {clearable && selected && !disabled && (
          <button
            type="button"
            onClick={() => {
              onValueChange(null)
            }}
            className="absolute right-8 top-1/2 grid size-6 -translate-y-1/2 place-items-center rounded text-muted-foreground hover:bg-muted hover:text-foreground"
            aria-label={t('actions.clear')}
          >
            <X className="size-3.5" aria-hidden />
          </button>
        )}
      </div>
      <PopoverContent className="w-[var(--radix-popover-trigger-width)] min-w-64">
        <Command shouldFilter={!onSearchChange} loop>
          <div className="flex items-center gap-2 border-b border-border px-3">
            <Search className="size-4 shrink-0 text-muted-foreground" aria-hidden />
            <Command.Input
              placeholder={searchPlaceholder ?? t('select.search')}
              onValueChange={onSearchChange}
              className="h-10 w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground focus-visible:ring-0 focus-visible:ring-offset-0"
            />
            {loading && <Spinner />}
          </div>
          <Command.List id={listId} className="scrollbar-thin max-h-64 overflow-y-auto p-1">
            <Command.Empty className="px-3 py-6 text-center text-sm text-muted-foreground">
              {emptyText ?? t('select.empty')}
            </Command.Empty>
            {options.map((option) => (
              <Command.Item
                key={option.value}
                value={option.value}
                keywords={[option.label, option.description ?? '']}
                onSelect={() => {
                  onValueChange(option.value)
                  setOpen(false)
                }}
                className="flex cursor-pointer items-center gap-2.5 rounded-md px-2.5 py-2 text-sm outline-none data-[selected=true]:bg-muted"
              >
                <Check
                  className={cn(
                    'size-4 shrink-0 text-primary-emphasis',
                    option.value === value ? 'opacity-100' : 'opacity-0',
                  )}
                  aria-hidden
                />
                <span className="grid min-w-0">
                  <span className="truncate">{option.label}</span>
                  {option.description && (
                    <span className="truncate text-xs text-muted-foreground">{option.description}</span>
                  )}
                </span>
              </Command.Item>
            ))}
          </Command.List>
        </Command>
      </PopoverContent>
    </Popover>
  )
}
