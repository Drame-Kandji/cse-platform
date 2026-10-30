package sn.cse.suivi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;
import sn.cse.suivi.api.dto.AffectationDto;
import sn.cse.suivi.api.mapper.ApiMapper;
import sn.cse.suivi.application.port.in.ConsulterAffectationsUseCase;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.StatutAffectation;

/** Consultation des affectations et du planning (chef : ses chantiers). */
@RestController
@RequestMapping("/api/affectations")
@Tag(name = "Affectations", description = "Affectations des matériels aux chantiers")
@ApiResponse(responseCode = "400", description = "Requête invalide")
@ApiResponse(responseCode = "401", description = "Non authentifié")
@ApiResponse(responseCode = "403", description = "Rôle insuffisant ou chantier hors périmètre")
public class AffectationController {

  /** Champs triables. */
  static final Set<String> TRIABLES = Set.of("dateDebut", "dateFinPrevue", "statut");

  private static final String TRI_DEFAUT = "dateDebut,desc";

  private final ConsulterAffectationsUseCase consultation;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;

  /**
   * @param consultation consultation des affectations
   * @param pages fabrique de pagination
   * @param utilisateurs utilisateur courant
   */
  public AffectationController(
      ConsulterAffectationsUseCase consultation,
      PageableFactory pages,
      CurrentUserProvider utilisateurs) {
    this.consultation = consultation;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @param chantierId chantier
   * @param materielId matériel
   * @param statut statut
   * @param page page
   * @param size taille
   * @param sort tri
   * @return la page d'affectations
   */
  @GetMapping
  @PreAuthorize(Acces.INTERNES)
  @Operation(summary = "Lister les affectations")
  @ApiResponse(responseCode = "200", description = "Page d'affectations")
  public PageResponse<AffectationDto> lister(
      @Parameter(example = "c0000000-0000-0000-0000-000000000001") @RequestParam(required = false)
          UUID chantierId,
      @Parameter(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") @RequestParam(required = false)
          UUID materielId,
      @Parameter(example = "EN_COURS") @RequestParam(required = false) StatutAffectation statut,
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(description = "Tri : dateDebut, dateFinPrevue, statut", example = "dateDebut,desc")
          @RequestParam(required = false)
          String sort) {
    return consultation
        .rechercher(
            chantierId,
            materielId,
            statut,
            pages.query(page, size, sort, TRIABLES, TRI_DEFAUT),
            utilisateurs.require())
        .map(ApiMapper::affectation);
  }

  /**
   * @param chantierId chantier
   * @param categorie catégorie
   * @param du début
   * @param au fin
   * @return les affectations de la période
   */
  @GetMapping("/planning")
  @PreAuthorize(Acces.INTERNES)
  @Operation(
      summary = "Planning des affectations",
      description =
          "Affectations dont la période (réelle si terminée) chevauche [du, au] ; amplitude"
              + " maximale 366 jours (400 PERIODE_TROP_LONGUE). Défaut : J-30 à J+90.")
  @ApiResponse(responseCode = "200", description = "Affectations triées par date de début")
  public List<AffectationDto> planning(
      @Parameter(example = "c0000000-0000-0000-0000-000000000001") @RequestParam(required = false)
          UUID chantierId,
      @Parameter(example = "MATERIEL_ROULANT") @RequestParam(required = false)
          CategorieMateriel categorie,
      @Parameter(example = "2026-10-01")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate du,
      @Parameter(example = "2026-12-31")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate au) {
    return consultation.planning(chantierId, categorie, du, au, utilisateurs.require()).stream()
        .map(ApiMapper::affectation)
        .toList();
  }
}
