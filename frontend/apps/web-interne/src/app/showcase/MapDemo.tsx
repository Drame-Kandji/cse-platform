import 'leaflet/dist/leaflet.css'
import { CircleMarker, MapContainer, TileLayer, Tooltip } from 'react-leaflet'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { chartColor } from '@/shared/ui/chart-theme'

const SITES = [
  { name: 'Autoroute Diamniadio – AIBD', position: [14.72, -17.18] as [number, number], engins: 18 },
  { name: 'TER phase 2', position: [14.76, -17.37] as [number, number], engins: 11 },
  { name: 'Port de Saint-Louis', position: [16.03, -16.5] as [number, number], engins: 7 },
  { name: 'Pont de Kaolack', position: [14.15, -16.07] as [number, number], engins: 9 },
]

/** Exemple react-leaflet (CircleMarker : pas d'icones images a charger). */
export function MapDemo() {
  return (
    <Card className="overflow-hidden">
      <CardHeader
        title="Chantiers actifs"
        description="react-leaflet — tuiles OpenStreetMap (chargées uniquement sur cette page)"
      />
      <CardContent>
        <div className="h-80 overflow-hidden rounded-lg border border-border">
          <MapContainer center={[14.9, -16.9]} zoom={7} scrollWheelZoom={false} className="size-full">
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">contributeurs OpenStreetMap</a>'
              url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {SITES.map((site) => (
              <CircleMarker
                key={site.name}
                center={site.position}
                radius={6 + site.engins / 3}
                pathOptions={{ color: 'white', weight: 2, fillColor: chartColor(0), fillOpacity: 0.9 }}
              >
                <Tooltip>{`${site.name} — ${String(site.engins)} engins`}</Tooltip>
              </CircleMarker>
            ))}
          </MapContainer>
        </div>
      </CardContent>
    </Card>
  )
}
