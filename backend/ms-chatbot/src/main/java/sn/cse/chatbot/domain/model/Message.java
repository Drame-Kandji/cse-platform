package sn.cse.chatbot.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Message d'une conversation.
 *
 * @param id identifiant
 * @param role auteur
 * @param contenu texte (markdown simple pour l'assistant)
 * @param sources sources citées (assistant)
 * @param intention intention détectée (assistant) ou {@code null}
 * @param modeDegrade vrai si la réponse a été produite sans LLM
 * @param horodatage date
 */
public record Message(
    UUID id,
    RoleMessage role,
    String contenu,
    List<Source> sources,
    Intention intention,
    boolean modeDegrade,
    Instant horodatage) {

  public Message {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(contenu, "contenu");
    Objects.requireNonNull(horodatage, "horodatage");
    sources = sources == null ? List.of() : List.copyOf(sources);
  }
}
