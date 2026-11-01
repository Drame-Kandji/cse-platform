import type { StatusDescriptor } from '@/shared/domain/labels'
import type { ResultatOffre, StatutContrat } from './types'

/**
 * Libelles propres au portail. Les autres enums (facture, categorie, document...) viennent du
 * referentiel partage `@/shared/domain/labels` (StatusBadge).
 * Tons : neutre par defaut, bleu = a traiter / en cours, rouge = rejet ou retard.
 */
export const STATUT_CONTRAT_LABELS: Readonly<Record<StatutContrat, StatusDescriptor>> = {
  BROUILLON: { label: 'En préparation', tone: 'neutral' },
  EN_ATTENTE_SIGNATURE: { label: 'À signer', tone: 'primary' },
  SIGNE: { label: 'Signé', tone: 'neutral' },
  ENGAGEMENT_VALIDE: { label: 'À expédier', tone: 'primary' },
  EN_LIVRAISON: { label: 'En livraison', tone: 'primary' },
  LIVRE: { label: 'Livré', tone: 'neutral' },
  CLOTURE: { label: 'Clôturé', tone: 'neutral' },
}

export const RESULTAT_OFFRE_LABELS: Readonly<Record<ResultatOffre, StatusDescriptor>> = {
  EN_COURS: { label: 'En cours', tone: 'primary' },
  RETENUE: { label: 'Retenue', tone: 'primary' },
  NON_RETENUE: { label: 'Non retenue', tone: 'neutral' },
}

/** Descripteur d'un statut de contrat (valeur inconnue : libelle brut, ton neutre). */
export function describeContrat(statut: string): StatusDescriptor {
  return (
    (STATUT_CONTRAT_LABELS as Readonly<Record<string, StatusDescriptor>>)[statut] ?? {
      label: statut,
      tone: 'neutral',
    }
  )
}
