package sn.cse.achats.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.achats.api.dto.ContratDto;
import sn.cse.achats.api.dto.DecisionRequest;
import sn.cse.achats.api.dto.EngagementDto;
import sn.cse.achats.api.mapper.AchatsApiMapper;
import sn.cse.achats.application.command.DecisionCommand;
import sn.cse.achats.application.port.in.ConsulterContratsUseCase;
import sn.cse.achats.application.port.in.ValiderEngagementUseCase;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.common.web.PageableFactory;

/** Engagements financiers à valider par la DAF (R06). */
@RestController
@RequestMapping("/api/engagements")
@Tag(name = "Engagements", description = "Validation des engagements par la DAF (R06)")
public class EngagementController {

  private final ConsulterContratsUseCase consultation;
  private final ValiderEngagementUseCase validation;
  private final PageableFactory pages;
  private final CurrentUserProvider utilisateurs;
  private final AchatsApiMapper mapper;

  /**
   * @param consultation lectures
   * @param validation décision DAF
   * @param pages pagination
   * @param utilisateurs utilisateur courant
   * @param mapper conversion DTO
   */
  public EngagementController(
      ConsulterContratsUseCase consultation,
      ValiderEngagementUseCase validation,
      PageableFactory pages,
      CurrentUserProvider utilisateurs,
      AchatsApiMapper mapper) {
    this.consultation = consultation;
    this.validation = validation;
    this.pages = pages;
    this.utilisateurs = utilisateurs;
    this.mapper = mapper;
  }

  /**
   * @param statut statut d'engagement
   * @param page page
   * @param size taille
   * @param sort tri
   * @return la page
   */
  @GetMapping
  @PreAuthorize(Acces.LECTURE_ENGAGEMENTS)
  @Operation(
      summary = "Lister les engagements",
      description = "Tri autorisé : dateDemande, dateDecision (défaut dateDemande,desc).")
  @ApiResponse(responseCode = "200", description = "Page d'engagements")
  public PageResponse<EngagementDto> lister(
      @Parameter(example = "EN_ATTENTE") @RequestParam(required = false) StatutApprobation statut,
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size,
      @Parameter(example = "dateDemande,desc") @RequestParam(required = false) String sort) {
    return consultation
        .engagements(
            statut,
            pages.query(
                page, size, sort, ContratRepository.TRIABLES_ENGAGEMENTS, "dateDemande,desc"))
        .map(mapper::engagement);
  }

  /**
   * @param contratId contrat
   * @param requete décision
   * @return le contrat
   */
  @PostMapping("/{contratId}/valider")
  @PreAuthorize(Acces.DAF)
  @Operation(
      summary = "Décider d'un engagement",
      description =
          "Approbation : contrat ENGAGEMENT_VALIDE (la livraison peut démarrer, R06). Refus :"
              + " contrat renvoyé en BROUILLON pour renégociation.")
  @ApiResponse(responseCode = "200", description = "Contrat après décision")
  public ContratDto valider(
      @PathVariable UUID contratId, @Valid @RequestBody DecisionRequest requete) {
    return mapper.contrat(
        validation.decider(
            new DecisionCommand(
                contratId, requete.approuve(), requete.commentaire(), utilisateurs.require())));
  }
}
