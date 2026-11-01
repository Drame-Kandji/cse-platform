import { Plus, Receipt } from 'lucide-react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router-dom'
import { labelOf, STATUT_FACTURE, type StatutFacture } from '@/shared/domain/labels'
import { formatDate, formatMoney } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { CountTabs } from '@/shared/ui/count-tabs'
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { PageHeader } from '@/shared/ui/page-header'
import { StatusBadge } from '@/shared/ui/status-badge'
import { useContrats, useFactures } from '@/api/queries'
import { contratsFacturables } from '@/domain/factures'
import type { Facture } from '@/domain/types'
import { NouvelleFactureDialog } from '../components/NouvelleFactureDialog'

function lireStatut(value: string | null): StatutFacture | null {
  return STATUT_FACTURE.find((s) => s === value) ?? null
}

/** Factures : onglets de statut, montants TTC, motif de rejet ; depot via « Nouvelle facture ». */
export function Component() {
  const { t } = useTranslation('portail')
  const [params, setParams] = useSearchParams()
  const statut = lireStatut(params.get('statut'))
  const nouvelle = params.get('nouvelle')
  const query = useFactures()
  const contrats = useContrats()
  const factures = query.data?.content ?? []
  // Compteurs masques si la liste n'a pas pu etre chargee (pas de « 0 » trompeur).
  const compter = (n: number) => (query.isError ? undefined : n)

  const setParam = (key: string, value: string | null) => {
    setParams(
      (current) => {
        const next = new URLSearchParams(current)
        if (value === null) next.delete(key)
        else next.set(key, value)
        return next
      },
      { replace: true },
    )
  }

  const columns = useMemo<ColumnDef<Facture>[]>(
    () => [
      {
        accessorKey: 'numero',
        header: t('factures.columns.facture'),
        meta: { mobilePrimary: true },
        cell: ({ row }) => (
          <div className="grid min-w-0 gap-0.5">
            <span className="font-medium">{row.original.numero}</span>
            <span className="font-mono text-xs text-muted-foreground">{row.original.contratNumero}</span>
          </div>
        ),
      },
      {
        accessorKey: 'montantTtc',
        header: t('factures.columns.ttc'),
        meta: { align: 'right', nowrap: true },
        cell: ({ row }) => formatMoney(row.original.montantTtc),
      },
      {
        accessorKey: 'dateEmission',
        header: t('factures.columns.dates'),
        meta: { hideOnMobile: true, nowrap: true },
        cell: ({ row }) => (
          <span className="grid tabular-nums">
            {formatDate(row.original.dateEmission)}
            <span
              className={
                row.original.enRetard ? 'text-xs text-danger-emphasis' : 'text-xs text-muted-foreground'
              }
            >
              {t('factures.echeance', { date: formatDate(row.original.dateEcheance) })}
            </span>
          </span>
        ),
      },
      {
        accessorKey: 'statut',
        header: t('factures.columns.statut'),
        cell: ({ row }) => (
          <span className="grid justify-items-start gap-1">
            <StatusBadge kind="statutFacture" value={row.original.statut} />
            {row.original.statut === 'PAYEE' && row.original.datePaiement && (
              <span className="text-xs text-muted-foreground">
                {t('factures.payeeLe', { date: formatDate(row.original.datePaiement) })}
              </span>
            )}
            {row.original.motifRejet && (
              <span className="max-w-xs text-xs text-danger-emphasis">{row.original.motifRejet}</span>
            )}
          </span>
        ),
      },
    ],
    [t],
  )

  return (
    <>
      <PageHeader
        title={t('nav.factures')}
        actions={
          <Button
            leftIcon={<Plus />}
            onClick={() => {
              setParam('nouvelle', '')
            }}
          >
            {t('factures.nouvelle')}
          </Button>
        }
      />
      <CountTabs<StatutFacture>
        label={t('nav.factures')}
        value={statut}
        loading={query.isPending}
        items={[
          { value: null, label: t('factures.toutes'), count: compter(factures.length) },
          ...STATUT_FACTURE.map((value) => ({
            value,
            label: t(`factures.onglets.${value}`),
            count: compter(factures.filter((f) => f.statut === value).length),
          })),
        ]}
        onChange={(value) => {
          setParam('statut', value)
        }}
      />
      <DataTable
        enableColumnVisibility={false}
        label={t('nav.factures')}
        columns={columns}
        data={statut ? factures.filter((f) => f.statut === statut) : factures}
        getRowId={(f) => f.id}
        isLoading={query.isPending}
        error={query.error}
        onRetry={() => void query.refetch()}
        emptyState={
          <EmptyState
            icon={<Receipt aria-hidden />}
            title={
              statut
                ? t('factures.emptyStatut', { statut: labelOf('statutFacture', statut).toLowerCase() })
                : t('factures.empty')
            }
          />
        }
      />
      <NouvelleFactureDialog
        open={nouvelle !== null}
        onOpenChange={(open) => {
          if (!open) setParam('nouvelle', null)
        }}
        contrats={contratsFacturables(contrats.data?.content ?? [])}
        contratId={nouvelle ?? ''}
      />
    </>
  )
}
