import { useQuery } from '@tanstack/react-query'
import { BarChart3, FileSpreadsheet, FileText } from 'lucide-react'
import { reportingApi } from '../api/reporting.api'
import { reportingKeys } from '../api/reporting.keys'
import { ChantiersTable } from '../components/ChantiersTable'
import { KpiTables } from '../components/KpiTables'
import { PeriodSelector } from '../components/PeriodSelector'
import { ProfilSwitcher } from '../components/ProfilSwitcher'
import { SeriesExplorer } from '../components/SeriesExplorer'
import { useExportTableau, useTableau } from '../hooks/useReporting'
import { useReportingFilters } from '../hooks/useReportingFilters'
import { granulariteFor } from '../lib/period'
import { PROFIL_LABELS } from '../lib/profils'
import { seriesFor } from '../lib/series'
import { formatDate, formatDateTime } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Card } from '@/shared/ui/card'
import { Combobox } from '@/shared/ui/combobox'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { PageHeader } from '@/shared/ui/page-header'
import { ProgressBar, Skeleton } from '@/shared/ui/skeleton'
import type { ExportFormat } from '../types'

/** Rapports : filtres profil / periode / chantier, apercu des tables, exports PDF et Excel, series. */
export function Component() {
  const filters = useReportingFilters()
  const { profil, period, chantierId } = filters
  const params = { profil, du: period.du, au: period.au, chantierId }
  const query = useTableau(params, { enabled: profil !== undefined })
  const chantiers = useQuery({
    queryKey: reportingKeys.custom('chantiers'),
    queryFn: ({ signal }) => reportingApi.chantiers(signal),
    staleTime: 5 * 60_000,
  })
  const exporter = useExportTableau()
  const exportAs = (format: ExportFormat) => {
    exporter.mutate({ format, params })
  }
  const pendingFormat = exporter.isPending ? exporter.variables.format : undefined

  if (!profil) {
    return (
      <>
        <PageHeader title="Rapports" />
        <Card>
          <EmptyState
            icon={<BarChart3 aria-hidden />}
            title="Aucun rapport disponible"
            description="Votre rôle ne dispose d'aucun profil de reporting."
          />
        </Card>
      </>
    )
  }

  return (
    <>
      <PageHeader
        title="Rapports"
        description={`${PROFIL_LABELS[profil].vue} · du ${formatDate(period.du)} au ${formatDate(period.au)}`}
        actions={
          <>
            <Button
              variant="secondary"
              leftIcon={<FileText />}
              loading={pendingFormat === 'pdf'}
              disabled={exporter.isPending}
              onClick={() => {
                exportAs('pdf')
              }}
            >
              Exporter en PDF
            </Button>
            <Button
              variant="secondary"
              leftIcon={<FileSpreadsheet />}
              loading={pendingFormat === 'xlsx'}
              disabled={exporter.isPending}
              onClick={() => {
                exportAs('xlsx')
              }}
            >
              Exporter en Excel
            </Button>
          </>
        }
      />

      <Card className="mb-6 flex flex-col gap-3 p-4 lg:flex-row lg:flex-wrap lg:items-center">
        <ProfilSwitcher profils={filters.allowed} value={profil} onChange={filters.setProfil} />
        <PeriodSelector value={period} onChange={filters.setPeriod} />
        <Combobox
          className="lg:w-72"
          aria-describedby={undefined}
          placeholder="Tous les chantiers"
          searchPlaceholder="Rechercher un chantier…"
          emptyText="Aucun chantier"
          loading={chantiers.isPending}
          value={chantierId ?? null}
          onValueChange={(value) => {
            filters.setChantier(value)
          }}
          options={(chantiers.data?.content ?? []).map((chantier) => ({
            value: chantier.id,
            label: chantier.nom,
            description: chantier.code,
          }))}
        />
      </Card>

      {query.isPending ? (
        <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
          {[0, 1, 2].map((index) => (
            <Skeleton key={index} className="h-72 rounded-xl" />
          ))}
        </div>
      ) : query.isError ? (
        <Card>
          <ErrorState error={query.error} onRetry={() => void query.refetch()} />
        </Card>
      ) : (
        <div className="grid gap-6">
          <ProgressBar active={query.isFetching} className="-mb-6" />
          <p className="-mt-2 text-xs text-muted-foreground">
            Généré le {formatDateTime(query.data.genereLe)}
          </p>
          <KpiTables tableau={query.data} />
          {query.data.chantiers && query.data.chantiers.length > 0 && (
            <ChantiersTable chantiers={query.data.chantiers} />
          )}
          <SeriesExplorer
            indicateurs={seriesFor(query.data)}
            du={period.du}
            au={period.au}
            chantierId={chantierId}
            defaultGranularite={granulariteFor(period)}
          />
        </div>
      )}
    </>
  )
}
