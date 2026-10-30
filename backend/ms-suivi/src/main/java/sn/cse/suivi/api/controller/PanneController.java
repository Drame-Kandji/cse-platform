package sn.cse.suivi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;
import sn.cse.suivi.api.dto.PanneDto;
import sn.cse.suivi.api.dto.PanneRequest;
import sn.cse.suivi.api.mapper.ApiMapper;
import sn.cse.suivi.application.command.SignalerPanneCommande;
import sn.cse.suivi.application.port.in.ConsulterPannesUseCase;
import sn.cse.suivi.application.port.in.SignalerPanneUseCase;
import sn.cse.suivi.domain.model.StatutPanne;

/** Pannes : signalement (chef sur ses chantiers, RM) et consultation. */
@RestController
@RequestMapping("/api/pannes")
@Tag(name = "Pannes", description = "Pannes signalées sur les matériels")
@ApiResponse(responseCode = "400", description = "Requête invalide")
@ApiResponse(responseCode = "401", description = "Non authentifié")
@ApiResponse(responseCode = "403", description = "Rôle insuffisant ou chantier hors périmètre")
public class PanneController {

  /** Champs triables. */
  static final Set<String> TRIABLES = Set.of("dateSignalement", "gravite", "statut");

  private static final String TRI_DEFAUT = "dateSignalement,desc";

  private final ConsulterPannesUseCase consultation;
  private final SignalerPanneUseCase signalement;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;

  /**
   * @param consultation consultation des pannes
   * @param signalement signalement
   * @param pages fabrique de pagination
   * @param utilisateurs utilisateur courant
   */
  public PanneController(
      ConsulterPannesUseCase consultation,
      SignalerPanneUseCase signalement,
      PageableFactory pages,
      CurrentUserProvider utilisateurs) {
    this.consultation = consultation;
    this.signalement = signalement;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @param statut statut
   * @param chantierId chantier
   * @param materielId matériel
   * @param page page
   * @param size taille
   * @param sort tri
   * @return la page de pannes
   */
  @GetMapping
  @PreAuthorize(Acces.INTERNES)
  @Operation(summary = "Lister les pannes")
  @ApiResponse(responseCode = "200", description = "Page de pannes")
  public PageResponse<PanneDto> lister(
      @Parameter(example = "SIGNALEE") @RequestParam(required = false) StatutPanne statut,
      @Parameter(example = "c0000000-0000-0000-0000-000000000001") @RequestParam(required = false)
          UUID chantierId,
      @Parameter(example = "7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d") @RequestParam(required = false)
          UUID materielId,
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(
              description = "Tri : dateSignalement, gravite, statut",
              example = "dateSignalement,desc")
          @RequestParam(required = false)
          String sort) {
    return consultation
        .rechercher(
            statut,
            chantierId,
            materielId,
            pages.query(page, size, sort, TRIABLES, TRI_DEFAUT),
            utilisateurs.require())
        .map(ApiMapper::panne);
  }

  /**
   * @param requete signalement
   * @return la panne créée
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.CHEF_OU_RM)
  @Operation(
      summary = "Signaler une panne",
      description =
          "Un chef de chantier ne peut signaler que sur un matériel présent sur ses chantiers ;"
              + " émet PANNE_SIGNALEE.")
  @ApiResponse(responseCode = "201", description = "Panne signalée")
  @ApiResponse(responseCode = "404", description = "Matériel introuvable")
  @ApiResponse(responseCode = "422", description = "Matériel réformé")
  public PanneDto signaler(@Valid @RequestBody PanneRequest requete) {
    return ApiMapper.panne(
        signalement.signaler(
            new SignalerPanneCommande(
                requete.materielId(),
                requete.gravite(),
                requete.description(),
                utilisateurs.require())));
  }
}
