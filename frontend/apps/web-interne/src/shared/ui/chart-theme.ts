/**
 * Palette categorielle (ordre valide daltonisme : ne pas reordonner ni cycler).
 * Au-dela de 6 series, regrouper en "Autres" ou decouper en petits multiples.
 */
export const CHART_COLORS = [1, 2, 3, 4, 5, 6].map((index) => `rgb(var(--chart-${index}))`)

/** Couleur de la serie n (0-based), bornee a la palette. */
export function chartColor(index: number): string {
  return CHART_COLORS[Math.min(index, CHART_COLORS.length - 1)] ?? 'rgb(var(--chart-1))'
}

/** Props d'axe sobres (a etaler sur XAxis / YAxis). */
export const axisProps = {
  stroke: 'rgb(var(--border-strong))',
  tick: { fill: 'rgb(var(--muted-foreground))', fontSize: 12 },
  tickLine: false,
  axisLine: false,
  tickMargin: 8,
} as const

/** Grille recessive horizontale (a etaler sur CartesianGrid). */
export const gridProps = { stroke: 'rgb(var(--border))', strokeDasharray: '3 3', vertical: false } as const

/** Curseur de survol discret (Tooltip `cursor`). */
export const cursorProps = {
  stroke: 'rgb(var(--border-strong))',
  strokeWidth: 1,
  fill: 'rgb(var(--muted) / 0.6)',
} as const

/** Marges standard d'un graphique cartesien (l'axe Y porte sa propre largeur, voir `yAxisWidth`). */
export const chartMargin = { top: 8, right: 12, left: 4, bottom: 0 } as const

/** Largeur moyenne (px) d'un caractere des graduations (12 px, chiffres tabulaires) : estimation prudente. */
const TICK_CHAR_WIDTH = 7.4

/**
 * Largeur d'axe Y suffisante pour afficher les libelles donnes sans troncature
 * (Recharts coupe silencieusement les graduations trop larges, ex. "100 %" affiche "00 %").
 * @example <YAxis {...axisProps} width={yAxisWidth(['100 %'])} />
 */
export function yAxisWidth(labels: readonly string[]): number {
  const longest = labels.reduce((max, label) => Math.max(max, label.length), 1)
  return Math.ceil(longest * TICK_CHAR_WIDTH + axisProps.tickMargin + 6)
}

/**
 * Largeur d'axe Y pour des valeurs formatees : borne haute arrondie a la puissance de 10 superieure
 * (les graduations "rondes" de Recharts peuvent depasser le maximum des donnees).
 * @example <YAxis width={yAxisWidthFor(data.map((d) => d.montant), compactMoney)} tickFormatter={compactMoney} />
 */
export function yAxisWidthFor(values: readonly number[], format: (value: number) => string): number {
  const finite = values.filter((value) => Number.isFinite(value))
  const max = Math.max(0, ...finite.map((value) => Math.abs(value)))
  const bound = max > 0 ? 10 ** Math.ceil(Math.log10(max)) : 0
  const hasNegative = finite.some((value) => value < 0)
  return yAxisWidth([format(bound), format(max), hasNegative ? format(-bound) : '0'])
}
