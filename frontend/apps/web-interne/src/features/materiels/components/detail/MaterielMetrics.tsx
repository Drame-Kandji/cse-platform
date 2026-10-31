import type { ReactNode } from 'react'
import { labelOf } from '@/shared/domain/labels'
import type { MaterielDetail } from '@/shared/domain/types'
import { cn } from '@/shared/lib/cn'
import { formatDate, formatMoney, formatNumber } from '@/shared/lib/format'
import { Card } from '@/shared/ui/card'
import { maintenanceRatio } from '../../lib/compteurs'

function Fact({ label, children, alert }: { label: string; children: ReactNode; alert?: boolean }) {
  return (
    <div className="grid content-start gap-0.5">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className={cn('text-sm font-medium tabular-nums', alert && 'text-danger-emphasis')}>{children}</dd>
    </div>
  )
}

/** Faits cles de la fiche engin en une grille compacte (identite, compteurs, maintenance, valeur). */
export function MaterielMetrics({ materiel }: { materiel: MaterielDetail }) {
  const ratio = maintenanceRatio(materiel)
  const modele = [materiel.marque, materiel.modele].filter(Boolean).join(' ')
  const depuis =
    materiel.seuilMaintenanceHeures && materiel.seuilMaintenanceHeures > 0
      ? `${formatNumber(materiel.heuresDepuisMaintenance)} / ${formatNumber(materiel.seuilMaintenanceHeures)} h`
      : `${formatNumber(materiel.heuresDepuisMaintenance)} h`
  return (
    <Card className="p-5">
      <dl className="grid grid-cols-2 gap-x-6 gap-y-4 sm:grid-cols-3 lg:grid-cols-4">
        <Fact label="Type">
          {materiel.typeEnginLibelle ?? labelOf('categorieMateriel', materiel.categorie)}
        </Fact>
        <Fact label="Marque / modèle">{modele || '-'}</Fact>
        <Fact label="Immatriculation">
          <span className="font-mono">{materiel.immatriculation ?? '-'}</span>
        </Fact>
        <Fact label="Heures de fonctionnement">{`${formatNumber(materiel.heuresFonctionnement)} h`}</Fact>
        <Fact label="Depuis la maintenance" alert={ratio !== null && ratio >= 1}>
          {depuis}
        </Fact>
        <Fact label="Prochaine maintenance" alert={materiel.maintenanceDue}>
          {materiel.maintenanceDue
            ? 'Maintenance due'
            : materiel.prochaineMaintenance
              ? formatDate(materiel.prochaineMaintenance)
              : 'Non planifiée'}
        </Fact>
        <Fact label="Tarif journalier">{formatMoney(materiel.tarifJournalier)}</Fact>
        <Fact label="Valeur d'acquisition">
          <span
            title={materiel.dateAcquisition ? `Acquis le ${formatDate(materiel.dateAcquisition)}` : undefined}
          >
            {formatMoney(materiel.valeurAcquisition)}
          </span>
        </Fact>
      </dl>
    </Card>
  )
}
