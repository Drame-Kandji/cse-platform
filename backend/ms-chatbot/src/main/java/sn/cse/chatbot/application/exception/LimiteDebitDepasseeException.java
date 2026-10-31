package sn.cse.chatbot.application.exception;

import sn.cse.common.exception.CseException;

/** Trop de questions en peu de temps pour un même utilisateur (HTTP 429). */
public class LimiteDebitDepasseeException extends CseException {

  private static final long serialVersionUID = 1L;

  /** Crée l'exception avec un message français. */
  public LimiteDebitDepasseeException() {
    super("TROP_DE_REQUETES", "Trop de questions en peu de temps. Patientez quelques instants.");
  }

  @Override
  public int status() {
    return 429;
  }
}
