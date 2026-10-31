import { AlertTriangle } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { to } from '@/app/routes'
import { labelOf } from '@/shared/domain/labels'
import { formatDate, formatMoney, formatNumber } from '@/shared/lib/format'
import { Card, CardContent, CardHeader } from '@/shared/ui'
import type { Contrat } from '../api/types'
import { livraisonEnRetard } from './contrat-domain'

function Item({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="grid gap-0.5">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-sm font-medium text-foreground">{children}</dd>
    </div>
  )
}

const linkClass = 'text-primary-emphasis underline-offset-4 hover:underline'

/** Synthese du contrat : parties, objet, montants, dates et penalites de retard. */
export function ContratSummary({ contrat }: { contrat: Contrat }) {
  const { t } = useTranslation('contrats')
  const late = livraisonEnRetard(contrat)
  const taux = contrat.clauses.penaliteRetardPctParJour
  return (
    <Card>
      <CardHeader title={t('detail.summary')} />
      <CardContent className="grid gap-5">
        {(late || contrat.penalitesRetard > 0) && (
          <div
            role="status"
            className="flex gap-3 rounded-lg border border-danger/30 bg-danger/10 p-3 text-sm text-danger-emphasis"
          >
            <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
            <div className="grid gap-0.5">
              <p className="font-semibold">{late ? t('detail.late') : t('detail.penalties')}</p>
              {contrat.penalitesRetard > 0 && (
                <p>
                  {t('detail.penalties')} :{' '}
                  <span className="font-semibold tabular-nums">{formatMoney(contrat.penalitesRetard)}</span>
                </p>
              )}
              {typeof taux === 'number' && (
                <p className="text-xs">
                  {t('detail.penaltiesHint', { taux: formatNumber(taux, { decimals: 2 }) })}
                </p>
              )}
            </div>
          </div>
        )}
        <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Item label={t('fields.fournisseur')}>
            <Link className={linkClass} to={to.fournisseurDetail(contrat.fournisseurId)}>
              {contrat.fournisseurNom}
            </Link>
          </Item>
          <Item label={t('fields.appelOffre')}>
            <Link className={linkClass} to={to.appelOffreDetail(contrat.appelOffreId)}>
              {contrat.appelOffreReference}
            </Link>
          </Item>
          <Item label={t('fields.chantier')}>{contrat.chantierNom ?? '—'}</Item>
          <Item label={t('fields.designation')}>
            {contrat.designation}
            <span className="block text-xs font-normal text-muted-foreground">
              {labelOf('categorieMateriel', contrat.categorie)}
            </span>
          </Item>
          <Item label={t('fields.quantite')}>
            <span className="tabular-nums">{formatNumber(contrat.quantite)}</span>
          </Item>
          <Item label={t('fields.montant')}>
            <span className="tabular-nums">{formatMoney(contrat.montant)}</span>
          </Item>
          <Item label={t('fields.dateSignature')}>{formatDate(contrat.dateSignature)}</Item>
          <Item label={t('fields.delai')}>
            <span className="tabular-nums">{formatNumber(contrat.delaiLivraisonJours)} jours</span>
          </Item>
          <Item label={t('fields.datePrevue')}>
            <span className={late ? 'text-danger-emphasis' : undefined}>
              {formatDate(contrat.dateLivraisonPrevue)}
            </span>
          </Item>
          <Item label={t('fields.dateReelle')}>{formatDate(contrat.dateLivraisonReelle)}</Item>
          <Item label={t('fields.bonLivraison')}>
            <span className="font-mono text-xs">{contrat.numeroBonLivraison ?? '—'}</span>
          </Item>
        </dl>
      </CardContent>
    </Card>
  )
}
