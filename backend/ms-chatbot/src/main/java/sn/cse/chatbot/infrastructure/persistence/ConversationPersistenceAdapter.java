package sn.cse.chatbot.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import sn.cse.chatbot.application.ParametresChatbot;
import sn.cse.chatbot.application.port.out.ConversationRepository;
import sn.cse.chatbot.domain.model.Conversation;
import sn.cse.chatbot.domain.model.ConversationResume;
import sn.cse.chatbot.domain.model.Message;
import sn.cse.chatbot.domain.model.Source;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Adaptateur JPA du port {@link ConversationRepository}. */
@Component
class ConversationPersistenceAdapter implements ConversationRepository {

  private static final TypeReference<List<Source>> SOURCES = new TypeReference<>() {};
  private static final Sort TRI = Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id"));

  private final ConversationJpaRepository jpa;
  private final ObjectMapper mapper;
  private final ParametresChatbot parametres;

  ConversationPersistenceAdapter(
      ConversationJpaRepository jpa, ObjectMapper mapper, ParametresChatbot parametres) {
    this.jpa = jpa;
    this.mapper = mapper;
    this.parametres = parametres;
  }

  @Override
  public void sauvegarder(Conversation c) {
    ConversationEntity e =
        jpa.findById(c.id())
            .orElseGet(
                () -> new ConversationEntity(c.id(), c.utilisateurId(), c.titre(), c.createdAt()));
    List<Message> messages = c.messages();
    for (int i = e.getMessages().size(); i < messages.size(); i++) {
      Message m = messages.get(i);
      e.getMessages()
          .add(
              new MessageEntity(
                  m.id(),
                  e,
                  i,
                  m.role(),
                  m.contenu(),
                  json(m.sources()),
                  m.intention(),
                  m.modeDegrade(),
                  m.horodatage()));
    }
    ConversationResume r = c.resume(parametres.longueurApercu());
    e.resumer(r.dernierMessage(), r.nombreMessages(), r.updatedAt());
    jpa.save(e);
  }

  @Override
  public Optional<Conversation> parId(UUID id) {
    return jpa.findById(id).map(this::versDomaine);
  }

  @Override
  public PageResponse<ConversationResume> resumes(UUID utilisateurId, PageQuery page) {
    Page<ConversationEntity> p =
        jpa.findByUtilisateurId(utilisateurId, PageRequest.of(page.page(), page.size(), TRI));
    return PageResponse.of(
        p.getContent().stream()
            .map(
                e ->
                    new ConversationResume(
                        e.getId(),
                        e.getTitre(),
                        e.getDernierMessage(),
                        e.getNombreMessages(),
                        e.getUpdatedAt()))
            .toList(),
        page.page(),
        page.size(),
        p.getTotalElements());
  }

  @Override
  public void supprimer(UUID id) {
    jpa.deleteById(id);
  }

  private Conversation versDomaine(ConversationEntity e) {
    return new Conversation(
        e.getId(),
        e.getUtilisateurId(),
        e.getTitre(),
        e.getMessages().stream()
            .map(
                m ->
                    new Message(
                        m.getId(),
                        m.getRole(),
                        m.getContenu(),
                        sources(m.getSources()),
                        m.getIntention(),
                        m.isModeDegrade(),
                        m.getHorodatage()))
            .toList(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getVersion());
  }

  private String json(List<Source> sources) {
    try {
      return mapper.writeValueAsString(sources);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Sources non sérialisables", ex);
    }
  }

  private List<Source> sources(String json) {
    try {
      return mapper.readValue(json, SOURCES);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Sources illisibles en base", ex);
    }
  }
}
