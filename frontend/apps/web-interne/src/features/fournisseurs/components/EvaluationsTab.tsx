import type { StatutFournisseur } from '@/shared/domain/labels'
import { CartesianGrid, Line, LineChart, Tooltip, XAxis, YAxis } from 'recharts'
import { useTranslation } from 'react-i18next'
import { access } from '@/app/access'
import { Can } from '@/shared/auth'
import { formatDate, formatNumber } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { ChartContainer, ChartTooltipContent } from '@/shared/ui/chart'
import { axisProps, chartColor, cursorProps, gridProps } from '@/shared/ui/chart-theme'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { useEvaluations } from '../hooks/useFournisseursQueries'
import { CRITERES_EVALUATION } from '../schemas/evaluation.schema'
import { ScoreBar } from './ScoreBar'
import { EvaluerDialog } from './EvaluerDialog'

/** Onglet "Evaluations" : courbe de la note globale et detail des evaluations. */
export function EvaluationsTab({
  fournisseurId,
  statut,
}: {
  fournisseurId: string
  statut: StatutFournisseur
}) {
  const { t } = useTranslation('fournisseurs')
  const query = useEvaluations(fournisseurId)
  const evaluations = [...(query.data ?? [])].sort((a, b) => a.date.localeCompare(b.date))
  const serie = evaluations.map((e) => ({ date: formatDate(e.date, 'dd/MM/yy'), note: e.noteGlobale }))

  return (
    <div className="grid gap-4">
      <Card>
        <CardHeader
          title={t('evaluations.chart')}
          action={
            <Can roles={access.fournisseurEvaluation}>
              {statut === 'EN_ATTENTE_VALIDATION' ? (
                <span className="text-xs text-muted-foreground">
                  Évaluation possible après validation de l'inscription.
                </span>
              ) : (
                <EvaluerDialog fournisseurId={fournisseurId} />
              )}
            </Can>
          }
        />
        <CardContent>
          {query.isPending ? (
            <Skeleton className="h-56 w-full" />
          ) : query.isError ? (
            <ErrorState error={query.error} onRetry={() => void query.refetch()} size="compact" />
          ) : evaluations.length === 0 ? (
            <EmptyState
              size="compact"
              title={t('evaluations.empty')}
              description={t('evaluations.emptyDescription')}
            />
          ) : (
            <ChartContainer label={t('evaluations.chartLabel')} height={220}>
              <LineChart data={serie} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid {...gridProps} />
                <XAxis dataKey="date" {...axisProps} />
                <YAxis {...axisProps} domain={[0, 100]} width={36} />
                <Tooltip
                  cursor={cursorProps}
                  content={<ChartTooltipContent valueFormatter={(v) => `${formatNumber(v)} / 100`} />}
                />
                <Line
                  type="monotone"
                  dataKey="note"
                  name="Note globale"
                  stroke={chartColor(0)}
                  strokeWidth={2}
                  dot={{ r: 3 }}
                />
              </LineChart>
            </ChartContainer>
          )}
        </CardContent>
      </Card>
      {evaluations.length > 0 && (
        <Card>
          <CardHeader title={t('evaluations.title')} />
          <CardContent>
            <ul className="grid gap-3">
              {[...evaluations].reverse().map((e) => (
                <li key={e.id} className="grid gap-2 rounded-lg border border-border p-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="text-sm font-medium">
                      {e.evaluateurNom}
                      <span className="ml-2 text-xs font-normal text-muted-foreground">
                        {formatDate(e.date)}
                      </span>
                    </p>
                    <ScoreBar score={e.noteGlobale} className="w-40" />
                  </div>
                  <dl className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
                    {CRITERES_EVALUATION.map(({ name, label }) => (
                      <div key={name} className="flex gap-1">
                        <dt>{label}</dt>
                        <dd className="font-medium tabular-nums text-foreground">
                          {formatNumber(e[name], { decimals: 1 })}/10
                        </dd>
                      </div>
                    ))}
                  </dl>
                  {e.commentaire && <p className="text-sm text-muted-foreground">{e.commentaire}</p>}
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
