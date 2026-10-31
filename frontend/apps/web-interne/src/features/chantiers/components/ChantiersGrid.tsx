import type { PageResponse } from '@/shared/api'
import { DataTablePagination } from '@/shared/ui/data-table'
import { Skeleton } from '@/shared/ui/skeleton'
import type { Chantier } from '../api/types'
import { ChantierCard } from './ChantierCard'

interface Props {
  page: PageResponse<Chantier> | undefined
  loading: boolean
  pageIndex: number
  pageSize: number
  onPageChange: (pageIndex: number) => void
  onPageSizeChange: (pageSize: number) => void
  onOpen: (chantier: Chantier) => void
}

/** Grille de cartes chantiers paginee cote serveur. */
export function ChantiersGrid({ page, loading, onOpen, ...pagination }: Props) {
  if (loading || !page) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {[0, 1, 2, 3, 4, 5].map((i) => (
          <Skeleton key={i} className="h-64 rounded-xl" />
        ))}
      </div>
    )
  }
  return (
    <div className="grid gap-4">
      <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {page.content.map((chantier) => (
          <li key={chantier.id}>
            <ChantierCard
              chantier={chantier}
              onOpen={() => {
                onOpen(chantier)
              }}
            />
          </li>
        ))}
      </ul>
      <div className="overflow-hidden rounded-xl border border-border bg-surface">
        <DataTablePagination {...pagination} pageCount={page.totalPages} totalElements={page.totalElements} />
      </div>
    </div>
  )
}
