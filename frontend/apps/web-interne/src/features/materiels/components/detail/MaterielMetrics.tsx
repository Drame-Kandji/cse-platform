import { differenceInCalendarDays } from 'date-fns'
import { CalendarClock, Coins, Gauge, Timer, Wallet } from 'lucide-react'
import type { MaterielDetail } from '@/shared/domain/types'
import { formatDate, formatMoney, formatNumber, toDate } from '@/shared/lib/format'
import { Card } from '@/shared/ui/card'
import { StatCard } from '@/shared/ui/stat-card'
import { maintenanceRatio } from '../../lib/compteurs'
import { MaintenanceGauge } from '../MaintenanceGauge'

function prochaineHint(date: string | null | undefined, due: boolean): string | undefined {
  if (due) return 'Maintenance due : à planifier'
  const target = toDate(date)
  if (!target) return 'Aucune échéance planifiée'
  const days = differenceInCalendarDays(target, new Date())
  if (days < 0) return `En retard de ${String(-days)} j`
  if (days === 0) return "Aujourd'hui"
  return `Dans ${String(days)} j`
}

/** Indicateurs cles de la fiche engin (compteurs, maintenance, valeur). */
export function MaterielMetrics({ materiel }: { materiel: MaterielDetail }) {
  const ratio = maintenanceRatio(materiel)
  return (
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
      <StatCard
        label="Heures de fonctionnement"
        value={`${formatNumber(materiel.heuresFonctionnement)} h`}
        icon={<Timer />}
        hint="Compteur total"
      />
      <Card className="flex flex-col gap-3 p-5 sm:col-span-2 xl:col-span-1">
        <div className="flex items-start justify-between gap-3">
          <p className="text-sm font-medium text-muted-foreground">Depuis la maintenance</p>
          <span className="grid size-8 place-items-center rounded-lg bg-warning/10 text-warning-emphasis [&_svg]:size-4">
            <Gauge aria-hidden />
          </span>
        </div>
        <MaintenanceGauge materiel={materiel} size="lg" />
        <p className="text-xs text-muted-foreground">
          {ratio === null
            ? 'Aucun seuil défini pour ce type d’engin'
            : ratio >= 1
              ? 'Seuil de maintenance préventive dépassé'
              : `Reste ${formatNumber(Math.max(0, (materiel.seuilMaintenanceHeures ?? 0) - materiel.heuresDepuisMaintenance))} h avant le seuil`}
        </p>
      </Card>
      <StatCard
        label="Prochaine maintenance"
        value={materiel.prochaineMaintenance ? formatDate(materiel.prochaineMaintenance) : 'Non planifiée'}
        icon={<CalendarClock />}
        iconTone={materiel.maintenanceDue ? 'danger' : 'info'}
        hint={prochaineHint(materiel.prochaineMaintenance, materiel.maintenanceDue)}
      />
      <StatCard
        label="Tarif journalier"
        value={formatMoney(materiel.tarifJournalier)}
        icon={<Coins />}
        iconTone="accent"
        hint="Base de refacturation chantier"
      />
      <StatCard
        label="Valeur d'acquisition"
        value={formatMoney(materiel.valeurAcquisition)}
        icon={<Wallet />}
        iconTone="success"
        hint={materiel.dateAcquisition ? `Acquis le ${formatDate(materiel.dateAcquisition)}` : undefined}
      />
    </div>
  )
}
