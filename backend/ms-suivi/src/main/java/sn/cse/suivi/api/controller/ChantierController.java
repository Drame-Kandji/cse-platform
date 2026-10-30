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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;
import sn.cse.suivi.api.dto.ChantierDto;
import sn.cse.suivi.api.dto.ChantierRequest;
import sn.cse.suivi.api.mapper.ApiMapper;
import sn.cse.suivi.application.port.in.ConsulterChantiersUseCase;
import sn.cse.suivi.application.port.in.GererChantiersUseCase;
import sn.cse.suivi.domain.model.StatutChantier;

/** Chantiers : consultation (chef : ses chantiers), création, modification, clôture. */
@RestController
@RequestMapping("/api/chantiers")
@Tag(name = "Chantiers", description = "Chantiers de la CSE (lieux d'affectation du parc)")
@ApiResponse(responseCode = "400", description = "Requête invalide")
@ApiResponse(responseCode = "401", description = "Non authentifié")
@ApiResponse(responseCode = "403", description = "Rôle insuffisant ou chantier hors périmètre")
public class ChantierController {

  /** Champs triables. */
  static final Set<String> TRIABLES =
      Set.of("code", "nom", "region", "statut", "dateDebut", "budget");

  private static final String TRI_DEFAUT = "code,asc";

  private final ConsulterChantiersUseCase consultation;
  private final GererChantiersUseCase gestion;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;

  /**
   * @param consultation consultation des chantiers
   * @param gestion gestion des chantiers
   * @param pages fabrique de pagination
   * @param utilisateurs utilisateur courant
   */
  public ChantierController(
      ConsulterChantiersUseCase consultation,
      GererChantiersUseCase gestion,
      PageableFactory pages,
      CurrentUserProvider utilisateurs) {
    this.consultation = consultation;
    this.gestion = gestion;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @param statut statut
   * @param q texte libre
   * @param page page
   * @param size taille
   * @param sort tri
   * @return la page de chantiers
   */
  @GetMapping
  @PreAuthorize(Acces.INTERNES)
  @Operation(
      summary = "Lister les chantiers",
      description = "Un chef de chantier ne voit que les chantiers de son claim chantier_ids.")
  @ApiResponse(responseCode = "200", description = "Page de chantiers")
  public PageResponse<ChantierDto> lister(
      @Parameter(example = "EN_COURS") @RequestParam(required = false) StatutChantier statut,
      @Parameter(description = "Code, nom, localisation ou région", example = "Dakar")
          @RequestParam(required = false)
          String q,
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(
              description = "Tri : code, nom, region, statut, dateDebut, budget",
              example = "nom,asc")
          @RequestParam(required = false)
          String sort) {
    return consultation
        .rechercher(
            statut, q, pages.query(page, size, sort, TRIABLES, TRI_DEFAUT), utilisateurs.require())
        .map(ApiMapper::chantier);
  }

  /**
   * @param id chantier
   * @return le chantier
   */
  @GetMapping("/{id}")
  @PreAuthorize(Acces.INTERNES)
  @Operation(summary = "Consulter un chantier")
  @ApiResponse(responseCode = "200", description = "Chantier")
  @ApiResponse(responseCode = "404", description = "Chantier introuvable")
  public ChantierDto consulter(
      @Parameter(example = "c0000000-0000-0000-0000-000000000001") @PathVariable UUID id) {
    return ApiMapper.chantier(consultation.consulter(id, utilisateurs.require()));
  }

  /**
   * @param requete données
   * @return le chantier créé
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.RM_OU_ADMIN)
  @Operation(
      summary = "Créer un chantier",
      description = "Statut initial EN_COURS ; émet CHANTIER_CREE.")
  @ApiResponse(responseCode = "201", description = "Chantier créé")
  @ApiResponse(responseCode = "409", description = "Code déjà utilisé")
  @ApiResponse(
      responseCode = "422",
      description = "Règle de gestion non respectée (dates, montants)")
  public ChantierDto creer(@Valid @RequestBody ChantierRequest requete) {
    return ApiMapper.chantier(gestion.creer(ApiMapper.donnees(requete), utilisateurs.require()));
  }

  /**
   * @param id chantier
   * @param requete données et version
   * @return le chantier modifié
   */
  @PutMapping("/{id}")
  @PreAuthorize(Acces.RM_OU_ADMIN)
  @Operation(
      summary = "Modifier un chantier",
      description = "Verrou optimiste : version obligatoire ; émet CHANTIER_MODIFIE.")
  @ApiResponse(responseCode = "200", description = "Chantier modifié")
  @ApiResponse(responseCode = "404", description = "Chantier introuvable")
  @ApiResponse(
      responseCode = "409",
      description = "Version obsolète, code déjà utilisé ou chantier terminé")
  public ChantierDto modifier(@PathVariable UUID id, @Valid @RequestBody ChantierRequest requete) {
    return ApiMapper.chantier(
        gestion.modifier(
            id,
            ApiMapper.donnees(requete),
            versionObligatoire(requete.version()),
            utilisateurs.require()));
  }

  /**
   * @param id chantier
   * @return le chantier terminé
   */
  @PatchMapping("/{id}/terminer")
  @PreAuthorize(Acces.RM)
  @Operation(
      summary = "Terminer un chantier",
      description =
          "Refusé (422 CHANTIER_MATERIELS_AFFECTES) si des matériels y sont encore affectés.")
  @ApiResponse(responseCode = "200", description = "Chantier terminé")
  @ApiResponse(responseCode = "404", description = "Chantier introuvable")
  @ApiResponse(responseCode = "409", description = "Chantier déjà terminé")
  @ApiResponse(responseCode = "422", description = "Matériels encore affectés")
  public ChantierDto terminer(@PathVariable UUID id) {
    return ApiMapper.chantier(gestion.terminer(id, utilisateurs.require()));
  }

  /**
   * @param version version reçue
   * @return la version
   * @throws InvalidRequestException si elle est absente (400)
   */
  static long versionObligatoire(Long version) {
    if (version == null) {
      throw new InvalidRequestException(
          "VERSION_OBLIGATOIRE", "La version lue est obligatoire pour une modification.");
    }
    return version;
  }
}
