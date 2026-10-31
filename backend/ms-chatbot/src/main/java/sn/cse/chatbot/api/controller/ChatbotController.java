package sn.cse.chatbot.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.chatbot.api.dto.ConversationDto;
import sn.cse.chatbot.api.dto.ConversationResumeDto;
import sn.cse.chatbot.api.dto.QuestionRequest;
import sn.cse.chatbot.api.dto.ReponseDto;
import sn.cse.chatbot.application.port.in.ConsulterHistoriqueUseCase;
import sn.cse.chatbot.application.port.in.QuestionCommand;
import sn.cse.chatbot.application.port.in.SuggestionsUseCase;
import sn.cse.chatbot.application.port.in.TraiterQuestionUseCase;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.security.Roles;
import sn.cse.common.web.PageableFactory;

/** ChatbotController du diagramme de séquence (contrat §10). */
@RestController
@RequestMapping("/api/chatbot")
@Tag(
    name = "Assistant",
    description = "Assistant matériel et logistique (questions, historique, suggestions)")
@PreAuthorize(
    "hasAnyRole('"
        + Roles.CHEF_CHANTIER
        + "','"
        + Roles.RESPONSABLE_MATERIEL
        + "','"
        + Roles.RESPONSABLE_ACHATS
        + "','"
        + Roles.DAF
        + "','"
        + Roles.DIRECTION_GENERALE
        + "','"
        + Roles.ADMIN_SYSTEME
        + "')")
@ApiResponse(
    responseCode = "401",
    description = "Non authentifié",
    content =
        @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(
    responseCode = "403",
    description = "Rôle insuffisant",
    content =
        @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)))
public class ChatbotController {

  private static final Set<String> TRIABLES = Set.of("updatedAt");

  private final TraiterQuestionUseCase questions;
  private final ConsulterHistoriqueUseCase historique;
  private final SuggestionsUseCase suggestions;
  private final CurrentUserProvider users;
  private final PageableFactory pages;

  /**
   * @param questions traitement des questions
   * @param historique historique
   * @param suggestions suggestions
   * @param users utilisateur courant
   * @param pages pagination
   */
  public ChatbotController(
      TraiterQuestionUseCase questions,
      ConsulterHistoriqueUseCase historique,
      SuggestionsUseCase suggestions,
      CurrentUserProvider users,
      PageableFactory pages) {
    this.questions = questions;
    this.historique = historique;
    this.suggestions = suggestions;
    this.users = users;
    this.pages = pages;
  }

  /**
   * @param requete question
   * @return la réponse
   */
  @PostMapping("/query")
  @Operation(
      summary = "Poser une question",
      description =
          "Analyse l'intention, lit les données réelles des services avec les droits de l'utilisateur,"
              + " filtre selon son rôle et répond (LLM, ou mode dégradé sans LLM). L'échange est"
              + " enregistré dans l'historique.")
  @ApiResponse(responseCode = "200", description = "Réponse de l'assistant")
  @ApiResponse(
      responseCode = "400",
      description = "Question vide ou trop longue",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Conversation inexistante ou appartenant à un autre utilisateur",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "429",
      description = "Trop de questions en peu de temps",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ReponseDto query(@Valid @RequestBody QuestionRequest requete) {
    return ReponseDto.depuis(
        questions.traiter(
            new QuestionCommand(users.require(), requete.question(), requete.conversationId())));
  }

  /**
   * @param page page
   * @param size taille
   * @return mes conversations
   */
  @GetMapping("/historique")
  @Operation(summary = "Lister mes conversations", description = "Les plus récentes d'abord.")
  @ApiResponse(responseCode = "200", description = "Page de conversations")
  public PageResponse<ConversationResumeDto> historique(
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(description = "Taille (max 50)", example = "20") @RequestParam(required = false)
          Integer size) {
    return historique
        .lister(users.require(), pages.query(page, size, null, TRIABLES, "updatedAt,desc"))
        .map(ConversationResumeDto::depuis);
  }

  /**
   * @param id conversation
   * @return la conversation
   */
  @GetMapping("/historique/{id}")
  @Operation(summary = "Consulter une de mes conversations")
  @ApiResponse(responseCode = "200", description = "Conversation complète")
  @ApiResponse(
      responseCode = "404",
      description = "Conversation inexistante ou appartenant à un autre utilisateur",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public ConversationDto detail(@PathVariable UUID id) {
    return ConversationDto.depuis(historique.detail(users.require(), id));
  }

  /**
   * @param id conversation
   */
  @DeleteMapping("/historique/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Supprimer une de mes conversations")
  @ApiResponse(responseCode = "204", description = "Conversation supprimée")
  @ApiResponse(
      responseCode = "404",
      description = "Conversation inexistante ou appartenant à un autre utilisateur",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public void supprimer(@PathVariable UUID id) {
    historique.supprimer(users.require(), id);
  }

  /**
   * @return questions types
   */
  @GetMapping("/suggestions")
  @Operation(summary = "Questions suggérées selon mon rôle")
  @ApiResponse(responseCode = "200", description = "Liste de questions")
  public List<String> suggestions() {
    return suggestions.suggestions(users.require());
  }
}
