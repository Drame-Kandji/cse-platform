import { CheckCircle2, MessageSquarePlus, Archive } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { access } from '@/app/access'
import { Can } from '@/shared/auth'
import type { Tone } from '@/shared/domain/labels'
import { formatDate } from '@/shared/lib/format'
import { Button } from '@/shared/ui/button'
import { Card, CardContent, CardHeader } from '@/shared/ui/card'
import { EmptyState } from '@/shared/ui/empty-state'
import { ErrorState } from '@/shared/ui/error-state'
import { Skeleton } from '@/shared/ui/skeleton'
import { StatusBadge } from '@/shared/ui/status-badge'
import { Timeline } from '@/shared/ui/timeline'
import type { Litige, TypeActionLitige } from '../api/types'
import { useActionLitige } from '../hooks/useFournisseurMutations'
import { useLitigesFournisseur } from '../hooks/useFournisseursQueries'
import { MotifDialog } from './MotifDialog'
import { OuvrirLitigeDialog } from './OuvrirLitigeDialog'

const ACTION_LABELS: Record<TypeActionLitige, { label: string; tone: Tone }> = {
  OUVERTURE: { label: 'Ouverture', tone: 'danger' },
  ACTION: { label: 'Action', tone: 'info' },
  RESOLUTION: { label: 'Résolution', tone: 'success' },
  CLASSEMENT: { label: 'Classement', tone: 'neutral' },
}

const ACTIONS = [
  {
    type: 'ACTION',
    label: 'Ajouter une action',
    icon: <MessageSquarePlus />,
    title: 'Ajouter une action de suivi',
  },
  { type: 'RESOLUTION', label: 'Résoudre', icon: <CheckCircle2 />, title: 'Résoudre le litige ?' },
  { type: 'CLASSEMENT', label: 'Classer', icon: <Archive />, title: 'Classer le litige sans suite ?' },
] as const

function LitigeCard({ litige, fournisseurId }: { litige: Litige; fournisseurId: string }) {
  const agir = useActionLitige(fournisseurId)
  const ouvert = litige.statut === 'OUVERT' || litige.statut === 'EN_COURS'
  const items = [...litige.actions].reverse().map((action, index) => ({
    id: `${litige.id}-${String(index)}`,
    title: ACTION_LABELS[action.type].label,
    tone: ACTION_LABELS[action.type].tone,
    date: action.date,
    actor: action.auteurNom,
    description: action.commentaire,
  }))
  return (
    <Card>
      <CardHeader
        title={`${litige.reference} · ${litige.objet}`}
        description={`Ouvert le ${formatDate(litige.dateOuverture)}${
          litige.dateResolution ? ` · clos le ${formatDate(litige.dateResolution)}` : ''
        }`}
        action={
          <div className="flex flex-wrap gap-1">
            <StatusBadge kind="graviteLitige" value={litige.gravite} />
            <StatusBadge kind="statutLitige" value={litige.statut} />
          </div>
        }
      />
      <CardContent className="grid gap-4">
        <p className="text-sm text-muted-foreground">{litige.description}</p>
        {items.length > 0 && <Timeline items={items} />}
        {ouvert && (
          <Can roles={access.achatsGestion}>
            <div className="flex flex-wrap gap-2 border-t border-border pt-3">
              {ACTIONS.map((action) => (
                <MotifDialog
                  key={action.type}
                  title={action.title}
                  description={`${litige.reference} · ${litige.objet}`}
                  label="Commentaire"
                  confirmLabel={action.label}
                  onConfirm={(commentaire) =>
                    agir.mutateAsync({ litigeId: litige.id, body: { type: action.type, commentaire } })
                  }
                  trigger={
                    <Button variant="secondary" size="sm" leftIcon={action.icon}>
                      {action.label}
                    </Button>
                  }
                />
              ))}
            </div>
          </Can>
        )}
      </CardContent>
    </Card>
  )
}

/** Onglet "Litiges" : litiges du fournisseur, leur historique et les actions du RA. */
export function LitigesTab({ fournisseurId }: { fournisseurId: string }) {
  const { t } = useTranslation('fournisseurs')
  const query = useLitigesFournisseur(fournisseurId)
  return (
    <div className="grid gap-4">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-base font-semibold">{t('litiges.title')}</h2>
        <Can roles={access.litigeOuverture}>
          <OuvrirLitigeDialog fournisseurId={fournisseurId} />
        </Can>
      </div>
      {query.isPending ? (
        <Skeleton className="h-40 w-full rounded-xl" />
      ) : query.isError ? (
        <ErrorState error={query.error} onRetry={() => void query.refetch()} />
      ) : query.data.content.length === 0 ? (
        <Card>
          <EmptyState title={t('litiges.empty')} description={t('litiges.emptyDescription')} />
        </Card>
      ) : (
        query.data.content.map((litige) => (
          <LitigeCard key={litige.id} litige={litige} fournisseurId={fournisseurId} />
        ))
      )}
    </div>
  )
}
