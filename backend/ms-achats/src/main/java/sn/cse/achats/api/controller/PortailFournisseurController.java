package sn.cse.achats.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.achats.api.dto.AppelOffrePortailDto;
import sn.cse.achats.api.dto.CreerFactureRequest;
import sn.cse.achats.api.dto.FactureDto;
import sn.cse.achats.api.dto.MarchePortailDto;
import sn.cse.achats.api.dto.OffrePortailDto;
import sn.cse.achats.api.dto.OffrePortailRequest;
import sn.cse.achats.api.mapper.AchatsApiMapper;
import sn.cse.achats.application.command.RecevoirFactureCommand;
import sn.cse.achats.application.port.in.PortailFournisseurUseCase;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;

/**
 * Portail fournisseur : données filtrées sur le claim {@code fournisseur_id} du jeton (403
 * COMPTE_NON_RATTACHE s'il est absent).
 */
@RestController
@RequestMapping("/api/portail")
@Tag(name = "Portail fournisseur", description = "Consultations et dépôts du fournisseur connecté")
public class PortailFournisseurController {

  private final PortailFournisseurUseCase portail;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;
  private final AchatsApiMapper mapper;

  /**
   * @param portail cas d'utilisation du portail
   * @param pages pagination
   * @param utilisateurs utilisateur courant
   * @param mapper conversion DTO
   */
  public PortailFournisseurController(
      PortailFournisseurUseCase portail,
      PageableFactory pages,
      CurrentUserProvider utilisateurs,
      AchatsApiMapper mapper) {
    this.portail = portail;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
    this.mapper = mapper;
  }

  /**
   * @param page page
   * @param size taille
   * @param sort tri
   * @return les appels d'offres ouverts
   */
  @GetMapping("/appels-offres")
  @PreAuthorize(Acces.FOURNISSEUR)
  @Operation(
      summary = "Appels d'offres ouverts",
      description =
          "Appels d'offres publiés dont la date limite n'est pas atteinte : procédures ouvertes et"
              + " demandes de prix où le fournisseur est invité.")
  @ApiResponse(responseCode = "200", description = "Page d'appels d'offres")
  public PageResponse<AppelOffrePortailDto> appelsOffres(
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(example = "dateLimite,asc") @RequestParam(required = false) String sort) {
    CurrentUser utilisateur = utilisateurs.require();
    UUID fournisseur = utilisateur.fournisseurId();
    return portail
        .appelsOffresOuverts(
            utilisateur,
            pages.query(page, size, sort, AppelOffreRepository.TRIABLES, "dateLimite,asc"))
        .map(a -> mapper.appelOffrePortail(a, fournisseur));
  }

  /**
   * @param id appel d'offres
   * @param requete offre
   * @return l'offre déposée
   */
  @PostMapping("/appels-offres/{id}/offres")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.FOURNISSEUR)
  @Operation(
      summary = "Déposer une offre",
      description =
          "R02 : refus après la date limite (422 DATE_LIMITE_DEPASSEE). R14 : fournisseur actif"
              + " requis (422 FOURNISSEUR_NON_ELIGIBLE). Une offre par appel d'offres (409).")
  @ApiResponse(responseCode = "201", description = "Offre déposée")
  public OffrePortailDto deposer(
      @PathVariable UUID id, @Valid @RequestBody OffrePortailRequest requete) {
    return mapper.offrePortail(
        portail.deposerOffre(
            utilisateurs.require(),
            id,
            requete.montantPropose(),
            requete.delaiLivraisonJours(),
            requete.garantieMois(),
            requete.description()));
  }

  /**
   * @param page page
   * @param size taille
   * @param sort tri
   * @return les offres du fournisseur
   */
  @GetMapping("/offres/mes-offres")
  @PreAuthorize(Acces.FOURNISSEUR)
  @Operation(summary = "Mes offres", description = "Offres déposées par le fournisseur connecté.")
  @ApiResponse(responseCode = "200", description = "Page d'offres")
  public PageResponse<OffrePortailDto> mesOffres(
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(example = "datePublication,desc") @RequestParam(required = false) String sort) {
    return portail
        .mesOffres(
            utilisateurs.require(),
            pages.query(page, size, sort, AppelOffreRepository.TRIABLES, "datePublication,desc"))
        .map(mapper::offrePortail);
  }

  /**
   * @param page page
   * @param size taille
   * @return les marchés attribués au fournisseur
   */
  @GetMapping("/attributions")
  @PreAuthorize(Acces.FOURNISSEUR)
  @Operation(summary = "Mes marchés", description = "Contrats attribués au fournisseur connecté.")
  @ApiResponse(responseCode = "200", description = "Page de marchés")
  public PageResponse<MarchePortailDto> attributions(
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size) {
    return portail
        .attributions(
            utilisateurs.require(),
            pages.query(page, size, null, ContratRepository.TRIABLES, "numero,desc"))
        .map(mapper::marchePortail);
  }

  /**
   * @param page page
   * @param size taille
   * @return les factures du fournisseur
   */
  @GetMapping("/factures")
  @PreAuthorize(Acces.FOURNISSEUR)
  @Operation(summary = "Mes factures", description = "Factures du fournisseur connecté.")
  @ApiResponse(responseCode = "200", description = "Page de factures")
  public PageResponse<FactureDto> factures(
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size) {
    return portail
        .factures(
            utilisateurs.require(),
            pages.query(page, size, null, FactureRepository.TRIABLES, "dateEmission,desc"))
        .map(mapper::facture);
  }

  /**
   * @param requete facture
   * @return la facture RECUE
   */
  @PostMapping("/factures")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.FOURNISSEUR)
  @Operation(
      summary = "Déposer une facture",
      description = "Uniquement sur un contrat du fournisseur connecté (404 sinon).")
  @ApiResponse(responseCode = "201", description = "Facture RECUE")
  public FactureDto deposerFacture(@Valid @RequestBody CreerFactureRequest requete) {
    return mapper.facture(
        portail.deposerFacture(
            new RecevoirFactureCommand(
                requete.contratId(),
                requete.numero(),
                requete.montantHt(),
                requete.tva(),
                requete.dateEmission(),
                requete.dateEcheance(),
                utilisateurs.require())));
  }
}
