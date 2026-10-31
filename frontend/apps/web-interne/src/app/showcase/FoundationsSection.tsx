import {
  formatDate,
  formatDateTime,
  formatFileSize,
  formatMoney,
  formatNumber,
  formatPercent,
  formatRelative,
} from '@/shared/lib/format'
import { Demo, Section } from './Section'

const SWATCHES = [
  ['background', 'bg-background'],
  ['surface', 'bg-surface'],
  ['surface-raised', 'bg-surface-raised'],
  ['surface-sunken', 'bg-surface-sunken'],
  ['muted', 'bg-muted'],
  ['border', 'bg-border'],
  ['foreground', 'bg-foreground'],
  ['primary', 'bg-primary'],
  ['accent', 'bg-accent'],
  ['success', 'bg-success'],
  ['warning', 'bg-warning'],
  ['danger', 'bg-danger'],
  ['info', 'bg-info'],
] as const

const CHARTS = ['bg-chart-1', 'bg-chart-2', 'bg-chart-3', 'bg-chart-4', 'bg-chart-5', 'bg-chart-6'] as const

const now = new Date()
const FORMATS: readonly [string, string][] = [
  ['formatMoney(1 250 000)', formatMoney(1_250_000)],
  ['formatMoney(3,48 Md, compact)', formatMoney(3_480_000_000, { compact: true })],
  ['formatNumber(12 345,678, 2 déc.)', formatNumber(12345.678, { decimals: 2 })],
  ['formatPercent(0,873)', formatPercent(0.873)],
  ['formatPercent(-0,042, signé)', formatPercent(-0.042, { signed: true })],
  ['formatDate', formatDate(now)],
  ['formatDateTime', formatDateTime(now)],
  ['formatRelative(-3 h)', formatRelative(new Date(now.getTime() - 3 * 3600_000))],
  ['formatFileSize(2 400 000)', formatFileSize(2_400_000)],
  ['valeur absente', formatMoney(null)],
]

/** Jetons de couleur, typographie et formateurs. */
export function FoundationsSection() {
  return (
    <Section
      id="fondations"
      title="Fondations"
      description="Jetons CSS (clair / sombre), typographie Inter auto-hébergée, formats fr-SN."
    >
      <Demo title="Couleurs sémantiques">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
          {SWATCHES.map(([name, className]) => (
            <div key={name} className="grid gap-1.5">
              <div className={`h-12 rounded-lg border border-border ${className}`} />
              <span className="font-mono text-2xs text-muted-foreground">{name}</span>
            </div>
          ))}
        </div>
      </Demo>
      <Demo title="Palette graphique (ordre fixe, validée daltonisme)">
        <div className="flex flex-wrap gap-3">
          {CHARTS.map((className, index) => (
            <div key={className} className="grid justify-items-center gap-1.5">
              <div className={`size-10 rounded-lg ${className}`} />
              <span className="font-mono text-2xs text-muted-foreground">chart-{index + 1}</span>
            </div>
          ))}
        </div>
      </Demo>
      <div className="grid grid-cols-[minmax(0,1fr)] gap-6 lg:grid-cols-2">
        <Demo title="Typographie">
          <div className="grid gap-2">
            <p className="text-2xl font-semibold tracking-tight">Titre de page · 24 px</p>
            <p className="text-lg font-semibold tracking-tight">Titre de section · 18 px</p>
            <p className="text-sm">
              Texte courant 14 px, interligne confortable pour la lecture sur chantier.
            </p>
            <p className="text-sm text-muted-foreground">
              Texte secondaire (muted-foreground), contraste AA.
            </p>
            <p className="text-2xl font-semibold tabular-nums">1 234 567 890 FCFA</p>
          </div>
        </Demo>
        <Demo title="Formateurs (shared/lib/format.ts)">
          <dl className="grid gap-1.5 text-sm">
            {FORMATS.map(([label, value]) => (
              <div key={label} className="flex justify-between gap-4">
                <dt className="font-mono text-xs text-muted-foreground">{label}</dt>
                <dd className="font-medium tabular-nums">{value}</dd>
              </div>
            ))}
          </dl>
        </Demo>
      </div>
    </Section>
  )
}
