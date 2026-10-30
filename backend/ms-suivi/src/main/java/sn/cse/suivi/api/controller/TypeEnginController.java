package sn.cse.suivi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.suivi.api.dto.TypeEnginDto;
import sn.cse.suivi.api.dto.TypeEnginRequest;
import sn.cse.suivi.api.mapper.ApiMapper;
import sn.cse.suivi.application.port.in.GererTypesEnginsUseCase;

/** Référentiel des types d'engins (lecture pour tous, écriture réservée à l'administrateur). */
@RestController
@RequestMapping("/api/referentiels/types-engins")
@Tag(name = "Référentiels", description = "Types d'engins et seuils de maintenance préventive")
@ApiResponse(responseCode = "400", description = "Requête invalide")
@ApiResponse(responseCode = "401", description = "Non authentifié")
@ApiResponse(responseCode = "403", description = "Rôle insuffisant")
public class TypeEnginController {

  private final GererTypesEnginsUseCase types;

  /**
   * @param types référentiel des types d'engins
   */
  public TypeEnginController(GererTypesEnginsUseCase types) {
    this.types = types;
  }

  /**
   * @return tous les types d'engins
   */
  @GetMapping
  @PreAuthorize(Acces.INTERNES)
  @Operation(summary = "Lister les types d'engins")
  @ApiResponse(responseCode = "200", description = "Types d'engins par libellé")
  public List<TypeEnginDto> lister() {
    return types.lister().stream().map(ApiMapper::typeEngin).toList();
  }

  /**
   * @param requete données
   * @return le type créé
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.ADMIN)
  @Operation(summary = "Créer un type d'engin")
  @ApiResponse(responseCode = "201", description = "Type créé")
  @ApiResponse(responseCode = "409", description = "Code déjà utilisé")
  @ApiResponse(responseCode = "422", description = "Seuils invalides")
  public TypeEnginDto creer(@Valid @RequestBody TypeEnginRequest requete) {
    return ApiMapper.typeEngin(types.creer(ApiMapper.donnees(requete)));
  }

  /**
   * @param id type
   * @param requete données
   * @return le type modifié
   */
  @PutMapping("/{id}")
  @PreAuthorize(Acces.ADMIN)
  @Operation(
      summary = "Modifier un type d'engin",
      description = "Les nouveaux seuils de maintenance sont répercutés sur les matériels du type.")
  @ApiResponse(responseCode = "200", description = "Type modifié")
  @ApiResponse(responseCode = "404", description = "Type introuvable")
  @ApiResponse(responseCode = "409", description = "Code déjà utilisé")
  @ApiResponse(
      responseCode = "422",
      description = "Changement de catégorie impossible (matériels rattachés)")
  public TypeEnginDto modifier(
      @Parameter(example = "e0000000-0000-0000-0000-000000000001") @PathVariable UUID id,
      @Valid @RequestBody TypeEnginRequest requete) {
    return ApiMapper.typeEngin(types.modifier(id, ApiMapper.donnees(requete)));
  }
}
