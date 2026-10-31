import { Wrench } from 'lucide-react'
import { optionsOf } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { useChantierOptions } from '@/shared/suivi'
import { FilterBar, FilterChip } from '@/shared/ui/filter-bar'
import type { MaterielFiltresState } from '../hooks/useMaterielFiltres'
import { SingleFilter } from './SingleFilter'

export interface MaterielFiltersProps {
  state: MaterielFiltresState
  onChange: (changes: Partial<MaterielFiltresState>) => void
  onReset: () => void
  canReset: boolean
}

/** Barre de filtres du parc : recherche + maintenance due ; etat, categorie, chantier dans "Filtres". */
export function MaterielFilters({ state, onChange, onReset, canReset }: MaterielFiltersProps) {
  const chantiers = useChantierOptions()
  return (
    <FilterBar
      search={state.q}
      onSearchChange={(q) => {
        onChange({ q })
      }}
      searchPlaceholder="Code, immatriculation, désignation…"
      canReset={canReset}
      onReset={onReset}
      moreCount={[state.etat, state.categorie, state.chantierId].filter((value) => value !== null).length}
      moreFilters={
        <>
          <SingleFilter
            label="État"
            allLabel="Tous les états"
            options={optionsOf('etatMateriel')}
            value={state.etat}
            onChange={(etat) => {
              onChange({ etat })
            }}
          />
          <SingleFilter
            label="Catégorie"
            allLabel="Toutes catégories"
            options={optionsOf('categorieMateriel')}
            value={state.categorie}
            onChange={(categorie) => {
              onChange({ categorie })
            }}
          />
          <FilterChip
            label="Chantier"
            searchable
            options={chantiers.data ?? []}
            value={state.chantierId ? [state.chantierId] : []}
            onChange={(values) => {
              onChange({ chantierId: values.at(-1) ?? null })
            }}
          />
        </>
      }
    >
      <button
        type="button"
        aria-pressed={state.maintenanceDue}
        onClick={() => {
          onChange({ maintenanceDue: !state.maintenanceDue })
        }}
        className={cn(
          'inline-flex h-10 items-center gap-1.5 rounded-lg border px-3 text-sm font-medium transition-colors sm:h-9',
          state.maintenanceDue
            ? 'border-primary/30 bg-primary/5 text-primary-emphasis'
            : 'border-border-strong bg-surface text-foreground hover:bg-muted',
        )}
      >
        <Wrench className="size-4" aria-hidden />
        Maintenance due
      </button>
    </FilterBar>
  )
}
