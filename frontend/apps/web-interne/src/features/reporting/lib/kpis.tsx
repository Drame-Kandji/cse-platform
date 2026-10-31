import { formatMoney, formatNumber } from '@/shared/lib/format'
import type { Profil, SectionParc, Tableau } from '../types'
import { formatTaux } from './format'

/** Indicateur cle pret a afficher (libelle, grand chiffre, une ligne discrete). */
export interface Kpi {
  readonly id: string
  readonly label: string
  readonly value: string
  readonly hint?: string
  /** Vrai seulement pour une alerte reelle (retards) : valeur en rouge. */
  readonly alert?: boolean
}

/** Nombre de KPI du tableau de bord (charte sobre). */
export const DASHBOARD_KPI_COUNT = 4

/** Les 4 indicateurs les plus utiles par profil, par ordre de priorite. */
const KPI_PRIORITE: Readonly<Record<Profil, readonly string[]>> = {
  DG: ['engage', 'disponibilite', 'besoins', 'retards'],
  DAF: ['engage', 'paye', 'cout-maintenance', 'retards'],
  RESPONSABLE_ACHATS: ['besoins', 'delai-appro', 'retards', 'fournisseurs'],
  RESPONSABLE_MATERIEL: ['disponibilite', 'utilisation', 'pannes', 'besoins'],
  CHEF_CHANTIER: ['besoins', 'disponibilite', 'utilisation', 'pannes'],
}

/**
 * Sous-libelle de la disponibilite operationnelle : "39 opérationnels sur 41" (parc hors reformes) ;
 * repli sur les materiels disponibles si l'API ne fournit pas encore `operationnels`.
 */
export function disponibiliteHint(parc: SectionParc): string {
  const parcActif = formatNumber(parc.total - parc.reformes)
  return typeof parc.operationnels === 'number'
    ? `${formatNumber(parc.operationnels)} opérationnels sur ${parcActif}`
    : `${formatNumber(parc.disponibles)} disponibles sur ${parcActif}`
}

/** Tous les indicateurs cles calculables a partir des sections presentes du tableau. */
export function buildKpis(tableau: Tableau): Kpi[] {
  const kpis: Kpi[] = []
  const { parc, besoins, achats, fournisseurs, finances } = tableau
  if (finances) {
    kpis.push(
      {
        id: 'engage',
        label: 'Montant engagé',
        value: formatMoney(finances.montantEngage, { compact: true }),
        hint: `${formatTaux(finances.tauxExecutionBudget)} du budget`,
      },
      {
        id: 'paye',
        label: 'Montant payé',
        value: formatMoney(finances.montantPaye, { compact: true }),
        hint: `Mandaté : ${formatMoney(finances.montantMandate, { compact: true })}`,
      },
      {
        id: 'cout-maintenance',
        label: 'Coût de maintenance',
        value: formatMoney(finances.coutMaintenance, { compact: true }),
      },
    )
  }
  if (parc) {
    kpis.push(
      {
        id: 'disponibilite',
        label: 'Disponibilité opérationnelle',
        value: formatTaux(parc.tauxDisponibilite),
        hint: disponibiliteHint(parc),
      },
      {
        id: 'utilisation',
        label: "Taux d'utilisation",
        value: formatTaux(parc.tauxUtilisation),
        hint: `${formatNumber(parc.affectes)} matériels affectés`,
      },
      {
        id: 'pannes',
        label: 'Pannes ouvertes',
        value: formatNumber(parc.pannesOuvertes),
        hint: `${formatNumber(parc.maintenancesEnCours)} maintenances en cours`,
      },
    )
  }
  if (besoins) {
    kpis.push({
      id: 'besoins',
      label: 'Besoins en attente',
      value: formatNumber(besoins.enAttente),
      hint: `${formatNumber(besoins.total)} soumis`,
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
      },
      {
        id: 'retards',
        label: 'Livraisons en retard',
        value: formatNumber(achats.livraisonsEnRetard),
        alert: achats.livraisonsEnRetard > 0,
      },
    )
  }
  if (fournisseurs) {
    kpis.push({
      id: 'fournisseurs',
      label: 'Fournisseurs actifs',
      value: formatNumber(fournisseurs.actifs),
      hint: `Score moyen ${formatNumber(fournisseurs.scoreMoyen, { decimals: 1 })}/100`,
    })
  }
  return kpis
}

/**
 * Indicateurs du tableau de bord : les plus importants pour le profil (au plus `max`),
 * completes dans l'ordre de lecture si une section manque.
 */
export function dashboardKpis(tableau: Tableau, max = DASHBOARD_KPI_COUNT): Kpi[] {
  const all = buildKpis(tableau)
  const priorite = KPI_PRIORITE[tableau.profil]
  const rank = (kpi: Kpi) => {
    const index = priorite.indexOf(kpi.id)
    return index === -1 ? priorite.length + all.indexOf(kpi) : index
  }
  return [...all].sort((a, b) => rank(a) - rank(b)).slice(0, max)
}
