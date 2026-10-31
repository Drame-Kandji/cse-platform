package sn.cse.chatbot.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.chatbot.application.ParametresChatbot;
import sn.cse.chatbot.application.port.in.ConsulterHistoriqueUseCase;
import sn.cse.chatbot.application.port.out.ConversationRepository;
import sn.cse.chatbot.domain.model.Conversation;
import sn.cse.chatbot.domain.model.ConversationResume;
import sn.cse.chatbot.domain.model.Intention;
import sn.cse.chatbot.domain.model.Message;
import sn.cse.chatbot.domain.model.Source;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;

/**
 * HistoriqueService du diagramme de séquence : persistance des échanges et consultation. Une
 * conversation d'un autre utilisateur est traitée comme inexistante (404).
 */
@Service
public class HistoriqueService implements ConsulterHistoriqueUseCase {

  private final ConversationRepository conversations;
  private final ParametresChatbot parametres;

  /**
   * @param conversations persistance
   * @param parametres paramètres
   */
  public HistoriqueService(ConversationRepository conversations, ParametresChatbot parametres) {
    this.conversations = conversations;
    this.parametres = parametres;
  }

  /**
   * Échange enregistré.
   *
   * @param conversationId conversation
   * @param reponse message de l'assistant
   */
  public record Echange(UUID conversationId, Message reponse) {}

  /**
   * @param utilisateur utilisateur courant
   * @param conversationId conversation demandée, ou {@code null}
   * @return la conversation existante (vide si nouvelle)
   * @throws ResourceNotFoundException si elle n'existe pas ou appartient à un autre utilisateur
   */
  @Transactional(readOnly = true)
  public Optional<Conversation> charger(CurrentUser utilisateur, UUID conversationId) {
    if (conversationId == null) {
      return Optional.empty();
    }
    return Optional.of(possedee(utilisateur, conversationId));
  }

  /**
   * @param utilisateur utilisateur courant
   * @param conversationId conversation à poursuivre ou {@code null}
   * @param question question posée
   * @param reponse réponse générée
   * @param sources sources citées
   * @param intention intention détectée
   * @param maintenant date
   * @return l'échange enregistré
   */
  @Transactional
  public Echange enregistrer(
      CurrentUser utilisateur,
      UUID conversationId,
      String question,
      ReponseGeneree reponse,
      List<Source> sources,
      Intention intention,
      Instant maintenant) {
    Conversation c =
        conversationId == null
            ? Conversation.demarrer(
                utilisateur.id(), question, maintenant, parametres.longueurTitre())
            : possedee(utilisateur, conversationId);
    c.ajouterQuestion(question, maintenant);
    Message m =
        c.ajouterReponse(reponse.texte(), sources, intention, reponse.modeDegrade(), maintenant);
    conversations.sauvegarder(c);
    return new Echange(c.id(), m);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ConversationResume> lister(CurrentUser utilisateur, PageQuery page) {
    return conversations.resumes(utilisateur.id(), page);
  }

  @Override
  @Transactional(readOnly = true)
  public Conversation detail(CurrentUser utilisateur, UUID id) {
    return possedee(utilisateur, id);
  }

  @Override
  @Transactional
  public void supprimer(CurrentUser utilisateur, UUID id) {
    conversations.supprimer(possedee(utilisateur, id).id());
  }

  private Conversation possedee(CurrentUser utilisateur, UUID id) {
    return conversations
        .parId(id)
        .filter(c -> c.appartientA(utilisateur.id()))
        .orElseThrow(() -> new ResourceNotFoundException("Conversation", id));
  }
}
