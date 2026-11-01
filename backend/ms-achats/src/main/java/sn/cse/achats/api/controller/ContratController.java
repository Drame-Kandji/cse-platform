package sn.cse.achats.api.controller;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.achats.api.dto.ClausesRequest;
import sn.cse.achats.api.dto.ConfirmerLivraisonRequest;
import sn.cse.achats.api.dto.ContratDto;
import sn.cse.achats.api.dto.CreerContratRequest;
import sn.cse.achats.api.dto.JalonRealiseRequest;
import sn.cse.achats.api.mapper.AchatsApiMapper;
import sn.cse.achats.application.command.ConfirmerLivraisonCommand;
import sn.cse.achats.application.command.ModifierClausesCommand;
import sn.cse.achats.application.port.in.ConsulterContratsUseCase;
import sn.cse.achats.application.port.in.ExecuterContratUseCase;
import sn.cse.achats.application.port.in.GenererContratUseCase;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.domain.model.ClausesContrat;
import sn.cse.achats.domain.model.NouveauJalon;
import sn.cse.achats.domain.model.StatutContrat;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;

/** Contrats d'achat : génération, clauses, signature, livraison, jalons et clôture. */
@RestController
@RequestMapping("/api/contrats")
@Tag(name = "Contrats", description = "Exécution des contrats d'achat (R03, R06)")
public class ContratController {

  private final ConsulterContratsUseCase consultation;
  private final GenererContratUseCase generation;
  private final ExecuterContratUseCase execution;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;
  private final AchatsApiMapper mapper;

  /**
   * @param consultation lectures
   * @param generation génération
   * @param execution exécution
   * @param pages pagination
   * @param utilisateurs utilisateur courant (signataire)
   * @param mapper conversion DTO
   */
  public ContratController(
      ConsulterContratsUseCase consultation,
      GenererContratUseCase generation,
      ExecuterContratUseCase execution,
      PageableFactory pages,
      CurrentUserProvider utilisateurs,
      AchatsApiMapper mapper) {
    this.consultation = consultation;
    this.generation = generation;
    this.execution = execution;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
    this.mapper = mapper;
  }

  /**
   * @param statut statut
   * @param fournisseurId titulaire
   * @param appelOffreId appel d'offres d'origine
   * @param q recherche
   * @param page page
   * @param size taille
   * @param sort tri
   * @return la page
   */
  @GetMapping
  @PreAuthorize(Acces.LECTURE_CONTRATS)
  @Operation(
      summary = "Lister les contrats",
      description =
          "Filtres facultatifs : statut, titulaire, appel d'offres d'origine, texte. Tri autorisé :"
              + " numero, dateSignature, dateLivraisonPrevue, statut, createdAt (défaut"
              + " numero,desc).")
  @ApiResponse(responseCode = "200", description = "Page de contrats")
  public PageResponse<ContratDto> lister(
      @Parameter(example = "SIGNE") @RequestParam(required = false) StatutContrat statut,
      @Parameter(example = "f0000000-0000-0000-0000-000000000001") @RequestParam(required = false)
          UUID fournisseurId,
      @Parameter(example = "d0000000-0000-0000-0000-000000000001") @RequestParam(required = false)
          UUID appelOffreId,
      @Parameter(example = "CTR-2026") @RequestParam(required = false) String q,
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(example = "numero,desc") @RequestParam(required = false) String sort) {
    return consultation
        .rechercher(
            new ContratRepository.Filtre(statut, fournisseurId, appelOffreId, q),
            pages.query(page, size, sort, ContratRepository.TRIABLES, "numero,desc"))
        .map(mapper::contrat);
  }

  /**
   * @param id contrat
   * @return le contrat
   */
  @GetMapping("/{id}")
  @PreAuthorize(Acces.LECTURE_CONTRATS)
  @Operation(summary = "Consulter un contrat")
  @ApiResponse(responseCode = "200", description = "Contrat")
  public ContratDto detail(@PathVariable UUID id) {
    return mapper.contrat(consultation.detail(id));
  }

  /**
   * @param requete appel d'offres
   * @return le contrat généré
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.ACHATS)
  @Operation(
      summary = "Générer le contrat d'un appel d'offres attribué",
      description =
          "Normalement automatique à l'attribution. R03 : AO ATTRIBUE requis (422"
              + " AO_NON_ATTRIBUE) ; un seul contrat par AO (409 CONTRAT_EXISTANT).")
  @ApiResponse(responseCode = "201", description = "Contrat BROUILLON généré")
  public ContratDto generer(@Valid @RequestBody CreerContratRequest requete) {
    return mapper.contrat(generation.generer(requete.appelOffreId()));
  }

  /**
   * @param id contrat
   * @param requete clauses
   * @return le contrat
   */
  @PutMapping("/{id}/clauses")
  @PreAuthorize(Acces.ACHATS)
  @Operation(
      summary = "Rédiger les clauses",
      description =
          "Contrat BROUILLON uniquement (409 CONTRAT_NON_MODIFIABLE). Remplace les jalons.")
  @ApiResponse(responseCode = "200", description = "Contrat mis à jour")
  public ContratDto clauses(@PathVariable UUID id, @Valid @RequestBody ClausesRequest requete) {
    List<NouveauJalon> jalons =
        requete.jalons() == null
            ? List.of()
            : requete.jalons().stream()
                .map(j -> new NouveauJalon(j.libelle(), j.datePrevue()))
                .toList();
    return mapper.contrat(
        execution.modifierClauses(
            new ModifierClausesCommand(
                id,
                new ClausesContrat(
                    requete.garantieMois(),
                    requete.conditionsMaintenance(),
                    requete.conditionsLivraison(),
                    requete.penaliteRetardPctParJour()),
                requete.delaiLivraisonJours(),
                jalons)));
  }

  /**
   * @param id contrat
   * @return le contrat en attente de la signature du fournisseur
   */
  @PostMapping("/{id}/signer")
  @PreAuthorize(Acces.ACHATS)
  @Operation(
      summary = "Signer le contrat pour le CSE",
      description =
          "Signature du CSE : BROUILLON → EN_ATTENTE_SIGNATURE, clauses figées, publie"
              + " CONTRAT_ENVOYE_SIGNATURE. Le contrat passe SIGNE (engagement EN_ATTENTE demandé à"
              + " la DAF) lorsque le fournisseur signe sur le portail. Autre statut : 409"
              + " TRANSITION_INTERDITE.")
  @ApiResponse(responseCode = "200", description = "Contrat EN_ATTENTE_SIGNATURE")
  public ContratDto signer(@PathVariable UUID id) {
    return mapper.contrat(execution.signer(id, utilisateurs.require()));
  }

  /**
   * @param id contrat
   * @return le contrat en livraison
   */
  @PostMapping("/{id}/livraison/demarrer")
  @PreAuthorize(Acces.ACHATS)
  @Operation(
      summary = "Démarrer la livraison (hors portail)",
      description =
          "Secours lorsque le fournisseur ne déclare pas son expédition sur le portail. R06 :"
              + " engagement validé par la DAF requis (422 ENGAGEMENT_NON_VALIDE).")
  @ApiResponse(responseCode = "200", description = "Contrat EN_LIVRAISON")
  public ContratDto demarrerLivraison(@PathVariable UUID id) {
    return mapper.contrat(execution.demarrerLivraison(id));
  }

  /**
   * @param id contrat
   * @param jalonId jalon
   * @param requete date de réalisation
   * @return le contrat
   */
  @PatchMapping("/{id}/jalons/{jalonId}")
  @PreAuthorize(Acces.ACHATS)
  @Operation(summary = "Renseigner la réalisation d'un jalon")
  @ApiResponse(responseCode = "200", description = "Contrat mis à jour")
  public ContratDto jalon(
      @PathVariable UUID id,
      @PathVariable UUID jalonId,
      @Valid @RequestBody JalonRealiseRequest requete) {
    return mapper.contrat(execution.enregistrerJalon(id, jalonId, requete.dateReelle()));
  }

  /**
   * @param id contrat
   * @param requete livraison
   * @return le contrat livré
   */
  @PostMapping("/{id}/livraison/confirmer")
  @PreAuthorize(Acces.ACHATS)
  @Operation(
      summary = "Confirmer la livraison",
      description =
          "EN_LIVRAISON → LIVRE. Pénalités = montant × pénalité journalière × jours de retard"
              + " (plafonnées). Publie LIVRAISON_EFFECTUEE (création des matériels par ms-suivi).")
  @ApiResponse(responseCode = "200", description = "Contrat LIVRE")
  public ContratDto confirmerLivraison(
      @PathVariable UUID id, @Valid @RequestBody ConfirmerLivraisonRequest requete) {
    return mapper.contrat(
        execution.confirmerLivraison(
            new ConfirmerLivraisonCommand(
                id,
                requete.dateLivraisonReelle(),
                requete.numeroBonLivraison(),
                requete.observations())));
  }

  /**
   * @param id contrat
   * @return le contrat clôturé
   */
  @PostMapping("/{id}/cloturer")
  @PreAuthorize(Acces.ACHATS)
  @Operation(
      summary = "Clôturer le contrat",
      description =
          "LIVRE → CLOTURE ; refusé si des factures restent à payer (409 FACTURES_EN_COURS).")
  @ApiResponse(responseCode = "200", description = "Contrat CLOTURE")
  public ContratDto cloturer(@PathVariable UUID id) {
    return mapper.contrat(execution.cloturer(id));
  }
}
