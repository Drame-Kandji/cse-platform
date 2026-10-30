package sn.cse.fournisseurs.domain.model;

/** Types de documents administratifs d'un fournisseur. */
public enum TypeDocument {
  /** Numéro d'identification national des entreprises et associations. */
  NINEA,
  /** Registre du commerce et du crédit mobilier. */
  RCCM,
  /** Attestation de régularité fiscale. */
  QUITUS_FISCAL,
  /** Attestation d'assurance (responsabilité civile, flotte…). */
  ASSURANCE,
  /** Certification qualité (ISO 9001…). */
  CERTIFICATION,
  /** Autre pièce justificative. */
  AUTRE
}
