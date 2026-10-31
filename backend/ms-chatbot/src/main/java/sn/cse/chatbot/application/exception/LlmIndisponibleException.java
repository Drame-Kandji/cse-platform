package sn.cse.chatbot.application.exception;

/** Le LLM n'a pas pu produire de réponse : bascule en mode dégradé. */
public class LlmIndisponibleException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * @param message raison
   * @param cause cause technique (peut être nulle)
   */
  public LlmIndisponibleException(String message, Throwable cause) {
    super(message, cause);
  }
}
