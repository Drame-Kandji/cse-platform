import { Send } from 'lucide-react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import type { UseQueryResult } from '@tanstack/react-query'
import type { PageResponse } from '@/shared/api/types'
import { formatDate, formatMoney } from '@/shared/lib/format'
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { to } from '@/app/routes'
import { ResultatBadge } from '@/domain/badges'
import { resultatOffre } from '@/domain/offres'
import type { OffrePortail } from '@/domain/types'

/** Offres deposees : montant, delai, date de depot et resultat. */
export function MesOffresTable({ query }: { query: UseQueryResult<PageResponse<OffrePortail>> }) {
  const { t } = useTranslation('portail')
  const navigate = useNavigate()
  const columns = useMemo<ColumnDef<OffrePortail>[]>(
    () => [
      {
        accessorKey: 'appelOffreObjet',
        header: t('ao.columns.appel'),
        meta: { mobilePrimary: true },
        cell: ({ row }) => (
          <div className="grid min-w-0 max-w-md gap-0.5">
            <span className="font-medium">{row.original.appelOffreObjet}</span>
            <span className="font-mono text-xs text-muted-foreground">
              {row.original.appelOffreReference}
            </span>
          </div>
        ),
      },
      {
        accessorKey: 'montantPropose',
        header: t('ao.columns.montant'),
        meta: { align: 'right', nowrap: true },
        cell: ({ row }) => formatMoney(row.original.montantPropose),
      },
      {
        accessorKey: 'dateDepot',
        header: t('ao.columns.depot'),
        meta: { hideOnMobile: true, nowrap: true },
        cell: ({ row }) => (
          <span className="grid tabular-nums">
            {formatDate(row.original.dateDepot)}
            <span className="text-xs text-muted-foreground">
              {t('ao.delaiJours', { n: row.original.delaiLivraisonJours })}
            </span>
          </span>
        ),
      },
      {
        id: 'resultat',
        header: t('ao.columns.resultat'),
        meta: { nowrap: true },
        cell: ({ row }) => <ResultatBadge resultat={resultatOffre(row.original)} />,
      },
    ],
    [t],
  )
  return (
    <DataTable
      enableColumnVisibility={false}
      label={t('ao.mesOffres')}
      columns={columns}
      data={query.data?.content}
      getRowId={(o) => o.id}
      isLoading={query.isPending}
      error={query.error}
      onRetry={() => void query.refetch()}
      onRowClick={(o) => {
        navigate(to.appelOffreDetail(o.appelOffreId))
      }}
      emptyState={<EmptyState icon={<Send aria-hidden />} title={t('ao.emptyOffres')} />}
    />
  )
}
