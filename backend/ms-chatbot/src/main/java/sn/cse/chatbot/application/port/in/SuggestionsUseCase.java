package sn.cse.chatbot.application.port.in;

import java.util.List;
import sn.cse.common.security.CurrentUser;

/** Questions types proposées selon le rôle. */
public interface SuggestionsUseCase {

  /**
   * @param utilisateur utilisateur courant
   * @return les questions suggérées
   */
  List<String> suggestions(CurrentUser utilisateur);
}
