import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { useHasRole } from '@/shared/auth'
import { describeStatus, labelOf } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatDateTime, formatMoney, formatNumber } from '@/shared/lib/format'
import { Badge } from '@/shared/ui/badge'
import { Card, CardContent } from '@/shared/ui/card'
import type { AppelOffre } from '../api/types'

function Info({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="min-w-0">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="truncate text-sm text-foreground">{children}</dd>
    </div>
  )
}

/** Synthese d'un appel d'offres : besoin, quantites, budget, criteres et invites. */
export function AppelOffreSummary({ ao }: { ao: AppelOffre }) {
  const voitBesoins = useHasRole(access.besoins)
  return (
    <Card>
      <CardContent className="grid gap-5">
        <dl className="grid grid-cols-2 gap-4 md:grid-cols-4">
          <Info label="Catégorie">{labelOf('categorieMateriel', ao.categorie)}</Info>
          <Info label="Quantité">{formatNumber(ao.quantite)}</Info>
          <Info label="Budget estimé">{formatMoney(ao.budgetEstime)}</Info>
          <Info label="Chantier">{ao.chantierNom ?? EMPTY_VALUE}</Info>
          <Info label="Besoin d'origine">
            {ao.besoinId && ao.besoinReference && voitBesoins ? (
              <Link
                className="font-mono text-primary-emphasis hover:underline"
                to={to.besoinDetail(ao.besoinId)}
              >
                {ao.besoinReference}
              </Link>
            ) : (
              (ao.besoinReference ?? EMPTY_VALUE)
            )}
          </Info>
          <Info label="Publié le">{formatDateTime(ao.datePublication)}</Info>
          <Info label="Date limite">{formatDateTime(ao.dateLimite)}</Info>
          <Info label="Créé par">{ao.creeParNom}</Info>
        </dl>
        {ao.description && (
          <p className="whitespace-pre-line text-sm text-muted-foreground">{ao.description}</p>
        )}
        {ao.specifications && (
          <div>
            <p className="text-xs text-muted-foreground">Spécifications techniques</p>
            <p className="whitespace-pre-line text-sm">{ao.specifications}</p>
          </div>
        )}
        <div className="grid gap-4 md:grid-cols-2">
          <div className="grid gap-2">
            <p className="text-xs text-muted-foreground">Critères pondérés</p>
            <ul className="flex flex-wrap gap-1.5">
              {ao.criteres.map((c) => (
                <li key={c.code}>
                  <Badge tone="neutral">
                    {describeStatus('critereEvaluation', c.code).label} · {c.poids} %
                  </Badge>
                </li>
              ))}
            </ul>
          </div>
          <div className="grid gap-2">
            <p className="text-xs text-muted-foreground">
              Fournisseurs invités ({ao.fournisseursInvites.length}) · {formatNumber(ao.nombreOffres)}{' '}
              offre(s) reçue(s)
            </p>
            <ul className="flex flex-wrap gap-1.5">
              {ao.fournisseursInvites.map((f) => (
                <li key={f.id}>
                  <Badge variant="outline">{f.raisonSociale}</Badge>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
