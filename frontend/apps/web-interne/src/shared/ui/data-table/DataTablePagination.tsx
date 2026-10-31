import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { PAGE_SIZE_OPTIONS } from '@/shared/api/types'
import { formatNumber } from '@/shared/lib/format'
import { IconButton } from '../button'
import { Select } from '../select'

export interface DataTablePaginationProps {
  pageIndex: number
  pageSize: number
  pageCount: number
  totalElements: number
  onPageChange: (pageIndex: number) => void
  onPageSizeChange: (pageSize: number) => void
}

/** Pied de tableau : taille de page, plage affichee et navigation. */
export function DataTablePagination({
  pageIndex,
  pageSize,
  pageCount,
  totalElements,
  onPageChange,
  onPageSizeChange,
}: DataTablePaginationProps) {
  const { t } = useTranslation()
  const from = totalElements === 0 ? 0 : pageIndex * pageSize + 1
  const to = Math.min(totalElements, (pageIndex + 1) * pageSize)
  const lastIndex = Math.max(0, pageCount - 1)
  return (
    <div className="flex flex-col-reverse items-center justify-between gap-3 border-t border-border px-4 py-3 sm:flex-row">
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <span className="hidden sm:inline">{t('table.rowsPerPage')}</span>
        <Select
          aria-label={t('table.rowsPerPage')}
          className="h-8 w-[4.5rem] sm:h-8"
          value={String(pageSize)}
          options={PAGE_SIZE_OPTIONS.map((size) => ({ value: String(size), label: String(size) }))}
          onValueChange={(value) => {
            onPageSizeChange(Number(value))
          }}
        />
        <span className="tabular-nums" aria-live="polite">
          {t('table.range', {
            from: formatNumber(from),
            to: formatNumber(to),
            total: formatNumber(totalElements),
          })}
        </span>
      </div>
      <div className="flex items-center gap-1">
        <span className="mr-2 text-sm tabular-nums text-muted-foreground">
          {t('table.pageOf', { page: pageIndex + 1, total: Math.max(1, pageCount) })}
        </span>
        <IconButton
          size="icon-sm"
          variant="secondary"
          tooltip={false}
          label={t('table.firstPage')}
          icon={<ChevronsLeft />}
          disabled={pageIndex === 0}
          onClick={() => {
            onPageChange(0)
          }}
          className="hidden sm:inline-flex"
        />
        <IconButton
          size="icon-sm"
          variant="secondary"
          tooltip={false}
          label={t('table.previousPage')}
          icon={<ChevronLeft />}
          disabled={pageIndex === 0}
          onClick={() => {
            onPageChange(pageIndex - 1)
          }}
        />
        <IconButton
          size="icon-sm"
          variant="secondary"
          tooltip={false}
          label={t('table.nextPage')}
          icon={<ChevronRight />}
          disabled={pageIndex >= lastIndex}
          onClick={() => {
            onPageChange(pageIndex + 1)
          }}
        />
        <IconButton
          size="icon-sm"
          variant="secondary"
          tooltip={false}
          label={t('table.lastPage')}
          icon={<ChevronsRight />}
          disabled={pageIndex >= lastIndex}
          onClick={() => {
            onPageChange(lastIndex)
          }}
          className="hidden sm:inline-flex"
        />
      </div>
    </div>
  )
}
