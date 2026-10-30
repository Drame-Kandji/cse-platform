package sn.cse.fournisseurs.application.query;

import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;

/**
 * Filtres de recherche des fournisseurs.
 *
 * @param q texte libre (raison sociale, NINEA, ville, e-mail)
 * @param specialite spécialité requise
 * @param statut statut requis
 */
public record CriteresFournisseurs(
    String q, CategorieMateriel specialite, StatutFournisseur statut) {

  /** Normalise le texte libre. */
  public CriteresFournisseurs {
    q = q == null || q.isBlank() ? null : q.trim();
  }
}
