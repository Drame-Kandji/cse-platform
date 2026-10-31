import { ChevronLeft, ChevronRight } from 'lucide-react'
import { optionsOf, type CategorieMateriel } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { useChantierOptions } from '@/shared/suivi'
import { Button, IconButton } from '@/shared/ui/button'
import { FilterChip } from '@/shared/ui/filter-bar'
import { libellePeriode, ZOOMS, type Periode, type Zoom } from '../lib/layout'

const ZOOM_LABELS: Readonly<Record<Zoom, string>> = {
  semaine: 'Semaine',
  mois: 'Mois',
  trimestre: 'Trimestre',
}

export interface PlanningToolbarProps {
  zoom: Zoom
  periode: Periode
  chantierId: string | null
  categorie: CategorieMateriel | null
  onZoomChange: (zoom: Zoom) => void
  onNavigate: (sens: number) => void
  onToday: () => void
  onChantierChange: (id: string | null) => void
  onCategorieChange: (categorie: CategorieMateriel | null) => void
}

/** Barre d'outils du planning : navigation temporelle, zoom et filtres. */
export function PlanningToolbar(props: PlanningToolbarProps) {
  const { zoom, periode, chantierId, categorie } = props
  const chantiers = useChantierOptions()
  return (
    <div className="flex flex-col gap-3 border-b border-border px-4 py-3 xl:flex-row xl:items-center xl:justify-between">
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-1">
          <IconButton
            label="Période précédente"
            icon={<ChevronLeft />}
            onClick={() => {
              props.onNavigate(-1)
            }}
          />
          <IconButton
            label="Période suivante"
            icon={<ChevronRight />}
            onClick={() => {
              props.onNavigate(1)
            }}
          />
          <Button variant="ghost" onClick={props.onToday}>
            Aujourd&apos;hui
          </Button>
        </div>
        <h2 className="min-w-40 text-sm font-semibold capitalize tabular-nums" aria-live="polite">
          {libellePeriode(zoom, periode)}
        </h2>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <FilterChip
          label="Chantier"
          searchable
          options={chantiers.data ?? []}
          value={chantierId ? [chantierId] : []}
          onChange={(values) => {
            props.onChantierChange(values.at(-1) ?? null)
          }}
        />
        <FilterChip
          label="Catégorie"
          options={optionsOf('categorieMateriel')}
          value={categorie ? [categorie] : []}
          onChange={(values) => {
            props.onCategorieChange(values.at(-1) ?? null)
          }}
        />
        <div
          role="group"
          aria-label="Échelle"
          className="inline-flex gap-0.5"
        >
          {ZOOMS.map((value) => (
            <button
              key={value}
              type="button"
              aria-pressed={zoom === value}
              onClick={() => {
                props.onZoomChange(value)
              }}
              className={cn(
                'h-9 rounded-lg px-3 text-sm transition-colors',
                zoom === value
                  ? 'bg-primary/[0.08] font-medium text-primary-emphasis'
                  : 'text-muted-foreground hover:bg-muted hover:text-foreground',
              )}
            >
              {ZOOM_LABELS[value]}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
