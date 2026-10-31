import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { useHasRole } from '@/shared/auth'
import { describeStatus, labelOf } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatDateTime, formatMoney, formatNumber } from '@/shared/lib/format'
import { Card, CardContent } from '@/shared/ui/card'
import type { AppelOffre } from '../api/types'

function Info({ label, children, wrap }: { label: string; children: ReactNode; wrap?: boolean }) {
  return (
    <div className="min-w-0">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className={wrap ? 'text-sm text-foreground' : 'truncate text-sm text-foreground'}>{children}</dd>
    </div>
  )
}

/** Synthese d'un appel d'offres : faits essentiels, le reste replie dans "Détails". */
export function AppelOffreSummary({ ao }: { ao: AppelOffre }) {
  const voitBesoins = useHasRole(access.besoins)
  const criteres = ao.criteres
    .filter((c) => c.poids > 0)
    .map((c) => `${describeStatus('critereEvaluation', c.code).label} ${c.poids} %`)
    .join(' · ')
  return (
    <Card>
      <CardContent className="grid gap-4">
        <dl className="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-6">
          <Info label="Budget estimé">{formatMoney(ao.budgetEstime)}</Info>
          <Info label="Quantité">
            {formatNumber(ao.quantite)} · {labelOf('categorieMateriel', ao.categorie)}
          </Info>
          <Info label="Chantier">{ao.chantierNom ?? EMPTY_VALUE}</Info>
          <Info label="Date limite">{formatDateTime(ao.dateLimite)}</Info>
          <Info label="Offres">
            {formatNumber(ao.nombreOffres)} / {formatNumber(ao.fournisseursInvites.length)} invité(s)
          </Info>
          <Info label="Besoin d'origine">
            {ao.besoinId && ao.besoinReference && voitBesoins ? (
              <Link className="text-primary-emphasis hover:underline" to={to.besoinDetail(ao.besoinId)}>
                {ao.besoinReference}
              </Link>
            ) : (
              (ao.besoinReference ?? EMPTY_VALUE)
            )}
          </Info>
        </dl>
        <details className="group text-sm">
          <summary className="w-fit cursor-pointer text-muted-foreground hover:text-foreground">
            Détails
          </summary>
          <dl className="mt-3 grid gap-3 md:grid-cols-2">
            <Info label="Critères" wrap>{criteres || EMPTY_VALUE}</Info>
            <Info label="Fournisseurs invités" wrap>
              {ao.fournisseursInvites.map((f) => f.raisonSociale).join(', ') || EMPTY_VALUE}
            </Info>
            <Info label="Publié le">
              {formatDateTime(ao.datePublication)} · {ao.creeParNom}
            </Info>
            {ao.description && (
              <div className="md:col-span-2">
                <dt className="text-xs text-muted-foreground">Description</dt>
                <dd className="whitespace-pre-line">{ao.description}</dd>
              </div>
            )}
            {ao.specifications && (
              <div className="md:col-span-2">
                <dt className="text-xs text-muted-foreground">Spécifications techniques</dt>
                <dd className="whitespace-pre-line">{ao.specifications}</dd>
              </div>
            )}
          </dl>
        </details>
      </CardContent>
    </Card>
  )
}
