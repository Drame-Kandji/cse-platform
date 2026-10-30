package sn.cse.fournisseurs.api.controller;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;
import sn.cse.fournisseurs.api.dto.ActionLitigeRequest;
import sn.cse.fournisseurs.api.dto.LitigeDto;
import sn.cse.fournisseurs.api.dto.LitigeRequest;
import sn.cse.fournisseurs.api.mapper.DtoMapper;
import sn.cse.fournisseurs.application.command.LitigeCommand;
import sn.cse.fournisseurs.application.port.in.GererLitigesUseCase;
import sn.cse.fournisseurs.domain.model.StatutLitige;

/** Litiges fournisseurs : ouverture, chronologie d'actions, résolution ou classement. */
@RestController
@Tag(name = "Litiges", description = "Litiges fournisseurs (références LIT-AAAA-NNN)")
public class LitigeController {

  /** Champs de tri autorisés. */
  static final Set<String> TRIABLES = Set.of("dateOuverture", "reference", "gravite", "statut");

  private static final String TRI_DEFAUT = "dateOuverture,desc";

  private final GererLitigesUseCase litiges;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;

  /**
   * @param litiges cas d'utilisation
   * @param pages pagination
   * @param utilisateurs utilisateur courant
   */
  public LitigeController(
      GererLitigesUseCase litiges, PageableFactory pages, CurrentUserProvider utilisateurs) {
    this.litiges = litiges;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @param statut statut
   * @param fournisseurId fournisseur
   * @param page page
   * @param size taille
   * @param sort tri
   * @return page de litiges
   */
  @GetMapping("/api/litiges")
  @PreAuthorize(Acces.LECTEURS_LITIGES)
  @Operation(
      summary = "Rechercher des litiges",
      description = "Liste paginée ; tri autorisé : dateOuverture, reference, gravite, statut.")
  @ApiResponse(responseCode = "200", description = "Page de litiges")
  public PageResponse<LitigeDto> rechercher(
      @Parameter(description = "Statut", example = "OUVERT") @RequestParam(required = false)
          StatutLitige statut,
      @Parameter(description = "Fournisseur", example = "f0000000-0000-0000-0000-000000000002")
          @RequestParam(required = false)
          UUID fournisseurId,
      @Parameter(description = "Numéro de page", example = "0") @RequestParam(required = false)
          Integer page,
      @Parameter(description = "Taille de page (max 50)", example = "20")
          @RequestParam(required = false)
          Integer size,
      @Parameter(description = "Tri champ,asc|desc", example = "dateOuverture,desc")
          @RequestParam(required = false)
          String sort) {
    return litiges
        .rechercher(statut, fournisseurId, pages.query(page, size, sort, TRIABLES, TRI_DEFAUT))
        .map(DtoMapper::litige);
  }

  /**
   * @param id fournisseur
   * @param requete litige
   * @return litige ouvert
   */
  @PostMapping("/api/fournisseurs/{id}/litiges")
  @PreAuthorize(Acces.RA_RM)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Ouvrir un litige",
      description = "Référence LIT-AAAA-NNN, événement LITIGE_OUVERT et recalcul du score (R07).")
  @ApiResponse(responseCode = "201", description = "Litige ouvert")
  public LitigeDto ouvrir(@PathVariable UUID id, @Valid @RequestBody LitigeRequest requete) {
    return DtoMapper.litige(
        litiges.ouvrir(
            id,
            new LitigeCommand(
                requete.contratId(), requete.objet(), requete.description(), requete.gravite()),
            utilisateurs.require()));
  }

  /**
   * @param id litige
   * @param requete action
   * @return litige
   */
  @PatchMapping("/api/litiges/{id}/action")
  @PreAuthorize(Acces.RA_SEUL)
  @Operation(
      summary = "Agir sur un litige",
      description =
          "ACTION → EN_COURS, RESOLUTION → RESOLU, CLASSEMENT → CLASSE. Un litige clos renvoie 409"
              + " LITIGE_CLOS. La clôture publie LITIGE_RESOLU et recalcule le score (R07).")
  @ApiResponse(responseCode = "200", description = "Litige mis à jour")
  public LitigeDto agir(@PathVariable UUID id, @Valid @RequestBody ActionLitigeRequest requete) {
    return DtoMapper.litige(
        litiges.agir(id, requete.type(), requete.commentaire(), utilisateurs.require()));
  }
}
