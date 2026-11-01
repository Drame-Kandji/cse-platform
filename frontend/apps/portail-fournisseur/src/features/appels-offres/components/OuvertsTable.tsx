import { Gavel } from 'lucide-react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import type { UseQueryResult } from '@tanstack/react-query'
import type { PageResponse } from '@/shared/api/types'
import { labelOf } from '@/shared/domain/labels'
import { formatDate, formatNumber } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { to } from '@/app/routes'
import { Countdown } from '@/components/Countdown'
import { estInvite } from '@/domain/offres'
import type { AppelOffrePortail } from '@/domain/types'

/** Appels d'offres ouverts : reference, objet, categorie, quantite, date limite et decompte. */
export function OuvertsTable({ query }: { query: UseQueryResult<PageResponse<AppelOffrePortail>> }) {
  const { t } = useTranslation('portail')
  const navigate = useNavigate()
  const columns = useMemo<ColumnDef<AppelOffrePortail>[]>(
    () => [
      {
        accessorKey: 'objet',
        header: t('ao.columns.appel'),
        meta: { mobilePrimary: true },
        cell: ({ row }) => (
          <div className="grid min-w-0 max-w-md gap-0.5">
            <span className="flex flex-wrap items-center gap-2">
              <span className="font-medium">{row.original.objet}</span>
              {estInvite(row.original) && (
                <Badge tone="primary" size="sm">
                  {t('ao.invite')}
                </Badge>
              )}
              {row.original.offreDeposee && <Badge size="sm">{t('ao.deposee')}</Badge>}
            </span>
            <span className="font-mono text-xs text-muted-foreground">{row.original.reference}</span>
          </div>
        ),
      },
      {
        accessorKey: 'categorie',
        header: t('ao.columns.categorie'),
        cell: ({ row }) => (
          <span className="grid">
            {labelOf('categorieMateriel', row.original.categorie)}
            <span className="text-xs tabular-nums text-muted-foreground">
              {t('ao.quantite', { n: formatNumber(row.original.quantite) })}
            </span>
          </span>
        ),
      },
      {
        accessorKey: 'dateLimite',
        header: t('ao.columns.dateLimite'),
        meta: { nowrap: true },
        cell: ({ row }) => (
          <span className="grid gap-0.5 tabular-nums">
            {formatDate(row.original.dateLimite)}
            <Countdown dateLimite={row.original.dateLimite} />
          </span>
        ),
      },
    ],
    [t],
  )
  return (
    <DataTable
      enableColumnVisibility={false}
      label={t('ao.ouverts')}
      columns={columns}
      data={query.data?.content}
      getRowId={(ao) => ao.id}
      isLoading={query.isPending}
      error={query.error}
      onRetry={() => void query.refetch()}
      onRowClick={(ao) => {
        navigate(to.appelOffreDetail(ao.id))
      }}
      emptyState={<EmptyState icon={<Gavel aria-hidden />} title={t('ao.emptyOuverts')} />}
    />
  )
}
