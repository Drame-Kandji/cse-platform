package sn.cse.fournisseurs.application.port.out;

import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.Litige;

/**
 * Port de publication des événements de domaine (outbox transactionnelle, topic {@code
 * cse.fournisseurs.events}). Doit être appelé dans la transaction du cas d'utilisation.
 */
public interface FournisseurEventPublisher {

  /** Modification du référentiel (type local, hors §11 du contrat : audit et read-models). */
  String FOURNISSEUR_MODIFIE = "FOURNISSEUR_MODIFIE";

  /** Refus d'une demande de blacklistage par la DG (type local, hors §11 du contrat). */
  String FOURNISSEUR_BLACKLISTAGE_REFUSE = "FOURNISSEUR_BLACKLISTAGE_REFUSE";

  /**
   * Publie un événement {@code FournisseurPayload}.
   *
   * @param type type d'événement ({@code EventTypes.*})
   * @param fournisseur état courant
   * @param motif motif ou détail (optionnel)
   */
  void publierFournisseur(String type, Fournisseur fournisseur, String motif);

  /**
   * Publie un événement {@code LitigePayload}.
   *
   * @param type type d'événement ({@code EventTypes.LITIGE_*})
   * @param litige état courant
   */
  void publierLitige(String type, Litige litige);
}
