package sn.cse.chatbot.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Appel d'offres (lecture ms-achats).
 *
 * @param id identifiant
 * @param reference référence
 * @param objet objet
 * @param statut statut
 * @param dateLimite date limite de dépôt
 * @param nombreOffres offres reçues
 * @param montantAttribue montant attribué ou {@code null} (masqué ou non attribué)
 */
public record AppelOffreInfo(
    String id,
    String reference,
    String objet,
    String statut,
    Instant dateLimite,
    int nombreOffres,
    BigDecimal montantAttribue) {

  /**
   * @return une copie sans montant
   */
  public AppelOffreInfo sansMontant() {
    return new AppelOffreInfo(id, reference, objet, statut, dateLimite, nombreOffres, null);
  }
}
