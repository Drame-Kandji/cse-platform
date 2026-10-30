package sn.cse.fournisseurs.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.fournisseurs.api.dto.ComparaisonPrixDto;
import sn.cse.fournisseurs.api.dto.LigneCatalogueDto;
import sn.cse.fournisseurs.api.dto.LigneCatalogueRequest;
import sn.cse.fournisseurs.api.dto.PrixHistoriqueDto;
import sn.cse.fournisseurs.api.mapper.DtoMapper;
import sn.cse.fournisseurs.application.command.LigneCatalogueCommand;
import sn.cse.fournisseurs.application.port.in.GererCatalogueUseCase;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;

/** Catalogue de prix, historique et comparaison multi-fournisseurs. */
@RestController
@RequestMapping("/api/fournisseurs")
@Tag(
    name = "Catalogue",
    description = "Catalogue de prix des fournisseurs, historique et comparaison")
public class CatalogueController {

  private final GererCatalogueUseCase catalogue;

  /**
   * @param catalogue cas d'utilisation
   */
  public CatalogueController(GererCatalogueUseCase catalogue) {
    this.catalogue = catalogue;
  }

  /**
   * @param id fournisseur
   * @return lignes
   */
  @GetMapping("/{id}/catalogue")
  @PreAuthorize(Acces.LECTEURS)
  @Operation(summary = "Consulter le catalogue", description = "Lignes triées par référence.")
  @ApiResponse(responseCode = "200", description = "Catalogue")
  public List<LigneCatalogueDto> catalogue(@PathVariable UUID id) {
    return catalogue.catalogue(id).stream().map(DtoMapper::ligne).toList();
  }

  /**
   * @param id fournisseur
   * @param requete ligne
   * @return ligne publiée
   */
  @PostMapping("/{id}/catalogue")
  @PreAuthorize(Acces.RA_SEUL)
  @Operation(
      summary = "Publier une ligne de catalogue",
      description =
          "Upsert par référence ; le prix initial et chaque changement de prix sont historisés. La"
              + " catégorie doit faire partie des spécialités du fournisseur.")
  @ApiResponse(responseCode = "200", description = "Ligne créée ou mise à jour")
  public LigneCatalogueDto publier(
      @PathVariable UUID id, @Valid @RequestBody LigneCatalogueRequest requete) {
    return DtoMapper.ligne(
        catalogue.publierLigne(
            id,
            new LigneCatalogueCommand(
                requete.reference(),
                requete.designation(),
                requete.categorie(),
                requete.prixUnitaire(),
                requete.unite(),
                requete.delaiLivraisonJours())));
  }

  /**
   * @param id fournisseur
   * @param reference référence
   * @return historique
   */
  @GetMapping("/{id}/prix/historique")
  @PreAuthorize(Acces.LECTEURS)
  @Operation(
      summary = "Historique des prix",
      description = "Du plus récent au plus ancien, filtrable par référence.")
  @ApiResponse(responseCode = "200", description = "Historique")
  public List<PrixHistoriqueDto> historique(
      @PathVariable UUID id,
      @Parameter(description = "Référence article", example = "PH-CAT320")
          @RequestParam(required = false)
          String reference) {
    return catalogue.historique(id, reference).stream().map(DtoMapper::prix).toList();
  }

  /**
   * @param categorie catégorie
   * @param q texte
   * @return comparaisons
   */
  @GetMapping("/comparaison-prix")
  @PreAuthorize(Acces.LECTEURS)
  @Operation(
      summary = "Comparer les prix",
      description =
          "Regroupe les lignes des fournisseurs ACTIF par catégorie et désignation (casse et accents"
              + " ignorés) et calcule prix minimal, maximal et moyen.")
  @ApiResponse(responseCode = "200", description = "Comparaisons")
  public List<ComparaisonPrixDto> comparer(
      @Parameter(description = "Catégorie", example = "OUTILLAGE") @RequestParam(required = false)
          CategorieMateriel categorie,
      @Parameter(description = "Texte recherché (désignation ou référence)", example = "groupe")
          @RequestParam(required = false)
          String q) {
    return catalogue.comparer(categorie, q).stream().map(DtoMapper::comparaison).toList();
  }
}
