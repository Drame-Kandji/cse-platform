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
import sn.cse.achats.api.dto.CreerFactureRequest;
import sn.cse.achats.api.dto.FactureDto;
import sn.cse.achats.api.dto.MandaterRequest;
import sn.cse.achats.api.dto.RejeterRequest;
import sn.cse.achats.api.mapper.AchatsApiMapper;
import sn.cse.achats.application.command.RecevoirFactureCommand;
import sn.cse.achats.application.port.in.ConsulterFinanceUseCase;
import sn.cse.achats.application.port.in.GererFacturesUseCase;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.domain.model.StatutFacture;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;

/** Factures fournisseurs : réception, mandatement, paiement, rejet. */
@RestController
@RequestMapping("/api/factures")
@Tag(name = "Factures", description = "Traitement des factures fournisseurs")
public class FactureController {

  private final GererFacturesUseCase factures;
  private final ConsulterFinanceUseCase consultation;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;
  private final AchatsApiMapper mapper;

  /**
   * @param factures traitement
   * @param consultation lectures
   * @param pages pagination
   * @param utilisateurs utilisateur courant
   * @param mapper conversion DTO
   */
  public FactureController(
      GererFacturesUseCase factures,
      ConsulterFinanceUseCase consultation,
      PageableFactory pages,
      CurrentUserProvider utilisateurs,
      AchatsApiMapper mapper) {
    this.factures = factures;
    this.consultation = consultation;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
    this.mapper = mapper;
  }

  /**
   * @param statut statut
   * @param fournisseurId fournisseur
   * @param enRetard filtre de retard
   * @param page page
   * @param size taille
   * @param sort tri
   * @return la page
   */
  @GetMapping
  @PreAuthorize(Acces.FACTURES)
  @Operation(
      summary = "Lister les factures",
      description =
          "enRetard=true : non payées dont l'échéance est dépassée. Tri autorisé : dateEmission,"
              + " dateEcheance, numero, statut (défaut dateEmission,desc).")
  @ApiResponse(responseCode = "200", description = "Page de factures")
  public PageResponse<FactureDto> lister(
      @Parameter(example = "RECUE") @RequestParam(required = false) StatutFacture statut,
      @Parameter(example = "f0000000-0000-0000-0000-000000000001") @RequestParam(required = false)
          UUID fournisseurId,
      @Parameter(example = "true") @RequestParam(required = false) Boolean enRetard,
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(example = "dateEcheance,asc") @RequestParam(required = false) String sort) {
    return consultation
        .factures(
            new FactureRepository.Filtre(statut, fournisseurId, null, enRetard),
            pages.query(page, size, sort, FactureRepository.TRIABLES, "dateEmission,desc"))
        .map(mapper::facture);
  }

  /**
   * @param requete facture
   * @return la facture RECUE
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Acces.FACTURES)
  @Operation(
      summary = "Enregistrer une facture reçue",
      description =
          "Contrat à engagement validé requis (422 CONTRAT_NON_FACTURABLE) ; le cumul TTC ne peut"
              + " dépasser le montant du contrat (422 DEPASSEMENT_MONTANT_CONTRAT) ; numéro unique"
              + " par fournisseur (409 FACTURE_EXISTANTE).")
  @ApiResponse(responseCode = "201", description = "Facture RECUE")
  public FactureDto recevoir(@Valid @RequestBody CreerFactureRequest requete) {
    return mapper.facture(
        factures.recevoir(
            new RecevoirFactureCommand(
                requete.contratId(),
                requete.numero(),
                requete.montantHt(),
                requete.tva(),
                requete.dateEmission(),
                requete.dateEcheance(),
                utilisateurs.require())));
  }

  /**
   * @param id facture
   * @param requete mandat
   * @return la facture MANDATEE
   */
  @PostMapping("/{id}/mandater")
  @PreAuthorize(Acces.DAF)
  @Operation(summary = "Mandater une facture", description = "RECUE → MANDATEE.")
  @ApiResponse(responseCode = "200", description = "Facture MANDATEE")
  public FactureDto mandater(@PathVariable UUID id, @Valid @RequestBody MandaterRequest requete) {
    return mapper.facture(factures.mandater(id, requete.referenceMandat()));
  }

  /**
   * @param id facture
   * @return la facture PAYEE
   */
  @PostMapping("/{id}/payer")
  @PreAuthorize(Acces.DAF)
  @Operation(summary = "Payer une facture", description = "MANDATEE → PAYEE.")
  @ApiResponse(responseCode = "200", description = "Facture PAYEE")
  public FactureDto payer(@PathVariable UUID id) {
    return mapper.facture(factures.payer(id));
  }

  /**
   * @param id facture
   * @param requete motif
   * @return la facture REJETEE
   */
  @PostMapping("/{id}/rejeter")
  @PreAuthorize(Acces.DAF)
  @Operation(summary = "Rejeter une facture", description = "RECUE → REJETEE, motif obligatoire.")
  @ApiResponse(responseCode = "200", description = "Facture REJETEE")
  public FactureDto rejeter(@PathVariable UUID id, @Valid @RequestBody RejeterRequest requete) {
    return mapper.facture(factures.rejeter(id, requete.motif()));
  }
}
