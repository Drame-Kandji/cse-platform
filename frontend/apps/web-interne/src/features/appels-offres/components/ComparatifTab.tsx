import { BarChart3, Lock, Star } from 'lucide-react'
import { describeStatus } from '@/shared/domain/labels'
import { cn } from '@/shared/lib/cn'
import { EMPTY_VALUE, formatMoney, formatNumber } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import type { AppelOffre, Comparatif } from '../api/types'
import { useComparatif } from '../hooks/useAppelsOffres'
import { ComparatifChart } from './ComparatifChart'
import { contribution, nomOffre } from './offre-display'

function TableauClassement({ comparatif, ao }: { comparatif: Comparatif; ao: AppelOffre }) {
  const criteres = comparatif.criteres.filter((c) => c.poids > 0)
  return (
    <div className="scrollbar-thin overflow-x-auto">
      <table className="w-full border-collapse text-sm" aria-label="Classement multicritère des offres">
        <thead className="bg-surface-sunken/70 text-xs text-muted-foreground">
          <tr className="border-b border-border">
            <th scope="col" className="h-10 px-3 text-left font-medium">
              Rang
            </th>
            <th scope="col" className="px-3 text-left font-medium">
              Offre
            </th>
            <th scope="col" className="px-3 text-right font-medium">
              Montant
            </th>
            {criteres.map((c) => (
              <th key={c.code} scope="col" className="whitespace-nowrap px-3 text-right font-medium">
                {describeStatus('critereEvaluation', c.code).label}
                <span className="block font-normal">{c.poids} %</span>
              </th>
            ))}
            <th scope="col" className="px-3 text-right font-medium">
              Score pondéré
            </th>
          </tr>
        </thead>
        <tbody>
          {comparatif.offres.map((offre, index) => {
            const recommandee = offre.id === comparatif.recommandationOffreId
            return (
              <tr
                key={offre.id}
                className={cn('border-b border-border last:border-0', recommandee && 'bg-success/10')}
                aria-label={recommandee ? `${nomOffre(offre, ao.statut)}, offre recommandée` : undefined}
              >
                <td className="h-12 px-3 font-semibold tabular-nums">{offre.rang ?? index + 1}</td>
                <td className="px-3">
                  <span className="flex flex-wrap items-center gap-2 font-medium">
                    {nomOffre(offre, ao.statut)}
                    {recommandee && (
                      <Badge tone="success">
                        <Star aria-hidden />
                        Recommandée
                      </Badge>
                    )}
                    {offre.retenue && <Badge tone="primary">Retenue</Badge>}
                  </span>
                </td>
                <td className="px-3 text-right tabular-nums">{formatMoney(offre.montantPropose)}</td>
                {criteres.map((c) => {
                  const note = offre.notes[c.code]
                  const part = contribution(note, c.poids)
                  return (
                    <td key={c.code} className="px-3 text-right tabular-nums">
                      {note == null ? EMPTY_VALUE : formatNumber(note, { decimals: 1 })}
                      {part !== undefined && (
                        <span className="block text-xs text-muted-foreground">
                          +{formatNumber(part, { decimals: 1 })} pt
                        </span>
                      )}
                    </td>
                  )
                })}
                <td className="px-3 text-right text-base font-semibold tabular-nums">
                  {offre.scoreGlobal == null ? EMPTY_VALUE : formatNumber(offre.scoreGlobal, { decimals: 1 })}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

/** Onglet Comparatif : classement pondere, graphique par critere, offre recommandee. */
export function ComparatifTab({ ao }: { ao: AppelOffre }) {
  const query = useComparatif(ao.id, ao.statut !== 'PUBLIE')
  if (ao.statut === 'PUBLIE') {
    return (
      <EmptyState
        icon={<Lock aria-hidden />}
        title="Comparatif disponible après la clôture"
        description="Le comparatif nominatif des offres est établi une fois l'appel d'offres clôturé."
      />
    )
  }
  if (query.isPending) return <Skeleton className="h-80 w-full" />
  if (query.isError) return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  const comparatif = query.data
  if (comparatif.offres.length === 0) {
    return <EmptyState icon={<BarChart3 aria-hidden />} title="Aucune offre à comparer" />
  }
  const recommandee = comparatif.offres.find((o) => o.id === comparatif.recommandationOffreId)
  return (
    <div className="grid gap-4">
      {recommandee && (
        <div className="flex items-start gap-3 rounded-xl border border-success/30 bg-success/10 px-4 py-3 text-sm text-success-emphasis">
          <Star className="mt-0.5 size-4 shrink-0" aria-hidden />
          <p>
            Offre recommandée : <strong>{nomOffre(recommandee, ao.statut)}</strong> — score pondéré{' '}
            {formatNumber(recommandee.scoreGlobal, { decimals: 1 })}/100 pour{' '}
            {formatMoney(recommandee.montantPropose)}.
          </p>
        </div>
      )}
      <Card>
        <CardHeader
          title="Classement pondéré"
          description="Note par critère sur 100 et points apportés au score."
        />
        <CardContent className="px-0 pb-0">
          <TableauClassement comparatif={comparatif} ao={ao} />
        </CardContent>
      </Card>
      <Card>
        <CardHeader
          title="Notes par critère"
          description="Comparaison visuelle des offres, critère par critère (sur 100)."
        />
        <CardContent>
          <ComparatifChart comparatif={comparatif} statut={ao.statut} />
        </CardContent>
      </Card>
    </div>
  )
}
