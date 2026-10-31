package sn.cse.chatbot.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Question posée à l'assistant.
 *
 * @param question texte libre (1000 caractères au plus, limite configurable)
 * @param conversationId conversation à poursuivre
 */
@Schema(name = "Question")
public record QuestionRequest(
    @Schema(
            description = "Question en langage naturel",
            example = "Où se trouve la niveleuse NIV-002 ?",
            maxLength = 1000)
        @NotBlank(message = "La question est obligatoire")
        String question,
    @Schema(
            description = "Conversation à poursuivre (absente = nouvelle conversation)",
            example = "5b0f3c1e-8a7d-4e2b-9c41-2d6f8e0a1b3c",
            nullable = true)
        UUID conversationId) {}
