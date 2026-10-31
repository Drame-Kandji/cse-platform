import type { Column } from '@tanstack/react-table'
import { ArrowDown, ArrowUp, ChevronsUpDown } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/cn'

/** En-tete cliquable : bascule tri croissant / decroissant / aucun. */
export function SortableHeader<T>({
  column,
  align,
  children,
}: {
  column: Column<T>
  align?: 'left' | 'center' | 'right'
  children: ReactNode
}) {
  const { t } = useTranslation()
  const sorted = column.getIsSorted()
  const Icon = sorted === 'asc' ? ArrowUp : sorted === 'desc' ? ArrowDown : ChevronsUpDown
  return (
    <button
      type="button"
      onClick={column.getToggleSortingHandler()}
      title={sorted === 'asc' ? t('table.sortDesc') : t('table.sortAsc')}
      className={cn(
        '-mx-1.5 inline-flex h-7 items-center gap-1 rounded-md px-1.5 transition-colors hover:bg-muted hover:text-foreground',
        sorted && 'text-foreground',
        align === 'right' && 'flex-row-reverse',
      )}
    >
      {children}
      <Icon className={cn('size-3.5', !sorted && 'opacity-50')} aria-hidden />
    </button>
  )
}
