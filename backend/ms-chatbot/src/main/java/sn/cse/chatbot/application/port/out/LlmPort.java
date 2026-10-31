package sn.cse.chatbot.application.port.out;

/** Modèle de langage (OpenAI-compatible). */
public interface LlmPort {

  /**
   * @return vrai si un modèle est configuré
   */
  boolean estDisponible();

  /**
   * @param requete question et contexte
   * @return le texte généré
   * @throws sn.cse.chatbot.application.exception.LlmIndisponibleException en cas d'échec
   */
  String generer(RequeteLlm requete);
}
