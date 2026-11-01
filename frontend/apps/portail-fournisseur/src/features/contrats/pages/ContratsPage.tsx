import { FileSignature } from 'lucide-react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { formatDate, formatMoney } from '@/shared/lib/format'
import { CountTabs } from '@/shared/ui/count-tabs'
import { DataTable, type ColumnDef } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { PageHeader } from '@/shared/ui/page-header'
import { useContrats } from '@/api/queries'
import { to } from '@/app/routes'
import { ContratBadge } from '@/domain/badges'
import { contratsOnglet, ONGLETS_CONTRAT, type OngletContrat } from '@/domain/contrats'
import type { Contrat } from '@/domain/types'

function lireOnglet(value: string | null): OngletContrat | null {
  return ONGLETS_CONTRAT.find((o) => o === value) ?? null
}

/** Contrats du fournisseur, par onglet de statut (À signer, En cours, Livrés, Clôturés, Tous). */
export function Component() {
  const { t } = useTranslation('portail')
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const onglet = lireOnglet(params.get('onglet'))
  const query = useContrats()
  const contrats = query.data?.content ?? []
  // Compteurs masques si la liste n'a pas pu etre chargee (pas de « 0 » trompeur).
  const compter = (n: number) => (query.isError ? undefined : n)

  const columns = useMemo<ColumnDef<Contrat>[]>(
    () => [
      {
        accessorKey: 'objet',
        header: t('contrats.columns.contrat'),
        meta: { mobilePrimary: true },
        cell: ({ row }) => (
          <div className="grid min-w-0 max-w-md gap-0.5">
            <span className="font-medium">{row.original.objet}</span>
            <span className="font-mono text-xs text-muted-foreground">{row.original.numero}</span>
          </div>
        ),
      },
      {
        accessorKey: 'montant',
        header: t('contrats.columns.montant'),
        meta: { align: 'right', nowrap: true },
        cell: ({ row }) => formatMoney(row.original.montant),
      },
      {
        accessorKey: 'dateLivraisonPrevue',
        header: t('contrats.columns.livraison'),
        meta: { hideOnMobile: true, nowrap: true },
        cell: ({ row }) => (
          <span className="tabular-nums">{formatDate(row.original.dateLivraisonPrevue)}</span>
        ),
      },
      {
        accessorKey: 'statut',
        header: t('contrats.columns.statut'),
        meta: { nowrap: true },
        cell: ({ row }) => <ContratBadge statut={row.original.statut} />,
      },
    ],
    [t],
  )

  return (
    <>
      <PageHeader title={t('nav.contrats')} />
      <CountTabs<OngletContrat>
        label={t('nav.contrats')}
        value={onglet}
        loading={query.isPending}
        items={[
          ...ONGLETS_CONTRAT.map((value) => ({
            value,
            label: t(`contrats.onglets.${value}`),
            count: compter(contratsOnglet(contrats, value).length),
          })),
          { value: null, label: t('contrats.onglets.TOUS'), count: compter(contrats.length) },
        ]}
        onChange={(value) => {
          setParams(value ? { onglet: value } : {}, { replace: true })
        }}
      />
      <DataTable
        enableColumnVisibility={false}
        label={t('nav.contrats')}
        columns={columns}
        data={contratsOnglet(contrats, onglet)}
        getRowId={(c) => c.id}
        isLoading={query.isPending}
        error={query.error}
        onRetry={() => void query.refetch()}
        onRowClick={(c) => {
          navigate(to.contratDetail(c.id))
        }}
        emptyState={<EmptyState icon={<FileSignature aria-hidden />} title={t('contrats.empty')} />}
      />
    </>
  )
}
