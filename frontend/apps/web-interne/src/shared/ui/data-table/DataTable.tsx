import {
  flexRender,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
  type ColumnDef,
  type OnChangeFn,
  type PaginationState,
  type SortingState,
  type VisibilityState,
} from '@tanstack/react-table'
import { useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { DEFAULT_PAGE_SIZE, type PageResponse } from '@/shared/api/types'
import { cn } from '@/shared/lib/cn'
import { BREAKPOINTS, useMediaQuery } from '@/shared/lib/hooks'
import { EmptyState } from '../empty-state'
import { ErrorState } from '../error-state'
import { ProgressBar, Skeleton } from '../skeleton'
import { alignClasses } from './column-meta'
import { DataTableColumnToggle } from './DataTableColumnToggle'
import { DataTableMobileList } from './DataTableMobileList'
import { DataTablePagination } from './DataTablePagination'
import { SortableHeader } from './SortableHeader'

export interface DataTableProps<T> {
  /** Colonnes typees `ColumnDef<T>[]` ; `enableSorting: true` uniquement sur les champs tries par l'API. */
  columns: ColumnDef<T>[]
  /** PageResponse (mode serveur) ou tableau complet (mode local : tri/pagination cote client). */
  data: PageResponse<T> | readonly T[] | undefined
  /** Etat controle (mode serveur) : utiliser `useTableState()`. */
  pagination?: PaginationState
  onPaginationChange?: OnChangeFn<PaginationState>
  sorting?: SortingState
  onSortingChange?: OnChangeFn<SortingState>
  getRowId?: (row: T) => string
  isLoading?: boolean
  /** Rechargement en arriere-plan (barre de progression fine). */
  isFetching?: boolean
  error?: unknown
  onRetry?: () => void
  onRowClick?: (row: T) => void
  /** Etat vide personnalise (sinon message generique). */
  emptyState?: ReactNode
  /** Contenu a gauche de la barre d'outils (FilterBar...). */
  toolbar?: ReactNode
  enableColumnVisibility?: boolean
  initialColumnVisibility?: VisibilityState
  /** Rendu personnalise des cartes mobiles. */
  mobileCard?: (row: T) => ReactNode
  /** Libelle accessible du tableau. */
  label: string
  className?: string
}

const EMPTY: never[] = []

function isPage<T>(data: DataTableProps<T>['data']): data is PageResponse<T> {
  return data !== undefined && !Array.isArray(data)
}

/**
 * Tableau de donnees standard (TanStack Table) : pagination serveur sur PageResponse,
 * tri, colonnes masquables, clic sur ligne, squelettes, etats vide/erreur, cartes sur mobile.
 */
export function DataTable<T>(props: DataTableProps<T>) {
  const {
    columns,
    data,
    getRowId,
    isLoading = false,
    isFetching = false,
    error,
    onRetry,
    onRowClick,
    emptyState,
    toolbar,
    label,
    className,
  } = props
  const { t } = useTranslation()
  const isDesktop = useMediaQuery(BREAKPOINTS.md)
  const server = isPage(data)
  const [localPagination, setLocalPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize: DEFAULT_PAGE_SIZE,
  })
  const [localSorting, setLocalSorting] = useState<SortingState>([])
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>(
    props.initialColumnVisibility ?? {},
  )
  const pagination = props.pagination ?? localPagination
  const sorting = props.sorting ?? localSorting
  const rows = server ? data.content : (data ?? EMPTY)

  const table = useReactTable<T>({
    data: rows as T[],
    columns,
    getRowId,
    state: { pagination, sorting, columnVisibility },
    defaultColumn: { enableSorting: false },
    onPaginationChange: props.onPaginationChange ?? setLocalPagination,
    onSortingChange: props.onSortingChange ?? setLocalSorting,
    onColumnVisibilityChange: setColumnVisibility,
    manualPagination: server,
    manualSorting: server,
    pageCount: server ? data.totalPages : undefined,
    rowCount: server ? data.totalElements : undefined,
    enableSortingRemoval: true,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: server ? undefined : getSortedRowModel(),
    getPaginationRowModel: server ? undefined : getPaginationRowModel(),
  })

  const totalElements = server ? data.totalElements : rows.length
  const skeletonCount = Math.min(pagination.pageSize, 8)
  const showEmpty = !isLoading && !error && table.getRowModel().rows.length === 0
  const visibleColumns = table.getVisibleLeafColumns()

  return (
    <div className={cn('overflow-hidden rounded-xl border border-border bg-surface shadow-card', className)}>
      {(toolbar ?? props.enableColumnVisibility !== false) && (
        <div className="flex flex-wrap items-center gap-2 border-b border-border px-4 py-3">
          <div className="min-w-0 flex-1">{toolbar}</div>
          {props.enableColumnVisibility !== false && isDesktop && <DataTableColumnToggle table={table} />}
        </div>
      )}
      <ProgressBar active={isFetching && !isLoading} className="-mb-0.5" />
      {error ? (
        <ErrorState error={error} onRetry={onRetry} />
      ) : showEmpty ? (
        (emptyState ?? (
          <EmptyState title={t('table.noResults')} description={t('table.noResultsDescription')} />
        ))
      ) : isDesktop ? (
        <div className="scrollbar-thin overflow-x-auto">
          <table
            className="w-full border-collapse text-sm"
            aria-label={label}
            aria-busy={isLoading || undefined}
          >
            <thead className="bg-surface-sunken/70">
              {table.getHeaderGroups().map((group) => (
                <tr key={group.id} className="border-b border-border">
                  {group.headers.map((header) => {
                    const meta = header.column.columnDef.meta
                    const sorted = header.column.getIsSorted()
                    return (
                      <th
                        key={header.id}
                        scope="col"
                        aria-sort={
                          sorted === 'asc' ? 'ascending' : sorted === 'desc' ? 'descending' : undefined
                        }
                        className={cn(
                          'h-10 whitespace-nowrap px-4 text-xs font-medium text-muted-foreground',
                          alignClasses[meta?.align ?? 'left'],
                          meta?.headerClassName,
                        )}
                      >
                        {header.isPlaceholder ? null : header.column.getCanSort() ? (
                          <SortableHeader column={header.column} align={meta?.align}>
                            {flexRender(header.column.columnDef.header, header.getContext())}
                          </SortableHeader>
                        ) : (
                          flexRender(header.column.columnDef.header, header.getContext())
                        )}
                      </th>
                    )
                  })}
                </tr>
              ))}
            </thead>
            <tbody>
              {isLoading
                ? Array.from({ length: skeletonCount }, (_, index) => (
                    <tr key={index} className="border-b border-border last:border-0">
                      {visibleColumns.map((column) => (
                        <td key={column.id} className="h-12 px-4">
                          <Skeleton className="h-4 w-full max-w-36" />
                        </td>
                      ))}
                    </tr>
                  ))
                : table.getRowModel().rows.map((row) => (
                    <tr
                      key={row.id}
                      onClick={
                        onRowClick
                          ? () => {
                              onRowClick(row.original)
                            }
                          : undefined
                      }
                      onKeyDown={
                        onRowClick
                          ? (event) => {
                              if (event.key === 'Enter') onRowClick(row.original)
                            }
                          : undefined
                      }
                      tabIndex={onRowClick ? 0 : undefined}
                      className={cn(
                        'border-b border-border transition-colors last:border-0 hover:bg-muted/40',
                        onRowClick &&
                          'cursor-pointer focus-visible:bg-muted/60 focus-visible:ring-inset focus-visible:ring-offset-0',
                      )}
                    >
                      {row.getVisibleCells().map((cell) => {
                        const meta = cell.column.columnDef.meta
                        return (
                          <td
                            key={cell.id}
                            className={cn(
                              'h-12 px-4 py-2',
                              alignClasses[meta?.align ?? 'left'],
                              meta?.align === 'right' && 'tabular-nums',
                              meta?.cellClassName,
                            )}
                          >
                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                          </td>
                        )
                      })}
                    </tr>
                  ))}
            </tbody>
          </table>
        </div>
      ) : (
        <DataTableMobileList
          table={table}
          loading={isLoading}
          skeletonCount={Math.min(skeletonCount, 4)}
          onRowClick={onRowClick}
          renderCard={props.mobileCard}
        />
      )}
      {!error && totalElements > 0 && (
        <DataTablePagination
          pageIndex={pagination.pageIndex}
          pageSize={pagination.pageSize}
          pageCount={table.getPageCount()}
          totalElements={totalElements}
          onPageChange={(pageIndex) => {
            table.setPageIndex(pageIndex)
          }}
          onPageSizeChange={(pageSize) => {
            table.setPagination({ pageIndex: 0, pageSize })
          }}
        />
      )}
    </div>
  )
}
