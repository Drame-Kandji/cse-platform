package sn.cse.chatbot.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import sn.cse.chatbot.application.port.in.ReponseChatbot;
import sn.cse.chatbot.domain.model.Intention;

/**
 * Réponse de l'assistant (contrat §10).
 *
 * @param conversationId conversation
 * @param messageId message
 * @param reponse markdown simple
 * @param sources sources citées
 * @param intention intention détectée
 * @param modeDegrade vrai si la réponse a été produite sans LLM
 * @param horodatage date
 */
@Schema(name = "Reponse")
public record ReponseDto(
    @Schema(example = "5b0f3c1e-8a7d-4e2b-9c41-2d6f8e0a1b3c") UUID conversationId,
    @Schema(example = "9d8c7b6a-1111-2222-3333-444455556666") UUID messageId,
    @Schema(
            description = "Réponse en markdown simple",
            example =
                "**Localisation** · 1 matériel trouvé :\n- **NIV-002** · Niveleuse CAT 140K : sur le chantier **Contournement de Thiès RN2** (affecté)")
        String reponse,
    List<SourceDto> sources,
    @Schema(example = "LOCALISER_ENGIN") Intention intention,
    @Schema(description = "Vrai si aucun LLM n'a été utilisé", example = "true")
        boolean modeDegrade,
    @Schema(example = "2026-10-30T09:15:00Z") Instant horodatage) {

  /**
   * @param r réponse applicative
   * @return le DTO
   */
  public static ReponseDto depuis(ReponseChatbot r) {
    return new ReponseDto(
        r.conversationId(),
        r.messageId(),
        r.reponse(),
        r.sources().stream().map(SourceDto::depuis).toList(),
        r.intention(),
        r.modeDegrade(),
        r.horodatage());
  }
}
