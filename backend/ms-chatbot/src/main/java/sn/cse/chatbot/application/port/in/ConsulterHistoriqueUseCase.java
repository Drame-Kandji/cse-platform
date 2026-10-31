package sn.cse.chatbot.application.port.in;

import java.util.UUID;
import sn.cse.chatbot.domain.model.Conversation;
import sn.cse.chatbot.domain.model.ConversationResume;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;

/** Historique des conversations de l'utilisateur courant (jamais celles d'autrui : 404). */
public interface ConsulterHistoriqueUseCase {

  /**
   * @param utilisateur utilisateur courant
   * @param page pagination
   * @return ses conversations, les plus récentes d'abord
   */
  PageResponse<ConversationResume> lister(CurrentUser utilisateur, PageQuery page);

  /**
   * @param utilisateur utilisateur courant
   * @param id conversation
   * @return la conversation complète
   */
  Conversation detail(CurrentUser utilisateur, UUID id);

  /**
   * @param utilisateur utilisateur courant
   * @param id conversation à supprimer
   */
  void supprimer(CurrentUser utilisateur, UUID id);
}
