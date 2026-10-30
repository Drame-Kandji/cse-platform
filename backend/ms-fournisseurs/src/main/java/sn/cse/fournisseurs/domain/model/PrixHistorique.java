package sn.cse.fournisseurs.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entrée de l'historique des prix d'une référence de catalogue.
 *
 * @param id identifiant
 * @param fournisseurId fournisseur
 * @param reference référence article
 * @param designation désignation au moment du changement
 * @param prixUnitaire prix unitaire (FCFA)
 * @param date instant du changement
 */
public record PrixHistorique(
    UUID id,
    UUID fournisseurId,
    String reference,
    String designation,
    BigDecimal prixUnitaire,
    Instant date) {

  /**
   * Instantané du prix courant d'une ligne de catalogue.
   *
   * @param ligne ligne de catalogue
   * @return entrée d'historique
   */
  public static PrixHistorique depuis(LigneCatalogue ligne) {
    return new PrixHistorique(
        UUID.randomUUID(),
        ligne.getFournisseurId(),
        ligne.getReference(),
        ligne.getDesignation(),
        ligne.getPrixUnitaire(),
        ligne.getDateMaj());
  }
}
