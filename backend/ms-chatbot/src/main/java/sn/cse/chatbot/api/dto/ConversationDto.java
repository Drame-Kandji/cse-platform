package sn.cse.chatbot.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import sn.cse.chatbot.domain.model.Conversation;
import sn.cse.chatbot.domain.model.RoleMessage;

/**
 * Conversation complète.
 *
 * @param id identifiant
 * @param titre titre
 * @param messages messages chronologiques
 */
@Schema(name = "Conversation")
public record ConversationDto(
    @Schema(example = "5b0f3c1e-8a7d-4e2b-9c41-2d6f8e0a1b3c") UUID id,
    @Schema(example = "Où se trouve la niveleuse NIV-002 ?") String titre,
    List<MessageDto> messages) {

  /**
   * Message d'une conversation.
   *
   * @param id identifiant
   * @param role auteur
   * @param contenu texte
   * @param sources sources citées
   * @param horodatage date
   */
  @Schema(name = "MessageConversation")
  public record MessageDto(
      @Schema(example = "9d8c7b6a-1111-2222-3333-444455556666") UUID id,
      @Schema(example = "ASSISTANT") RoleMessage role,
      @Schema(example = "**Localisation** · 1 matériel trouvé…") String contenu,
      List<SourceDto> sources,
      @Schema(example = "2026-10-30T09:15:00Z") Instant horodatage) {}

  /**
   * @param c conversation
   * @return le DTO
   */
  public static ConversationDto depuis(Conversation c) {
    return new ConversationDto(
        c.id(),
        c.titre(),
        c.messages().stream()
            .map(
                m ->
                    new MessageDto(
                        m.id(),
                        m.role(),
                        m.contenu(),
                        m.sources().stream().map(SourceDto::depuis).toList(),
                        m.horodatage()))
            .toList());
  }
}
