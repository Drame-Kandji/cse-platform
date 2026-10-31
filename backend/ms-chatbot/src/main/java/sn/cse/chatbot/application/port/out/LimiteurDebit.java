package sn.cse.chatbot.application.port.out;

import java.util.UUID;

/** Limitation du nombre de questions par utilisateur. */
public interface LimiteurDebit {

  /**
   * Consomme un jeton si possible.
   *
   * @param utilisateurId utilisateur
   * @return vrai si la question est autorisée
   */
  boolean autoriser(UUID utilisateurId);
}
