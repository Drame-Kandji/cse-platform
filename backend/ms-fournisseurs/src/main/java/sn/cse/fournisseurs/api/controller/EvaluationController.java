package sn.cse.fournisseurs.api.controller;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.common.security.CurrentUserProvider;
import sn.cse.fournisseurs.api.dto.EvaluationDto;
import sn.cse.fournisseurs.api.dto.EvaluationRequest;
import sn.cse.fournisseurs.api.mapper.DtoMapper;
import sn.cse.fournisseurs.application.command.EvaluationCommand;
import sn.cse.fournisseurs.application.port.in.EvaluerFournisseurUseCase;

/** Évaluations des fournisseurs et recalcul du score (R07). */
@RestController
@RequestMapping("/api/fournisseurs/{id}")
@Tag(
    name = "Évaluations",
    description = "Évaluation des fournisseurs et score de performance (R07)")
public class EvaluationController {

  private final EvaluerFournisseurUseCase evaluations;
  private final CurrentUserProvider utilisateurs;

  /**
   * @param evaluations cas d'utilisation
   * @param utilisateurs utilisateur courant
   */
  public EvaluationController(
      EvaluerFournisseurUseCase evaluations, CurrentUserProvider utilisateurs) {
    this.evaluations = evaluations;
    this.utilisateurs = utilisateurs;
  }

  /**
   * @param id fournisseur
   * @param requete notes
   * @return évaluation
   */
  @PostMapping("/evaluer")
  @PreAuthorize(Acces.RA_RM)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Évaluer un fournisseur",
      description =
          "Notes 0..10 sur cinq critères ; la note globale (0..100) est pondérée selon"
              + " cse.fournisseurs.score.* et le score de performance est recalculé (R07).")
  @ApiResponse(responseCode = "201", description = "Évaluation enregistrée")
  public EvaluationDto evaluer(
      @PathVariable UUID id, @Valid @RequestBody EvaluationRequest requete) {
    EvaluationCommand commande =
        new EvaluationCommand(
            requete.contratId(),
            requete.qualite(),
            requete.delais(),
            requete.conformite(),
            requete.sav(),
            requete.prix(),
            requete.commentaire());
    return DtoMapper.evaluation(evaluations.evaluer(id, commande, utilisateurs.require()));
  }

  /**
   * @param id fournisseur
   * @return évaluations
   */
  @GetMapping("/evaluations")
  @PreAuthorize(Acces.LECTEURS)
  @Operation(
      summary = "Lister les évaluations",
      description = "De la plus récente à la plus ancienne.")
  @ApiResponse(responseCode = "200", description = "Évaluations")
  public List<EvaluationDto> evaluations(@PathVariable UUID id) {
    return evaluations.evaluations(id).stream().map(DtoMapper::evaluation).toList();
  }
}
