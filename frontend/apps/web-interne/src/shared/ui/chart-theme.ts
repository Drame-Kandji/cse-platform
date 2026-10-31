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
