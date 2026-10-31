import { labelOf, PRIORITE } from '@/shared/domain/labels'
import { formatMoney, formatNumber } from '@/shared/lib/format'
import type { Tableau } from '../types'
import { formatTaux } from './format'

export interface KpiTable {
  readonly id: 'parc' | 'besoins' | 'achats' | 'fournisseurs' | 'finances'
  readonly title: string
  readonly rows: readonly { label: string; value: string }[]
}

const n = (value: number | null | undefined, decimals = 0) => formatNumber(value, { decimals })

/** Tables de synthese (libelle / valeur) des sections presentes, dans l'ordre de l'export. */
export function kpiTables(tableau: Tableau): KpiTable[] {
  const tables: KpiTable[] = []
  const { parc, besoins, achats, fournisseurs, finances } = tableau
  if (parc) {
    tables.push({
      id: 'parc',
      title: 'Parc matériel',
      rows: [
        { label: 'Matériels', value: n(parc.total) },
        { label: 'Disponibles', value: n(parc.disponibles) },
        { label: 'Affectés', value: n(parc.affectes) },
        { label: 'En maintenance', value: n(parc.enMaintenance) },
        { label: 'Réformés', value: n(parc.reformes) },
        { label: 'Disponibilité opérationnelle', value: formatTaux(parc.tauxDisponibilite) },
        { label: "Taux d'utilisation", value: formatTaux(parc.tauxUtilisation) },
        { label: 'Maintenances en cours', value: n(parc.maintenancesEnCours) },
        { label: 'Pannes ouvertes', value: n(parc.pannesOuvertes) },
        { label: 'Alertes de maintenance', value: n(parc.alertesMaintenance) },
      ],
    })
  }
  if (besoins) {
    tables.push({
      id: 'besoins',
      title: 'Besoins',
      rows: [
        { label: 'Besoins soumis', value: n(besoins.total) },
        { label: 'En attente', value: n(besoins.enAttente) },
        { label: 'Validés', value: n(besoins.valides) },
        { label: 'Rejetés', value: n(besoins.rejetes) },
        { label: 'Affectés directement', value: n(besoins.affectesDirect) },
        { label: 'En achat', value: n(besoins.enAchat) },
        {
          label: 'Délai moyen de traitement',
          value:
            besoins.delaiMoyenTraitementHeures != null
              ? `${n(besoins.delaiMoyenTraitementHeures, 1)} h`
              : '-',
        },
        ...PRIORITE.map((priorite) => ({
          label: `Priorité ${labelOf('priorite', priorite).toLowerCase()}`,
          value: n(besoins.parPriorite[priorite] ?? 0),
        })),
      ],
    })
  }
  if (achats) {
    tables.push({
      id: 'achats',
      title: 'Achats',
      rows: [
        { label: "Appels d'offres publiés", value: n(achats.aoPublies) },
        { label: "Appels d'offres clôturés", value: n(achats.aoClotures) },
        { label: "Appels d'offres attribués", value: n(achats.aoAttribues) },
        { label: 'Contrats en cours', value: n(achats.contratsEnCours) },
        { label: 'Livraisons en retard', value: n(achats.livraisonsEnRetard) },
        {
          label: "Délai moyen d'approvisionnement",
          value:
            achats.delaiMoyenApprovisionnementJours != null
              ? `${n(achats.delaiMoyenApprovisionnementJours, 1)} j`
              : '-',
        },
        { label: "Livraisons à l'heure", value: formatTaux(achats.tauxLivraisonALHeure) },
      ],
    })
  }
  if (fournisseurs) {
    tables.push({
      id: 'fournisseurs',
      title: 'Fournisseurs',
      rows: [
        { label: 'Fournisseurs actifs', value: n(fournisseurs.actifs) },
        { label: 'Score moyen', value: `${n(fournisseurs.scoreMoyen, 1)} / 100` },
        { label: 'Litiges ouverts', value: n(fournisseurs.litigesOuverts) },
      ],
    })
  }
  if (finances) {
    tables.push({
      id: 'finances',
      title: 'Finances',
      rows: [
        { label: 'Budget total', value: formatMoney(finances.budgetTotal) },
        { label: 'Montant engagé', value: formatMoney(finances.montantEngage) },
        { label: 'Montant mandaté', value: formatMoney(finances.montantMandate) },
        { label: 'Montant payé', value: formatMoney(finances.montantPaye) },
        { label: "Taux d'exécution budgétaire", value: formatTaux(finances.tauxExecutionBudget) },
        { label: 'Coût de maintenance', value: formatMoney(finances.coutMaintenance) },
        { label: "Coût d'exploitation", value: formatMoney(finances.coutExploitation) },
      ],
    })
  }
  return tables
}

/** Libelles et unites des indicateurs de series. */
export const INDICATEUR_LABELS = {
  TAUX_DISPONIBILITE: { label: 'Taux de disponibilité', unite: 'pourcentage' },
  TAUX_UTILISATION: { label: "Taux d'utilisation", unite: 'pourcentage' },
  MONTANT_ENGAGE: { label: 'Montant engagé', unite: 'montant' },
  COUT_MAINTENANCE: { label: 'Coût de maintenance', unite: 'montant' },
  BESOINS_SOUMIS: { label: 'Besoins soumis', unite: 'nombre' },
  DELAI_APPROVISIONNEMENT: { label: "Délai moyen d'approvisionnement", unite: 'jours' },
} as const
