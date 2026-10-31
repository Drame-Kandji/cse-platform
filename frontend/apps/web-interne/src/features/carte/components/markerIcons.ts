import { divIcon, type DivIcon } from 'leaflet'
import type { EtatMateriel } from '@/shared/domain/labels'
import { ETAT_BG, ETAT_TEXT } from '../lib/tiles'

const BASE =
  'grid place-items-center border-2 border-surface font-semibold tabular-nums shadow-overlay transition-transform'

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (char) => `&#${String(char.charCodeAt(0))};`)
}

/**
 * Marqueur de chantier : carre arrondi (forme distincte des engins) dimensionne par l'effectif,
 * libelle = nombre de materiels. Les chantiers termines sont grises.
 */
export function chantierIcon(options: {
  size: number
  count: number
  termine: boolean
  selected: boolean
}): DivIcon {
  const { size, count, termine, selected } = options
  const tone = termine ? 'bg-muted-foreground text-surface' : 'bg-primary text-primary-foreground'
  const ring = selected ? ' ring-4 ring-ring/40 scale-110' : ''
  return divIcon({
    className: 'cse-map-marker',
    html: `<span class="${BASE} ${tone} rounded-lg text-xs${ring}" style="width:${String(size)}px;height:${String(size)}px">${String(count)}</span>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2],
  })
}

/** Agregat de plusieurs engins hors chantier au meme point : cercle colore par l'etat dominant. */
export function clusterIcon(options: { count: number; etat: EtatMateriel | null; label: string }): DivIcon {
  const size = Math.min(44, 24 + options.count * 2)
  const bg = options.etat
    ? `${ETAT_BG[options.etat]} ${ETAT_TEXT[options.etat]}`
    : 'bg-muted-foreground text-surface'
  return divIcon({
    className: 'cse-map-marker',
    html: `<span class="${BASE} ${bg} rounded-full text-2xs" style="width:${String(size)}px;height:${String(size)}px" title="${escapeHtml(options.label)}">${String(options.count)}</span>`,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2],
  })
}
