import { Link } from 'react-router-dom'
import { to } from '@/app/routes'
import { cn } from '@/shared/lib/cn'
import { formatNumber } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import type { SectionFournisseurs } from '../types'

/** Sous ce score, le fournisseur est signale en rouge (alerte). */
const SEUIL_ALERTE = 50

/** Classement des meilleurs fournisseurs : rang, nom, score (rouge seulement sous le seuil). */
export function TopFournisseurs({ fournisseurs }: { fournisseurs: SectionFournisseurs }) {
  const top = fournisseurs.top.slice(0, 6)
  return (
    <Card className="flex h-full flex-col">
      <CardHeader title="Meilleurs fournisseurs" />
      <CardContent className="flex-1">
        {top.length === 0 ? (
          <EmptyState size="compact" title="Aucun fournisseur évalué" />
        ) : (
          <ol className="grid gap-3">
            {top.map((fournisseur, index) => (
              <li
                key={fournisseur.fournisseurId}
                className="grid grid-cols-[1.25rem_minmax(0,1fr)_auto] items-baseline gap-3 text-sm"
              >
                <span className="tabular-nums text-muted-foreground">{index + 1}</span>
                <span className="min-w-0">
                  <Link
                    to={to.fournisseurDetail(fournisseur.fournisseurId)}
                    className="block truncate font-medium hover:text-primary-emphasis hover:underline"
                  >
                    {fournisseur.raisonSociale}
                  </Link>
                  {fournisseur.nombreLitiges > 0 && (
                    <span className="text-xs text-muted-foreground">
                      {fournisseur.nombreLitiges} litige(s)
                    </span>
                  )}
                </span>
                <span
                  className={cn(
                    'font-semibold tabular-nums',
                    fournisseur.scorePerformance < SEUIL_ALERTE && 'text-danger-emphasis',
                  )}
                >
                  {formatNumber(fournisseur.scorePerformance, { decimals: 1 })}
                </span>
              </li>
            ))}
          </ol>
        )}
      </CardContent>
    </Card>
  )
}
