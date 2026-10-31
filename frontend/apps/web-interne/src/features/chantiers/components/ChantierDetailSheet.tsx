import { Ban, Pencil, Truck } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { access } from '@/app/access'
import { to } from '@/app/routes'
import { Can } from '@/shared/auth'
import { EMPTY_VALUE, formatDate, formatMoney, formatNumber } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { ConfirmDialog } from '@/shared/ui/alert-dialog'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Sheet, SheetContent } from '@/shared/ui/sheet'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import { useAffectationsChantier, useChantier, useTerminerChantier } from '../hooks/useChantiers'
import type { Chantier } from '../api/types'
import { BudgetBar } from './BudgetBar'

function Ligne({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex justify-between gap-4 py-2 text-sm">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="text-right font-medium tabular-nums">{children}</dd>
    </div>
  )
}

function MaterielsEnCours({ chantierId }: { chantierId: string }) {
  const { t } = useTranslation('chantiers')
  const query = useAffectationsChantier(chantierId)
  if (query.isPending) return <Skeleton className="h-20 w-full" />
  if (query.isError)
    return <ErrorState size="compact" error={query.error} onRetry={() => void query.refetch()} />
  if (query.data.content.length === 0) {
    return <EmptyState size="compact" icon={<Truck aria-hidden />} title={t('detail.materielsVide')} />
  }
  const reste = query.data.totalElements - query.data.content.length
  return (
    <ul className="grid gap-1.5">
      {query.data.content.map((affectation) => (
        <li key={affectation.id}>
          <Link
            to={to.materielDetail(affectation.materielId)}
            className="flex items-center justify-between gap-3 rounded-lg border border-border px-3 py-2 text-sm hover:bg-muted/40"
          >
            <span className="min-w-0">
              <span className="block truncate font-medium">{affectation.materielDesignation}</span>
              <span className="font-mono text-xs text-muted-foreground">{affectation.materielCode}</span>
            </span>
            <span className="shrink-0 text-xs tabular-nums text-muted-foreground">
              {formatDate(affectation.dateFinPrevue)}
            </span>
          </Link>
        </li>
      ))}
      {reste > 0 && <li className="text-xs text-muted-foreground">{t('detail.plusDe', { count: reste })}</li>}
    </ul>
  )
}

interface Props {
  chantierId: string | null
  onClose: () => void
  onEdit: (chantier: Chantier) => void
}

/** Panneau de detail d'un chantier : faits, budget, materiels en cours, actions RM/ADMIN. */
export function ChantierDetailSheet({ chantierId, onClose, onEdit }: Props) {
  const { t } = useTranslation('chantiers')
  const query = useChantier(chantierId)
  const terminer = useTerminerChantier()
  const chantier = query.data

  const footer = chantier && chantier.statut === 'EN_COURS' && (
    <>
      <Can roles={access.chantierTerminer}>
        <ConfirmDialog
          tone="danger"
          title={t('terminer.title', { code: chantier.code })}
          description={t('terminer.description')}
          confirmLabel={t('terminer.confirmer')}
          onConfirm={() =>
            new Promise<void>((resolve) => {
              terminer.mutate(chantier.id, {
                onSettled: () => {
                  resolve()
                },
              })
            })
          }
          trigger={
            <Button variant="secondary" leftIcon={<Ban />}>
              {t('detail.terminer')}
            </Button>
          }
        />
      </Can>
      <Can roles={access.chantierGestion}>
        <Button
          leftIcon={<Pencil />}
          onClick={() => {
            onEdit(chantier)
          }}
        >
          {t('detail.modifier')}
        </Button>
      </Can>
    </>
  )

  return (
    <Sheet
      open={chantierId !== null}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
    >
      <SheetContent
        size="lg"
        title={chantier ? chantier.nom : t('page.title')}
        description={chantier ? `${chantier.code} · ${chantier.localisation}` : undefined}
        footer={footer || undefined}
      >
        {query.isPending ? (
          <div className="grid gap-3">
            <Skeleton className="h-6 w-40" />
            <Skeleton className="h-40 w-full" />
          </div>
        ) : query.isError ? (
          <ErrorState error={query.error} onRetry={() => void query.refetch()} />
        ) : chantier ? (
          <div className="grid gap-6">
            <StatusBadge kind="statutChantier" value={chantier.statut} className="justify-self-start" />
            <BudgetBar budget={chantier.budget} montantMarche={chantier.montantMarche} />
            <dl className="divide-y divide-border">
              <Ligne label={t('detail.localisation')}>
                {chantier.localisation} · {chantier.region}
              </Ligne>
              <Ligne label={t('detail.coordonnees')}>
                {formatNumber(chantier.latitude, { decimals: 4 })},{' '}
                {formatNumber(chantier.longitude, { decimals: 4 })}
              </Ligne>
              <Ligne label={t('detail.chef')}>{chantier.chefChantierNom ?? EMPTY_VALUE}</Ligne>
              <Ligne label={t('detail.periode')}>
                {formatDate(chantier.dateDebut)} → {formatDate(chantier.dateFinPrevue)}
              </Ligne>
              <Ligne label={t('detail.budget')}>{formatMoney(chantier.budget)}</Ligne>
              <Ligne label={t('detail.montantMarche')}>{formatMoney(chantier.montantMarche)}</Ligne>
              <Ligne label={t('detail.ecart')}>{formatMoney(chantier.montantMarche - chantier.budget)}</Ligne>
            </dl>
            <section className="grid gap-2">
              <h3 className="text-sm font-semibold">
                {t('detail.materielsAffectes')} ({formatNumber(chantier.nombreMateriels)})
              </h3>
              <MaterielsEnCours chantierId={chantier.id} />
            </section>
          </div>
        ) : null}
      </SheetContent>
    </Sheet>
  )
}
