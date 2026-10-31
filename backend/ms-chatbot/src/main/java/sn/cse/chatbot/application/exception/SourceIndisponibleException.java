package sn.cse.chatbot.application.exception;

/** Un service interrogé est indisponible (panne, délai, circuit ouvert) : réponse dégradée. */
public class SourceIndisponibleException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * @param service service concerné
   * @param cause cause technique
   */
  public SourceIndisponibleException(String service, Throwable cause) {
    super("Service " + service + " indisponible", cause);
  }
}
