import { CalendarDays, HardHat, MapPin, Truck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { formatDate } from '@/shared/lib/format'
import { Card } from '@/shared/ui/card'
import { StatusBadge } from '@/shared/ui/status-badge'
import type { Chantier } from '../api/types'
import { BudgetBar } from './BudgetBar'

/** Carte resumee d'un chantier (grille et vue mobile), cliquable pour ouvrir le detail. */
export function ChantierCard({ chantier, onOpen }: { chantier: Chantier; onOpen: () => void }) {
  const { t } = useTranslation('chantiers')
  return (
    <Card interactive className="h-full">
      <button
        type="button"
        onClick={onOpen}
        className="grid h-full w-full gap-4 rounded-xl p-4 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:p-5"
      >
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="font-mono text-xs text-muted-foreground">{chantier.code}</p>
            <p className="truncate font-semibold">{chantier.nom}</p>
          </div>
          <StatusBadge kind="statutChantier" value={chantier.statut} />
        </div>
        <ul className="grid gap-1.5 text-sm text-muted-foreground">
          <li className="flex items-center gap-2">
            <MapPin className="size-4 shrink-0" aria-hidden />
            <span className="truncate">
              {chantier.localisation} · {chantier.region}
            </span>
          </li>
          <li className="flex items-center gap-2">
            <HardHat className="size-4 shrink-0" aria-hidden />
            <span className="truncate">{chantier.chefChantierNom ?? t('card.sansChef')}</span>
          </li>
          <li className="flex items-center gap-2">
            <CalendarDays className="size-4 shrink-0" aria-hidden />
            <span className="tabular-nums">
              {t('card.periode', {
                debut: formatDate(chantier.dateDebut),
                fin: chantier.dateFinPrevue ? formatDate(chantier.dateFinPrevue) : t('card.finIndeterminee'),
              })}
            </span>
          </li>
          <li className="flex items-center gap-2">
            <Truck className="size-4 shrink-0" aria-hidden />
            <span>{t('card.materiels', { count: chantier.nombreMateriels })}</span>
          </li>
        </ul>
        <BudgetBar budget={chantier.budget} montantMarche={chantier.montantMarche} />
      </button>
    </Card>
  )
}
