import {
  Activity,
  ClipboardList,
  Gauge,
  Handshake,
  PackageCheck,
  Timer,
  Truck,
  Wallet,
  Wrench,
} from 'lucide-react'
import type { ReactNode } from 'react'
import type { Tone } from '@/shared/domain/labels'
import { formatMoney, formatNumber } from '@/shared/lib/format'
import type { Indicateur, Tableau } from '../types'
import { formatTaux } from './format'

/** Indicateur cle pret a afficher. */
export interface Kpi {
  readonly id: string
  readonly label: string
  readonly value: string
  readonly hint?: string
  readonly icon: ReactNode
  readonly tone: Tone
  readonly serie?: Indicateur
  /** Classe de couleur de la sparkline (jeton chart-n). */
  readonly sparkClass?: string
}

/** Indicateurs cles du tableau, dans l'ordre de lecture, selon les sections presentes. */
export function buildKpis(tableau: Tableau): Kpi[] {
  const kpis: Kpi[] = []
  const { parc, besoins, achats, fournisseurs, finances } = tableau
  if (finances) {
    kpis.push(
      {
        id: 'engage',
        label: 'Montant engagé',
        value: formatMoney(finances.montantEngage, { compact: true }),
        hint: `${formatTaux(finances.tauxExecutionBudget)} du budget de ${formatMoney(finances.budgetTotal, { compact: true })}`,
        icon: <Wallet />,
        tone: 'primary',
        serie: 'MONTANT_ENGAGE',
        sparkClass: 'text-chart-1',
      },
      {
        id: 'paye',
        label: 'Montant payé',
        value: formatMoney(finances.montantPaye, { compact: true }),
        hint: `Mandaté : ${formatMoney(finances.montantMandate, { compact: true })}`,
        icon: <PackageCheck />,
        tone: 'success',
      },
      {
        id: 'cout-maintenance',
        label: 'Coût de maintenance',
        value: formatMoney(finances.coutMaintenance, { compact: true }),
        hint: `Exploitation : ${formatMoney(finances.coutExploitation, { compact: true })}`,
        icon: <Wrench />,
        tone: 'warning',
        serie: 'COUT_MAINTENANCE',
        sparkClass: 'text-chart-2',
      },
    )
  }
  if (parc) {
    kpis.push(
      {
        id: 'disponibilite',
        label: 'Taux de disponibilité',
        value: formatTaux(parc.tauxDisponibilite),
        hint: `${formatNumber(parc.disponibles)} disponibles sur ${formatNumber(parc.total - parc.reformes)}`,
        icon: <Gauge />,
        tone: 'success',
        serie: 'TAUX_DISPONIBILITE',
        sparkClass: 'text-chart-3',
      },
      {
        id: 'utilisation',
        label: "Taux d'utilisation",
        value: formatTaux(parc.tauxUtilisation),
        hint: `${formatNumber(parc.affectes)} matériels affectés`,
        icon: <Activity />,
        tone: 'primary',
        serie: 'TAUX_UTILISATION',
        sparkClass: 'text-chart-1',
      },
      {
        id: 'pannes',
        label: 'Pannes ouvertes',
        value: formatNumber(parc.pannesOuvertes),
        hint: `${formatNumber(parc.maintenancesEnCours)} maintenances en cours · ${formatNumber(parc.alertesMaintenance)} alertes`,
        icon: <Wrench />,
        tone: parc.pannesOuvertes > 0 ? 'danger' : 'neutral',
      },
    )
  }
  if (besoins) {
    kpis.push({
      id: 'besoins',
      label: 'Besoins en attente',
      value: formatNumber(besoins.enAttente),
      hint:
        besoins.delaiMoyenTraitementHeures != null
          ? `${formatNumber(besoins.total)} soumis · traitement ${formatNumber(besoins.delaiMoyenTraitementHeures, { decimals: 1 })} h`
          : `${formatNumber(besoins.total)} soumis sur la période`,
      icon: <ClipboardList />,
      tone: besoins.enAttente > 0 ? 'warning' : 'neutral',
      serie: 'BESOINS_SOUMIS',
      sparkClass: 'text-chart-4',
    })
  }
  if (achats) {
    kpis.push(
      {
        id: 'delai-appro',
        label: "Délai d'approvisionnement",
        value:
          achats.delaiMoyenApprovisionnementJours != null
            ? `${formatNumber(achats.delaiMoyenApprovisionnementJours, { decimals: 1 })} j`
            : '—',
        hint: `${formatNumber(achats.contratsEnCours)} contrats en cours`,
        icon: <Timer />,
        tone: 'info',
        serie: 'DELAI_APPROVISIONNEMENT',
        sparkClass: 'text-chart-2',
      },
      {
        id: 'retards',
        label: 'Livraisons en retard',
        value: formatNumber(achats.livraisonsEnRetard),
        hint:
          achats.tauxLivraisonALHeure != null
            ? `${formatTaux(achats.tauxLivraisonALHeure)} livrées à l'heure`
            : undefined,
        icon: <Truck />,
        tone: achats.livraisonsEnRetard > 0 ? 'danger' : 'neutral',
      },
    )
  }
  if (fournisseurs) {
    kpis.push({
      id: 'fournisseurs',
      label: 'Fournisseurs actifs',
      value: formatNumber(fournisseurs.actifs),
      hint: `Score moyen ${formatNumber(fournisseurs.scoreMoyen, { decimals: 1 })}/100 · ${formatNumber(fournisseurs.litigesOuverts)} litiges`,
      icon: <Handshake />,
      tone: 'accent',
    })
  }
  return kpis
}
