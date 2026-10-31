package sn.cse.chatbot.application.port.in;

/** Traitement d'une question (diagramme de séquence « chatbot IA »). */
public interface TraiterQuestionUseCase {

  /**
   * @param commande question
   * @return la réponse enregistrée dans l'historique
   */
  ReponseChatbot traiter(QuestionCommand commande);
}
