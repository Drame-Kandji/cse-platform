import 'leaflet/dist/leaflet.css'
import './carte.css'
import type { Map as LeafletMap, Marker as LeafletMarker, CircleMarker as LeafletCircle } from 'leaflet'
import { Crosshair } from 'lucide-react'
import { useContext, useEffect, useRef, useState } from 'react'
import { CircleMarker, MapContainer, Marker, Popup, TileLayer } from 'react-leaflet'
import { labelOf } from '@/shared/domain/labels'
import { ThemeContext } from '@/shared/theme/theme-context'
import { IconButton } from '@/shared/ui/button'
import {
  effectifGroupe,
  etatDominant,
  SENEGAL_BOUNDS,
  tailleMarqueur,
  type CarteGroupe,
} from '../lib/grouping'
import { ETAT_FILL, TILES } from '../lib/tiles'
import { GroupePopup } from './GroupePopup'
import { chantierIcon, clusterIcon } from './markerIcons'

export interface CarteMapProps {
  groupes: readonly CarteGroupe[]
  selectedKey: string | null
  onSelect: (key: string | null) => void
  /** Fermeture de l'infobulle d'un point (le parent ne deselectionne que s'il est encore actif). */
  onPopupClose: (key: string) => void
}

type Layer = LeafletMarker | LeafletCircle

/** Carte Leaflet du parc : chantiers (carres dimensionnes), engins isoles (cercles par etat), agregats. */
export function CarteMap({ groupes, selectedKey, onSelect, onPopupClose }: CarteMapProps) {
  const theme = useContext(ThemeContext)?.resolvedTheme ?? 'light'
  const tiles = TILES[theme]
  const [map, setMap] = useState<LeafletMap | null>(null)
  const layers = useRef(new Map<string, Layer>())

  useEffect(() => {
    if (!map || !selectedKey) return
    const groupe = groupes.find((item) => item.key === selectedKey)
    if (!groupe) return
    map.flyTo([groupe.lat, groupe.lng], Math.max(map.getZoom(), 10), { duration: 0.6 })
    layers.current.get(selectedKey)?.openPopup()
  }, [map, selectedKey, groupes])

  const register = (key: string) => (layer: Layer | null) => {
    if (layer) layers.current.set(key, layer)
    else layers.current.delete(key)
  }
  const handlers = (key: string) => ({
    click: () => {
      onSelect(key)
    },
    popupclose: () => {
      onPopupClose(key)
    },
  })

  return (
    <div className="cse-carte relative isolate size-full">
      <MapContainer
        ref={setMap}
        bounds={SENEGAL_BOUNDS}
        minZoom={6}
        maxZoom={18}
        className="size-full"
        zoomControl
      >
        <TileLayer
          key={theme}
          url={tiles.url}
          attribution={tiles.attribution}
          subdomains={tiles.subdomains}
        />
        {groupes.map((groupe) => {
          const position: [number, number] = [groupe.lat, groupe.lng]
          const popup = (
            <Popup>
              <GroupePopup groupe={groupe} />
            </Popup>
          )
          if (groupe.chantier) {
            const count = effectifGroupe(groupe)
            return (
              <Marker
                key={groupe.key}
                ref={register(groupe.key)}
                position={position}
                title={`${groupe.chantier.libelle} — ${String(count)} matériel(s)`}
                icon={chantierIcon({
                  size: tailleMarqueur(count),
                  count,
                  termine: groupe.chantier.statut === 'TERMINE',
                  selected: selectedKey === groupe.key,
                })}
                eventHandlers={handlers(groupe.key)}
                zIndexOffset={selectedKey === groupe.key ? 1000 : 0}
              >
                {popup}
              </Marker>
            )
          }
          const [seul] = groupe.materiels
          if (groupe.materiels.length === 1 && seul) {
            return (
              <CircleMarker
                key={groupe.key}
                ref={register(groupe.key)}
                center={position}
                radius={selectedKey === groupe.key ? 10 : 7}
                pathOptions={{
                  className: `${seul.etat ? ETAT_FILL[seul.etat] : 'fill-muted-foreground'} stroke-surface`,
                  weight: 2,
                  fillOpacity: 0.95,
                }}
                eventHandlers={handlers(groupe.key)}
              >
                {popup}
              </CircleMarker>
            )
          }
          const label = `${String(groupe.materiels.length)} matériels hors chantier`
          const dominant = etatDominant(groupe)
          return (
            <Marker
              key={groupe.key}
              ref={register(groupe.key)}
              position={position}
              title={dominant ? `${label} (majorité ${labelOf('etatMateriel', dominant)})` : label}
              icon={clusterIcon({ count: groupe.materiels.length, etat: dominant, label })}
              eventHandlers={handlers(groupe.key)}
            >
              {popup}
            </Marker>
          )
        })}
      </MapContainer>
      <div className="absolute right-3 top-3 z-[1000]">
        <IconButton
          label="Recentrer sur le Sénégal"
          variant="secondary"
          icon={<Crosshair />}
          onClick={() => {
            onSelect(null)
            map?.closePopup()
            map?.fitBounds(SENEGAL_BOUNDS)
          }}
        />
      </div>
    </div>
  )
}
