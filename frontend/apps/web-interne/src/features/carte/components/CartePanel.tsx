import { ChevronUp } from 'lucide-react'
import { useState } from 'react'
import {
  ETAT_MATERIEL,
  labelOf,
  optionsOf,
  type CategorieMateriel,
  type EtatMateriel,
} from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { EmptyState } from '@/shared/ui/empty-state'
import { FilterChip } from '@/shared/ui/filter-bar'
import { Skeleton } from '@/shared/ui/skeleton'
import { effectifGroupe, type CarteGroupe } from '../lib/grouping'
import { ETAT_BG } from '../lib/tiles'

export interface CarteFiltresState {
  readonly categorie: CategorieMateriel | null
  readonly etat: EtatMateriel | null
}

export interface CartePanelProps {
  groupes: readonly CarteGroupe[]
  loading: boolean
  filtres: CarteFiltresState
  onFiltresChange: (filtres: CarteFiltresState) => void
  selectedKey: string | null
  onSelect: (key: string) => void
}

function Legend() {
  return (
    <div className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground" aria-label="Légende">
      {ETAT_MATERIEL.map((etat) => (
        <span key={etat} className="inline-flex items-center gap-1.5">
          <span className={cn('size-2 rounded-full', ETAT_BG[etat])} aria-hidden />
          {labelOf('etatMateriel', etat)}
        </span>
      ))}
    </div>
  )
}

/** Panneau lateral (desktop) ou feuille basse repliable (mobile) : filtres, legende et points. */
export function CartePanel({
  groupes,
  loading,
  filtres,
  onFiltresChange,
  selectedKey,
  onSelect,
}: CartePanelProps) {
  const [expanded, setExpanded] = useState(false)
  const total = groupes.reduce((sum, groupe) => sum + groupe.materiels.length, 0)
  const chantiers = groupes.filter((groupe) => groupe.chantier).length
  return (
    <aside
      aria-label="Points de la carte"
      className={cn(
        'flex flex-col overflow-hidden border-border bg-surface',
        'max-md:absolute max-md:inset-x-0 max-md:bottom-0 max-md:z-[1000] max-md:rounded-t-2xl max-md:border-t max-md:shadow-overlay',
        'md:w-80 md:shrink-0 md:border-l lg:w-96',
        expanded ? 'max-md:max-h-[70%]' : 'max-md:max-h-[8.5rem]',
      )}
    >
      <button
        type="button"
        className="flex items-center justify-between gap-2 px-4 pb-1 pt-3 text-left md:pointer-events-none"
        aria-expanded={expanded}
        onClick={() => {
          setExpanded((value) => !value)
        }}
      >
        <span className="text-sm font-semibold">
          {loading
            ? 'Chargement…'
            : `${formatNumber(chantiers)} chantiers · ${formatNumber(total)} matériels`}
        </span>
        <ChevronUp
          className={cn(
            'size-4 text-muted-foreground transition-transform md:hidden',
            expanded && 'rotate-180',
          )}
          aria-hidden
        />
      </button>
      <div className="grid gap-3 px-4 pb-3 pt-1">
        <div className="flex flex-wrap gap-2">
          <FilterChip
            label="Catégorie"
            options={optionsOf('categorieMateriel')}
            value={filtres.categorie ? [filtres.categorie] : []}
            onChange={(values) => {
              onFiltresChange({ ...filtres, categorie: values.at(-1) ?? null })
            }}
          />
          <FilterChip
            label="État"
            options={optionsOf('etatMateriel')}
            value={filtres.etat ? [filtres.etat] : []}
            onChange={(values) => {
              onFiltresChange({ ...filtres, etat: values.at(-1) ?? null })
            }}
          />
        </div>
        <Legend />
      </div>
      <ul className="scrollbar-thin min-h-0 flex-1 overflow-y-auto border-t border-border p-2">
        {loading &&
          Array.from({ length: 5 }, (_, index) => (
            <li key={index} className="p-2">
              <Skeleton className="h-10 w-full" />
            </li>
          ))}
        {!loading && groupes.length === 0 && (
          <li>
            <EmptyState size="compact" title="Aucun point à afficher" />
          </li>
        )}
        {groupes.map((groupe) => {
          const titre =
            groupe.chantier?.libelle ??
            (groupe.materiels.length === 1 ? groupe.materiels[0]?.libelle : 'Matériels hors chantier')
          return (
            <li key={groupe.key}>
              <button
                type="button"
                aria-current={selectedKey === groupe.key || undefined}
                onClick={() => {
                  onSelect(groupe.key)
                  setExpanded(false)
                }}
                className={cn(
                  'flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-muted',
                  selectedKey === groupe.key && 'bg-primary/[0.08] font-medium text-primary-emphasis hover:bg-primary/10',
                )}
              >
                <span className="min-w-0 flex-1 truncate text-sm">{titre}</span>
                <span className="text-xs tabular-nums text-muted-foreground">{effectifGroupe(groupe)}</span>
              </button>
            </li>
          )
        })}
      </ul>
    </aside>
  )
}
