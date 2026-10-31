import type { OnChangeFn, PaginationState, SortingState } from '@tanstack/react-table'
import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS, type PageParams } from '@/shared/api/types'

export interface TableStateOptions {
  /** Tri initial, ex. [{ id: 'dateCreation', desc: true }]. */
  defaultSort?: SortingState
  defaultPageSize?: number
  /** Prefixe des parametres d'URL (plusieurs tableaux sur une page). */
  prefix?: string
}

export interface TableState {
  pagination: PaginationState
  sorting: SortingState
  onPaginationChange: OnChangeFn<PaginationState>
  onSortingChange: OnChangeFn<SortingState>
  /** Parametres prets a envoyer a l'API : { page, size, sort: "champ,desc" }. */
  pageParams: Required<Pick<PageParams, 'page' | 'size'>> & Pick<PageParams, 'sort'>
  reset: () => void
}

function parseSort(value: string | null, fallback: SortingState): SortingState {
  if (!value) return fallback
  const [id, direction] = value.split(',')
  return id ? [{ id, desc: direction === 'desc' }] : fallback
}

/**
 * Etat pagination + tri d'un DataTable serveur, synchronise avec l'URL
 * (?page=2&size=20&sort=dateCreation,desc) pour le partage de liens et le bouton Retour.
 * La page est 1-based dans l'URL, 0-based pour l'API.
 */
export function useTableState(options: TableStateOptions = {}): TableState {
  const { defaultSort = [], defaultPageSize = DEFAULT_PAGE_SIZE, prefix = '' } = options
  const [params, setParams] = useSearchParams()
  const keys = useMemo(
    () => ({ page: `${prefix}page`, size: `${prefix}size`, sort: `${prefix}sort` }),
    [prefix],
  )

  const sizeParam = Number(params.get(keys.size))
  const pageSize = (PAGE_SIZE_OPTIONS as readonly number[]).includes(sizeParam) ? sizeParam : defaultPageSize
  const pageIndex = Math.max(0, (Number(params.get(keys.page)) || 1) - 1)
  const sortParam = params.get(keys.sort)
  const sorting = useMemo(() => parseSort(sortParam, defaultSort), [sortParam, defaultSort])
  const pagination = useMemo(() => ({ pageIndex, pageSize }), [pageIndex, pageSize])

  const update = useCallback(
    (changes: Record<string, string | null>) => {
      setParams(
        (current) => {
          const next = new URLSearchParams(current)
          for (const [key, value] of Object.entries(changes)) {
            if (value === null) next.delete(key)
            else next.set(key, value)
          }
          return next
        },
        { replace: true },
      )
    },
    [setParams],
  )

  const onPaginationChange: OnChangeFn<PaginationState> = (updater) => {
    const next = typeof updater === 'function' ? updater(pagination) : updater
    update({
      [keys.page]: next.pageIndex > 0 ? String(next.pageIndex + 1) : null,
      [keys.size]: next.pageSize !== defaultPageSize ? String(next.pageSize) : null,
    })
  }
  const onSortingChange: OnChangeFn<SortingState> = (updater) => {
    const next = typeof updater === 'function' ? updater(sorting) : updater
    const first = next[0]
    update({ [keys.sort]: first ? `${first.id},${first.desc ? 'desc' : 'asc'}` : null, [keys.page]: null })
  }
  const first = sorting[0]
  return {
    pagination,
    sorting,
    onPaginationChange,
    onSortingChange,
    pageParams: {
      page: pageIndex,
      size: pageSize,
      sort: first ? `${first.id},${first.desc ? 'desc' : 'asc'}` : undefined,
    },
    reset: () => {
      update({ [keys.page]: null, [keys.size]: null, [keys.sort]: null })
    },
  }
}
