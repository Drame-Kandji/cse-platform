package sn.cse.chatbot.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import sn.cse.chatbot.domain.model.ConversationResume;

/**
 * Résumé d'une conversation.
 *
 * @param id identifiant
 * @param titre titre
 * @param dernierMessage aperçu du dernier message
 * @param nombreMessages nombre de messages
 * @param updatedAt dernière activité
 */
@Schema(name = "ConversationResume")
public record ConversationResumeDto(
    @Schema(example = "5b0f3c1e-8a7d-4e2b-9c41-2d6f8e0a1b3c") UUID id,
    @Schema(example = "Où se trouve la niveleuse NIV-002 ?") String titre,
    @Schema(example = "**Localisation** — 1 matériel trouvé…") String dernierMessage,
    @Schema(example = "2") int nombreMessages,
    @Schema(example = "2026-10-30T09:15:00Z") Instant updatedAt) {

  /**
   * @param r résumé
   * @return le DTO
   */
  public static ConversationResumeDto depuis(ConversationResume r) {
    return new ConversationResumeDto(
        r.id(), r.titre(), r.dernierMessage(), r.nombreMessages(), r.updatedAt());
  }
}
