package sn.cse.chatbot.application.port.out;

import java.util.Optional;
import java.util.UUID;
import sn.cse.chatbot.domain.model.Conversation;
import sn.cse.chatbot.domain.model.ConversationResume;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Persistance des conversations. */
public interface ConversationRepository {

  /**
   * @param conversation conversation (nouveaux messages ajoutés)
   */
  void sauvegarder(Conversation conversation);

  /**
   * @param id identifiant
   * @return la conversation, si elle existe
   */
  Optional<Conversation> parId(UUID id);

  /**
   * @param utilisateurId propriétaire
   * @param page pagination (tri : dernière activité décroissante)
   * @return ses conversations résumées
   */
  PageResponse<ConversationResume> resumes(UUID utilisateurId, PageQuery page);

  /**
   * @param id conversation à supprimer
   */
  void supprimer(UUID id);
}
