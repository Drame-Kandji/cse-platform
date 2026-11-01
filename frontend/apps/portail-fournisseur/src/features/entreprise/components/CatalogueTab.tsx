import { Package, Plus } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { labelOf } from '@/shared/domain/labels'
import { formatDate, formatMoney } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { useCatalogue } from '@/api/queries'
import type { LigneCatalogue } from '@/domain/types'
import { LigneDialog } from './LigneDialog'

/** Catalogue de prix : tableau, ajout et modification d'une ligne (clic). */
export function CatalogueTab() {
  const { t } = useTranslation('portail')
  const query = useCatalogue()
  const [edition, setEdition] = useState<{ ligne: LigneCatalogue | null } | null>(null)
  const columns = useMemo<ColumnDef<LigneCatalogue>[]>(
    () => [
      {
        accessorKey: 'designation',
        header: t('catalogue.columns.designation'),
        meta: { mobilePrimary: true },
        cell: ({ row }) => (
          <div className="grid min-w-0 max-w-md gap-0.5">
            <span className="font-medium">{row.original.designation}</span>
            <span className="font-mono text-xs text-muted-foreground">{row.original.reference}</span>
          </div>
        ),
      },
      {
        accessorKey: 'categorie',
        header: t('catalogue.columns.categorie'),
        meta: { hideOnMobile: true },
        cell: ({ row }) => labelOf('categorieMateriel', row.original.categorie),
      },
      {
        accessorKey: 'prixUnitaire',
        header: t('catalogue.columns.prix'),
        meta: { align: 'right', nowrap: true },
        cell: ({ row }) => (
          <span className="grid justify-items-end">
            {formatMoney(row.original.prixUnitaire)}
            <span className="text-xs text-muted-foreground">
              {t('catalogue.par', { unite: row.original.unite })}
            </span>
          </span>
        ),
      },
      {
        accessorKey: 'delaiLivraisonJours',
        header: t('catalogue.columns.delaiCourt'),
        meta: { align: 'right', nowrap: true },
        cell: ({ row }) => t('ao.delaiJours', { n: row.original.delaiLivraisonJours }),
      },
      {
        accessorKey: 'dateMaj',
        header: t('catalogue.columns.maj'),
        meta: { hideOnMobile: true, nowrap: true },
        cell: ({ row }) => <span className="tabular-nums">{formatDate(row.original.dateMaj)}</span>,
      },
    ],
    [t],
  )
  return (
    <>
      <DataTable
        enableColumnVisibility={false}
        label={t('entreprise.tabs.catalogue')}
        columns={columns}
        data={query.data}
        getRowId={(l) => l.id}
        isLoading={query.isPending}
        error={query.error}
        onRetry={() => void query.refetch()}
        onRowClick={(ligne) => {
          setEdition({ ligne })
        }}
        toolbar={
          <Button
            size="sm"
            leftIcon={<Plus />}
            onClick={() => {
              setEdition({ ligne: null })
            }}
          >
            {t('catalogue.ajouter')}
          </Button>
        }
        emptyState={<EmptyState icon={<Package aria-hidden />} title={t('catalogue.empty')} />}
      />
      <LigneDialog
        open={edition !== null}
        ligne={edition?.ligne ?? null}
        onOpenChange={(open) => {
          if (!open) setEdition(null)
        }}
      />
    </>
  )
}
