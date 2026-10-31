package sn.cse.notification.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;
import sn.cse.notification.api.dto.CompteurDto;
import sn.cse.notification.api.dto.NotificationDto;
import sn.cse.notification.api.sse.AbonnementsSse;
import sn.cse.notification.application.port.in.ConsulterNotificationsUseCase;

/** Notifications de l'utilisateur courant (contrat §7). */
@RestController
@RequestMapping("/api/notifications")
@Tag(
    name = "Notifications",
    description = "Notifications in-app de l'utilisateur et flux temps réel")
@PreAuthorize(AccesInternes.EXPRESSION)
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
public class NotificationController {

  private static final Set<String> TRIABLES = Set.of("createdAt");

  private final ConsulterNotificationsUseCase useCase;
  private final CurrentUserProvider users;
  private final PageableFactory pages;
  private final AbonnementsSse abonnements;

  /**
   * @param useCase consultation
   * @param users utilisateur courant
   * @param pages pagination
   * @param abonnements flux SSE
   */
  public NotificationController(
      ConsulterNotificationsUseCase useCase,
      CurrentUserProvider users,
      PageableFactory pages,
      AbonnementsSse abonnements) {
    this.useCase = useCase;
    this.users = users;
    this.pages = pages;
    this.abonnements = abonnements;
  }

  /**
   * @param lue filtre de lecture
   * @param page page
   * @param size taille
   * @return les notifications, plus récentes d'abord
   */
  @GetMapping
  @Operation(
      summary = "Lister mes notifications",
      description =
          "Notifications adressées à l'utilisateur ou à l'un de ses rôles, les plus récentes d'abord.")
  @ApiResponse(responseCode = "200", description = "Page de notifications")
  @ApiResponse(
      responseCode = "400",
      description = "Paramètre invalide",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public PageResponse<NotificationDto> lister(
      @Parameter(description = "Filtre sur l'état de lecture", example = "false")
          @RequestParam(required = false)
          Boolean lue,
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(description = "Taille (max 50)", example = "20") @RequestParam(required = false)
          Integer size) {
    return useCase
        .lister(users.require(), lue, pages.query(page, size, null, TRIABLES, "createdAt,desc"))
        .map(NotificationDto::depuis);
  }

  /**
   * @return le nombre de non lues
   */
  @GetMapping("/non-lues/count")
  @Operation(summary = "Compter mes notifications non lues")
  @ApiResponse(responseCode = "200", description = "Compteur")
  public CompteurDto compter() {
    return new CompteurDto(useCase.compterNonLues(users.require()));
  }

  /**
   * @param id notification
   * @return la notification lue
   */
  @PatchMapping("/{id}/lue")
  @Operation(summary = "Marquer une notification comme lue", description = "Idempotent.")
  @ApiResponse(responseCode = "200", description = "Notification marquée comme lue")
  @ApiResponse(
      responseCode = "404",
      description = "Notification inexistante ou destinée à un autre utilisateur",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public NotificationDto marquerLue(@PathVariable UUID id) {
    return NotificationDto.depuis(useCase.marquerLue(users.require(), id));
  }

  /** Marque toutes les notifications visibles comme lues. */
  @PostMapping("/tout-lire")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Tout marquer comme lu")
  @ApiResponse(responseCode = "204", description = "Toutes les notifications sont lues")
  public void toutLire() {
    useCase.toutLire(users.require());
  }

  /**
   * @param response réponse HTTP (en-têtes anti-mise en mémoire tampon)
   * @return le flux SSE
   */
  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(
      summary = "Flux temps réel des notifications (SSE)",
      description =
          "Événements « notification » (données = Notification) et « ping » périodique. Le client"
              + " utilise fetch en streaming avec l'en-tête Authorization (jamais de jeton dans l'URL).")
  @ApiResponse(responseCode = "200", description = "Flux text/event-stream ouvert")
  public SseEmitter stream(HttpServletResponse response) {
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
    // Désactive la mise en mémoire tampon des proxys (Nginx) pour une diffusion immédiate.
    response.setHeader("X-Accel-Buffering", "no");
    return abonnements.abonner(users.require());
  }
}
