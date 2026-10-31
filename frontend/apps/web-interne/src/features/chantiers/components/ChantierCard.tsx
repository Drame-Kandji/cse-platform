import { useTranslation } from 'react-i18next'
import { Card } from '@/shared/ui/card'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Chantier } from '../api/types'

/** Carte resumee d'un chantier (grille et vue mobile), cliquable pour ouvrir le detail. */
export function ChantierCard({ chantier, onOpen }: { chantier: Chantier; onOpen: () => void }) {
  const { t } = useTranslation('chantiers')
  return (
    <Card interactive className="h-full">
      <button
        type="button"
        onClick={onOpen}
        className="grid h-full w-full content-start gap-3 rounded-xl p-4 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:p-5"
      >
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="font-mono text-xs text-muted-foreground">{chantier.code}</p>
            <p className="truncate font-semibold">{chantier.nom}</p>
          </div>
          <StatusBadge kind="statutChantier" value={chantier.statut} />
        </div>
        <p className="truncate text-sm text-muted-foreground">
          {chantier.localisation} · {t('card.materiels', { count: chantier.nombreMateriels })}
        </p>
      </button>
    </Card>
  )
}
