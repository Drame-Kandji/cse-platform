import { Map as MapIcon, Plus, Truck } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { paths, to } from '@/app/routes'
import { Can } from '@/shared/auth/Can'
import { useMateriels } from '@/shared/suivi'
import { Button } from '@/shared/ui/button'
import { DataTable, useTableState } from '@/shared/ui/data-table'
import { EmptyState } from '@/shared/ui/empty-state'
import { PageHeader } from '@/shared/ui/page-header'
import { EtatSummary } from '../components/EtatSummary'
import { materielColumns } from '../components/materielColumns'
import { MaterielFilters } from '../components/MaterielFilters'
import { MaterielFormDialog } from '../components/MaterielFormDialog'
import { useMaterielFiltres } from '../hooks/useMaterielFiltres'
import { useEtatSummary } from '../hooks/useParcQueries'

const DEFAULT_SORT = [{ id: 'code', desc: false }]

/** Parc materiel : liste paginee serveur, filtres synchronises avec l'URL, synthese par etat. */
export function Component() {
  const navigate = useNavigate()
  const [creating, setCreating] = useState(false)
  const filtres = useMaterielFiltres()
  const table = useTableState({ defaultSort: DEFAULT_SORT })
  const query = useMateriels({ ...filtres.api, ...table.pageParams })
  const { etat: _etat, ...horsEtat } = filtres.api
  const summary = useEtatSummary(horsEtat)

  return (
    <>
      <PageHeader
        title="Parc matériel"
        description="Engins, outillage, informatique et consommables : état, localisation et usure."
        actions={
          <>
            <Button variant="secondary" leftIcon={<MapIcon />} asChild>
              <Link to={paths.carte}>Voir sur la carte</Link>
            </Button>
            <Can roles={['RESPONSABLE_MATERIEL']}>
              <Button
                leftIcon={<Plus />}
                onClick={() => {
                  setCreating(true)
                }}
              >
                Nouveau matériel
              </Button>
            </Can>
          </>
        }
      />
      <EtatSummary
        counts={summary.counts}
        total={summary.total}
        loading={summary.isPending}
        selected={filtres.state.etat}
        onToggle={(etat) => {
          filtres.update({ etat: filtres.state.etat === etat ? null : etat })
        }}
      />
      <DataTable
        label="Parc matériel"
        columns={materielColumns}
        data={query.data}
        getRowId={(materiel) => materiel.id}
        isLoading={query.isPending}
        isFetching={query.isFetching}
        error={query.error}
        onRetry={() => void query.refetch()}
        pagination={table.pagination}
        onPaginationChange={table.onPaginationChange}
        sorting={table.sorting}
        onSortingChange={table.onSortingChange}
        onRowClick={(materiel) => {
          navigate(to.materielDetail(materiel.id))
        }}
        toolbar={
          <MaterielFilters
            state={filtres.state}
            onChange={filtres.update}
            onReset={filtres.reset}
            canReset={filtres.active}
          />
        }
        emptyState={
          <EmptyState
            icon={<Truck aria-hidden />}
            title={filtres.active ? 'Aucun matériel ne correspond' : 'Le parc est vide'}
            description={
              filtres.active
                ? 'Élargissez les filtres ou modifiez la recherche.'
                : 'Les matériels enregistrés ou livrés apparaîtront ici.'
            }
            action={
              filtres.active ? (
                <Button variant="secondary" onClick={filtres.reset}>
                  Réinitialiser les filtres
                </Button>
              ) : undefined
            }
          />
        }
      />
      <MaterielFormDialog open={creating} onOpenChange={setCreating} />
    </>
  )
}
