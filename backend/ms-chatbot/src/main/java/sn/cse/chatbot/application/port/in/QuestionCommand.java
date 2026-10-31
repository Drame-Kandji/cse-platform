package sn.cse.chatbot.application.port.in;

import java.util.Objects;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;

/**
 * Question posée à l'assistant.
 *
 * @param utilisateur utilisateur courant
 * @param question texte libre
 * @param conversationId conversation à poursuivre, ou {@code null} pour en créer une
 */
public record QuestionCommand(CurrentUser utilisateur, String question, UUID conversationId) {

  public QuestionCommand {
    Objects.requireNonNull(utilisateur, "utilisateur");
  }
}
