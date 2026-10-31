package sn.cse.chatbot.application.port.out;

import java.time.LocalDate;
import java.util.List;
import sn.cse.chatbot.domain.model.Intention;
import sn.cse.chatbot.domain.model.Message;

/**
 * Requête au LLM : question et données déjà assainies, à délimiter par l'adaptateur.
 *
 * @param question question de l'utilisateur
 * @param contexte données CSE sérialisées
 * @param intention intention détectée
 * @param profil libellé des rôles de l'utilisateur
 * @param date date du jour
 * @param historique messages précédents de la conversation
 */
public record RequeteLlm(
    String question,
    String contexte,
    Intention intention,
    String profil,
    LocalDate date,
    List<Message> historique) {

  public RequeteLlm {
    historique = List.copyOf(historique);
  }
}
