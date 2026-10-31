package sn.cse.audit.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.audit.api.dto.AuditLogDto;
import sn.cse.audit.api.dto.StatistiquesAuditDto;
import sn.cse.audit.application.port.in.ConsulterAuditUseCase;
import sn.cse.audit.domain.model.CritereRechercheAudit;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.web.PageableFactory;

/** Consultation du journal d'audit, réservée à l'administrateur système (règle R12). */
@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit", description = "Journal d'audit immuable de la plateforme (administrateur)")
@PreAuthorize("hasRole(T(sn.cse.common.security.Roles).ADMIN_SYSTEME)")
@ApiResponse(
    responseCode = "401",
    description = "Jeton absent ou invalide",
    content =
        @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(
    responseCode = "403",
    description = "Rôle ADMIN_SYSTEME requis (R12)",
    content =
        @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)))
public class AuditController {

  /** Champs triables (liste blanche). */
  static final Set<String> TRIABLES =
      Set.of("occurredAt", "service", "action", "entite", "utilisateurNom");

  /** Tri par défaut : actions les plus récentes d'abord. */
  static final String TRI_DEFAUT = "occurredAt,desc";

  private final ConsulterAuditUseCase consulter;
  private final PageableFactory pages;

  /**
   * @param consulter cas d'utilisation de consultation
   * @param pages fabrique de pagination bornée
   */
  public AuditController(ConsulterAuditUseCase consulter, PageableFactory pages) {
    this.consulter = consulter;
    this.pages = pages;
  }

  /**
   * Recherche paginée dans le journal.
   *
   * @return la page d'entrées
   */
  @GetMapping
  @Operation(
      summary = "Rechercher dans le journal d'audit",
      description =
          "Liste paginée (taille max 50) des actions journalisées, filtrable par auteur, entité,"
              + " action, service, période (jours UTC inclus) et texte libre. Tri autorisé : "
              + "occurredAt, service, action, entite, utilisateurNom.")
  @ApiResponse(responseCode = "200", description = "Page d'entrées")
  @ApiResponse(
      responseCode = "400",
      description = "Filtre, période ou tri invalide",
      content =
          @Content(
              mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  public PageResponse<AuditLogDto> rechercher(
      @Parameter(
              description = "Identifiant de l'auteur",
              example = "00000000-0000-0000-0000-000000000201")
          @RequestParam(required = false)
          String utilisateurId,
      @Parameter(description = "Type d'entité", example = "BesoinMateriel")
          @RequestParam(required = false)
          String entite,
      @Parameter(description = "Action (type d'événement)", example = "BESOIN_VALIDE")
          @RequestParam(required = false)
          String action,
      @Parameter(description = "Service émetteur", example = "ms-besoins")
          @RequestParam(required = false)
          String service,
      @Parameter(description = "Premier jour inclus (UTC)", example = "2026-10-01")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate du,
      @Parameter(description = "Dernier jour inclus (UTC)", example = "2026-10-30")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate au,
      @Parameter(description = "Texte libre (résumé, identifiant, auteur…)", example = "BES-2026")
          @RequestParam(required = false)
          String q,
      @Parameter(description = "Index de page (base 0)", example = "0")
          @RequestParam(required = false)
          Integer page,
      @Parameter(description = "Taille de page (max 50)", example = "20")
          @RequestParam(required = false)
          Integer size,
      @Parameter(description = "Tri champ,asc|desc", example = "occurredAt,desc")
          @RequestParam(required = false)
          String sort) {
    PageQuery query = pages.query(page, size, sort, TRIABLES, TRI_DEFAUT);
    CritereRechercheAudit critere =
        new CritereRechercheAudit(utilisateurId, entite, action, service, du, au, q);
    return consulter.rechercher(critere, query).map(AuditLogDto::depuis);
  }

  /**
   * @return les statistiques du journal
   */
  @GetMapping("/statistiques")
  @Operation(
      summary = "Statistiques du journal d'audit",
      description = "Total, entrées du jour, des sept derniers jours et répartition par service.")
  @ApiResponse(responseCode = "200", description = "Statistiques")
  public StatistiquesAuditDto statistiques() {
    return StatistiquesAuditDto.depuis(consulter.statistiques());
  }
}
