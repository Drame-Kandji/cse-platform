import type { ColumnDef, PaginationState, SortingState } from '@tanstack/react-table'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import type { PageResponse } from '@/shared/api/types'
import { renderWithProviders } from '@/test/render'
import { DataTable } from './DataTable'

interface Row {
  id: string
  code: string
}

const ROWS: Row[] = Array.from({ length: 45 }, (_, index) => ({
  id: String(index + 1),
  code: `MAT-${String(index + 1).padStart(2, '0')}`,
}))
const columns: ColumnDef<Row>[] = [{ accessorKey: 'code', header: 'Code', enableSorting: true }]

function toPage(pagination: PaginationState): PageResponse<Row> {
  const { pageIndex, pageSize } = pagination
  const totalPages = Math.ceil(ROWS.length / pageSize)
  return {
    content: ROWS.slice(pageIndex * pageSize, (pageIndex + 1) * pageSize),
    page: pageIndex,
    size: pageSize,
    totalElements: ROWS.length,
    totalPages,
    first: pageIndex === 0,
    last: pageIndex === totalPages - 1,
  }
}

function ServerTable({ onSortingChange }: { onSortingChange?: (sorting: SortingState) => void }) {
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 })
  const [sorting, setSorting] = useState<SortingState>([])
  return (
    <DataTable
      label="Materiels"
      columns={columns}
      data={toPage(pagination)}
      pagination={pagination}
      onPaginationChange={setPagination}
      sorting={sorting}
      onSortingChange={(updater) => {
        const next = typeof updater === 'function' ? updater(sorting) : updater
        setSorting(next)
        onSortingChange?.(next)
      }}
    />
  )
}

describe('DataTable (pagination serveur)', () => {
  it('affiche la plage et navigue entre les pages', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ServerTable />)
    const table = screen.getByRole('table', { name: 'Materiels' })
    expect(within(table).getAllByRole('row')).toHaveLength(21)
    expect(screen.getByText('1-20 sur 45')).toBeInTheDocument()
    expect(screen.getByText('Page 1 sur 3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Page précédente' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Page suivante' }))
    expect(screen.getByText('21-40 sur 45')).toBeInTheDocument()
    expect(screen.getByText('MAT-21')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Dernière page' }))
    expect(screen.getByText('41-45 sur 45')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Page suivante' })).toBeDisabled()
    expect(within(table).getAllByRole('row')).toHaveLength(6)
  })

  it('remonte le tri au parent (tri serveur)', async () => {
    const user = userEvent.setup()
    const onSortingChange = vi.fn()
    renderWithProviders(<ServerTable onSortingChange={onSortingChange} />)
    await user.click(screen.getByRole('button', { name: /Code/ }))
    expect(onSortingChange).toHaveBeenLastCalledWith([{ id: 'code', desc: false }])
    expect(screen.getByRole('columnheader', { name: /Code/ })).toHaveAttribute('aria-sort', 'ascending')
  })

  it('declenche onRowClick au clic et au clavier', async () => {
    const user = userEvent.setup()
    const onRowClick = vi.fn()
    renderWithProviders(
      <DataTable label="T" columns={columns} data={ROWS.slice(0, 3)} onRowClick={onRowClick} />,
    )
    await user.click(screen.getByText('MAT-02'))
    expect(onRowClick).toHaveBeenCalledWith(ROWS[1])
    screen.getByText('MAT-03').closest('tr')?.focus()
    await user.keyboard('{Enter}')
    expect(onRowClick).toHaveBeenLastCalledWith(ROWS[2])
  })

  it('affiche des lignes squelettes pendant le chargement', () => {
    renderWithProviders(<DataTable label="T" columns={columns} data={undefined} isLoading />)
    const table = screen.getByRole('table', { name: 'T' })
    expect(table).toHaveAttribute('aria-busy', 'true')
    expect(within(table).getAllByRole('row')).toHaveLength(9)
  })

  it('affiche un etat vide sans pagination', () => {
    renderWithProviders(<DataTable label="T" columns={columns} data={[]} />)
    expect(screen.getByText('Aucun résultat')).toBeInTheDocument()
    expect(screen.queryByText(/sur 0/)).not.toBeInTheDocument()
  })
})
