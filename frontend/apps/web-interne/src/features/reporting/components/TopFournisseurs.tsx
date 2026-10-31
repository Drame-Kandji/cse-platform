import { Trophy } from 'lucide-react'
import { Link } from 'react-router-dom'
import { to } from '@/app/routes'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { formatTaux } from '../lib/format'
import type { SectionFournisseurs } from '../types'

function scoreTone(score: number) {
  if (score >= 75) return 'bg-success'
  if (score >= 50) return 'bg-warning'
  return 'bg-danger'
}

/** Classement des meilleurs fournisseurs : rang, barre de score, litiges, ponctualite. */
export function TopFournisseurs({ fournisseurs }: { fournisseurs: SectionFournisseurs }) {
  const top = fournisseurs.top.slice(0, 6)
  return (
    <Card className="flex h-full flex-col">
      <CardHeader
        title="Meilleurs fournisseurs"
        description={`Score moyen ${formatNumber(fournisseurs.scoreMoyen, { decimals: 1 })}/100 · ${formatNumber(fournisseurs.actifs)} actifs`}
        icon={<Trophy />}
      />
      <CardContent className="flex-1">
        {top.length === 0 ? (
          <EmptyState
            size="compact"
            title="Aucun fournisseur évalué"
            description="Le classement apparaîtra après les premières évaluations."
          />
        ) : (
          <ol className="grid gap-3.5">
            {top.map((fournisseur, index) => (
              <li
                key={fournisseur.fournisseurId}
                className="grid grid-cols-[1.5rem_minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1"
              >
                <span
                  className={cn(
                    'grid size-6 place-items-center rounded-md text-xs font-semibold tabular-nums',
                    index === 0 ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground',
                  )}
                >
                  {index + 1}
                </span>
                <Link
                  to={to.fournisseurDetail(fournisseur.fournisseurId)}
                  className="truncate text-sm font-medium hover:text-primary-emphasis hover:underline"
                >
                  {fournisseur.raisonSociale}
                </Link>
                <span className="text-sm font-semibold tabular-nums">
                  {formatNumber(fournisseur.scorePerformance, { decimals: 1 })}
                  <span className="text-xs font-normal text-muted-foreground">/100</span>
                </span>
                <span aria-hidden />
                <div className="h-1.5 overflow-hidden rounded-full bg-muted" aria-hidden>
                  <div
                    className={cn('h-full rounded-full', scoreTone(fournisseur.scorePerformance))}
                    style={{ width: `${Math.max(0, Math.min(100, fournisseur.scorePerformance))}%` }}
                  />
                </div>
                <span className="text-right text-2xs text-muted-foreground">
                  {fournisseur.nombreLitiges > 0 ? `${fournisseur.nombreLitiges} litige(s)` : 'Sans litige'}
                  {fournisseur.tauxLivraisonALHeure != null &&
                    ` · ${formatTaux(fournisseur.tauxLivraisonALHeure, 0)} à l'heure`}
                </span>
              </li>
            ))}
          </ol>
        )}
      </CardContent>
    </Card>
  )
}
