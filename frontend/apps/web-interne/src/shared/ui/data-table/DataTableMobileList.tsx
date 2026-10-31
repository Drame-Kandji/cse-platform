import { flexRender, type Row, type Table } from '@tanstack/react-table'
import { ChevronRight } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '@/shared/lib/cn'
import { Skeleton } from '../skeleton'
import { columnLabel } from './column-meta'

interface MobileListProps<T> {
  table: Table<T>
  loading: boolean
  skeletonCount: number
  onRowClick?: (row: T) => void
  renderCard?: (row: T) => ReactNode
}

function DefaultCard<T>({ row }: { row: Row<T> }) {
  const cells = row.getVisibleCells().filter((cell) => !cell.column.columnDef.meta?.hideOnMobile)
  const primary = cells.find((cell) => cell.column.columnDef.meta?.mobilePrimary) ?? cells[0]
  const rest = cells.filter((cell) => cell !== primary)
  return (
    <div className="grid gap-3">
      {primary && (
        <div className="pr-6 text-sm font-medium">
          {flexRender(primary.column.columnDef.cell, primary.getContext())}
        </div>
      )}
      <dl className="grid grid-cols-2 gap-x-4 gap-y-2.5">
        {rest.map((cell) => (
          <div key={cell.id} className="min-w-0">
            <dt className="text-xs text-muted-foreground">
              {columnLabel(cell.column)}
            </dt>
            <dd className="mt-0.5 truncate text-sm">
              {flexRender(cell.column.columnDef.cell, cell.getContext())}
            </dd>
          </div>
        ))}
      </dl>
    </div>
  )
}

/** Vue "cartes empilees" du DataTable sous 768px. */
export function DataTableMobileList<T>({
  table,
  loading,
  skeletonCount,
  onRowClick,
  renderCard,
}: MobileListProps<T>) {
  if (loading) {
    return (
      <ul className="divide-y divide-border">
        {Array.from({ length: skeletonCount }, (_, index) => (
          <li key={index} className="grid gap-3 p-4">
            <Skeleton className="h-4 w-2/3" />
            <div className="grid grid-cols-2 gap-3">
              <Skeleton className="h-8" />
              <Skeleton className="h-8" />
            </div>
          </li>
        ))}
      </ul>
    )
  }
  return (
    <ul className="divide-y divide-border">
      {table.getRowModel().rows.map((row) => {
        const content = renderCard ? renderCard(row.original) : <DefaultCard row={row} />
        return (
          <li key={row.id} className="relative">
            {onRowClick ? (
              <button
                type="button"
                onClick={() => {
                  onRowClick(row.original)
                }}
                className={cn(
                  'block w-full p-4 text-left transition-colors hover:bg-muted/50 focus-visible:ring-inset focus-visible:ring-offset-0',
                )}
              >
                {content}
                <ChevronRight className="absolute right-3 top-4 size-4 text-muted-foreground" aria-hidden />
              </button>
            ) : (
              <div className="p-4">{content}</div>
            )}
          </li>
        )
      })}
    </ul>
  )
}
