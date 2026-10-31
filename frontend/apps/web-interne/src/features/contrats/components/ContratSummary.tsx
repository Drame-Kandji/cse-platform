import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { to } from '@/app/routes'
import { labelOf } from '@/shared/domain/labels'
import { EMPTY_VALUE, formatDate, formatMoney, formatNumber } from '@/shared/lib/format'
import { Card, CardContent, StatusBadge } from '@/shared/ui'
import type { Contrat } from '../api/types'
import { livraisonEnRetard } from './contrat-domain'

function Item({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="grid min-w-0 content-start gap-0.5">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-sm text-foreground">{children}</dd>
    </div>
  )
}

const linkClass = 'text-primary-emphasis underline-offset-4 hover:underline'

/** Faits essentiels du contrat (grille compacte) + alertes reelles (retard, refus DAF). */
export function ContratSummary({ contrat }: { contrat: Contrat }) {
  const { t } = useTranslation('contrats')
  const late = livraisonEnRetard(contrat)
  const taux = contrat.clauses.penaliteRetardPctParJour
  const engagement = contrat.engagement
  const refuse = contrat.statut === 'BROUILLON' && engagement?.statut === 'REFUSEE'
  return (
    <Card>
      <CardContent className="grid gap-4">
        {(late || contrat.penalitesRetard > 0) && (
          <div
            role="status"
            className="flex flex-wrap items-baseline gap-x-3 text-sm text-danger-emphasis"
            title={
              typeof taux === 'number'
                ? t('detail.penaltiesHint', { taux: formatNumber(taux, { decimals: 2 }) })
                : undefined
            }
          >
            <p className="font-medium">{late ? t('detail.late') : t('detail.penalties')}</p>
            {contrat.penalitesRetard > 0 && (
              <p>
                {late && `${t('detail.penalties')} : `}
                <span className="font-medium tabular-nums">{formatMoney(contrat.penalitesRetard)}</span>
              </p>
            )}
          </div>
        )}
        {refuse && (
          <div role="status" className="grid gap-0.5 text-sm">
            <p className="font-medium text-danger-emphasis">{t('engagement.refuse')}</p>
            {engagement.commentaire && <p className="text-muted-foreground">{engagement.commentaire}</p>}
          </div>
        )}
        <dl className="grid grid-cols-2 gap-4 md:grid-cols-4">
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
          <Item label={t('fields.designation')}>
            {formatNumber(contrat.quantite)} × {contrat.designation}
            <span className="block text-xs text-muted-foreground">
              {labelOf('categorieMateriel', contrat.categorie)}
            </span>
          </Item>
          <Item label={t('engagement.title')}>
            {engagement ? (
              <StatusBadge kind="statutApprobation" value={engagement.statut} size="sm" />
            ) : (
              EMPTY_VALUE
            )}
          </Item>
          <Item label={t('fields.dateSignature')}>{formatDate(contrat.dateSignature)}</Item>
          <Item label={t('fields.datePrevue')}>
            <span className={late ? 'text-danger-emphasis' : undefined}>
              {formatDate(contrat.dateLivraisonPrevue)}
            </span>
            <span className="block text-xs text-muted-foreground tabular-nums">
              {formatNumber(contrat.delaiLivraisonJours)} jours
            </span>
          </Item>
          <Item label={t('fields.dateReelle')}>{formatDate(contrat.dateLivraisonReelle)}</Item>
          <Item label={t('fields.bonLivraison')}>{contrat.numeroBonLivraison ?? EMPTY_VALUE}</Item>
        </dl>
      </CardContent>
    </Card>
  )
}
