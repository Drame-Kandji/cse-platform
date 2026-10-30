package sn.cse.fournisseurs.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.fournisseurs.api.dto.FournisseurDetailDto;
import sn.cse.fournisseurs.api.dto.LigneCatalogueDto;
import sn.cse.fournisseurs.api.dto.LigneCatalogueRequest;
import sn.cse.fournisseurs.api.dto.ModifierFournisseurRequest;
import sn.cse.fournisseurs.api.mapper.DtoMapper;
import sn.cse.fournisseurs.application.command.LigneCatalogueCommand;
import sn.cse.fournisseurs.application.port.in.PortailFournisseurUseCase;

/** Portail fournisseur (SPEC §B.6) : profil et catalogue du fournisseur connecté. */
@RestController
@RequestMapping("/api/portail")
@Tag(
    name = "Portail fournisseur",
    description = "Profil et catalogue du fournisseur connecté (claim fournisseur_id)")
public class PortailController {

  private final PortailFournisseurUseCase portail;
  private final CurrentUserProvider utilisateurs;

  /**
   * @param portail cas d'utilisation
   * @param utilisateurs utilisateur courant
   */
  public PortailController(PortailFournisseurUseCase portail, CurrentUserProvider utilisateurs) {
    this.portail = portail;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @return profil
   */
  @GetMapping("/profil")
  @PreAuthorize(Acces.FOURNISSEUR)
  @Operation(
      summary = "Consulter mon profil",
      description =
          "Profil du fournisseur associé au compte, documents et coordonnées bancaires inclus.")
  @ApiResponse(responseCode = "200", description = "Profil")
  public FournisseurDetailDto profil() {
    return DtoMapper.detail(portail.profil(utilisateurs.require()));
  }

  /**
   * @param requete nouvelles informations + version
   * @return profil modifié
   */
  @PutMapping("/profil")
  @PreAuthorize(Acces.FOURNISSEUR)
  @Operation(
      summary = "Modifier mon profil",
      description =
          "Mise à jour des informations légales et de contact (verrou optimiste) ; le statut n'est pas modifiable.")
  @ApiResponse(responseCode = "200", description = "Profil modifié")
  public FournisseurDetailDto modifierProfil(
      @Valid @RequestBody ModifierFournisseurRequest requete) {
    return DtoMapper.detail(
        portail.modifierProfil(
            utilisateurs.require(), requete.version(), DtoMapper.commande(requete)));
  }

  /**
   * @return catalogue
   */
  @GetMapping("/catalogue")
  @PreAuthorize(Acces.FOURNISSEUR)
  @Operation(
      summary = "Consulter mon catalogue",
      description = "Lignes du catalogue de prix du fournisseur connecté.")
  @ApiResponse(responseCode = "200", description = "Catalogue")
  public List<LigneCatalogueDto> catalogue() {
    return portail.catalogue(utilisateurs.require()).stream().map(DtoMapper::ligne).toList();
  }

  /**
   * @param requete ligne
   * @return ligne publiée
   */
  @PutMapping("/catalogue")
  @PreAuthorize(Acces.FOURNISSEUR)
  @Operation(
      summary = "Publier une ligne de mon catalogue",
      description = "Upsert par référence ; tout changement de prix est historisé.")
  @ApiResponse(responseCode = "200", description = "Ligne créée ou mise à jour")
  public LigneCatalogueDto publier(@Valid @RequestBody LigneCatalogueRequest requete) {
    return DtoMapper.ligne(
        portail.publierLigne(
            utilisateurs.require(),
            new LigneCatalogueCommand(
                requete.reference(),
                requete.designation(),
                requete.categorie(),
                requete.prixUnitaire(),
                requete.unite(),
                requete.delaiLivraisonJours())));
  }
}
