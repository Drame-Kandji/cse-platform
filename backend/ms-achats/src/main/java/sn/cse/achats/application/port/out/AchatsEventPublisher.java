package sn.cse.achats.application.port.out;

import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.Facture;

/**
 * Publication des événements de domaine (outbox transactionnelle, topic {@code cse.achats.events}).
 * Doit être appelé dans la transaction du cas d'utilisation.
 */
public interface AchatsEventPublisher {

  /**
   * @param type type d'événement (AO_PUBLIE, OFFRE_DEPOSEE, AO_CLOTURE, ATTRIBUTION_*,
   *     MARCHE_ATTRIBUE)
   * @param appelOffre état après l'opération
   */
  void appelOffre(String type, AppelOffre appelOffre);

  /**
   * @param type type d'événement (CONTRAT_*, ENGAGEMENT_*, LIVRAISON_*, ALERTE_RETARD_LIVRAISON)
   * @param contrat état après l'opération
   */
  void contrat(String type, ContratAchat contrat);

  /**
   * @param type type d'événement (FACTURE_*)
   * @param facture état après l'opération
   */
  void facture(String type, Facture facture);
}
