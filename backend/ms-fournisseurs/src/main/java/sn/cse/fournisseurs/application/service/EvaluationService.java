package sn.cse.fournisseurs.application.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.security.CurrentUser;
import sn.cse.fournisseurs.application.command.EvaluationCommand;
import sn.cse.fournisseurs.application.port.in.EvaluerFournisseurUseCase;
import sn.cse.fournisseurs.application.port.out.EvaluationRepository;
import sn.cse.fournisseurs.domain.model.Evaluation;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.NotesEvaluation;
import sn.cse.fournisseurs.domain.service.ScorePolicy;

/** Évaluation des fournisseurs et recalcul automatique du score (R07). */
@Service
@Transactional
public class EvaluationService implements EvaluerFournisseurUseCase {

  private final EvaluationRepository evaluations;
  private final ChargeurFournisseur chargeur;
  private final ScorePolicy politique;
  private final RecalculScore recalcul;
  private final Clock clock;

  /**
   * @param evaluations dépôt des évaluations
   * @param chargeur chargement des fournisseurs
   * @param politique politique de score (note globale)
   * @param recalcul recalcul du score
   * @param clock horloge
   */
  public EvaluationService(
      EvaluationRepository evaluations,
      ChargeurFournisseur chargeur,
      ScorePolicy politique,
      RecalculScore recalcul,
      Clock clock) {
    this.evaluations = evaluations;
    this.chargeur = chargeur;
    this.politique = politique;
    this.recalcul = recalcul;
    this.clock = clock;
  }

  @Override
  public Evaluation evaluer(
      UUID fournisseurId, EvaluationCommand commande, CurrentUser utilisateur) {
    Fournisseur fournisseur = chargeur.charger(fournisseurId);
    NotesEvaluation notes = commande.notes();
    Evaluation evaluation =
        evaluations.enregistrer(
            Evaluation.creer(
                fournisseur,
                commande.contratId(),
                notes,
                politique.noteGlobale(notes),
                commande.commentaire(),
                Auteurs.depuis(utilisateur),
                clock.instant()));
    // Règle R07 : toute nouvelle évaluation déclenche le recalcul du score.
    recalcul.recalculer(fournisseur, "Nouvelle évaluation (note " + evaluation.noteGlobale() + ")");
    return evaluation;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Evaluation> evaluations(UUID fournisseurId) {
    chargeur.charger(fournisseurId);
    return evaluations.parFournisseur(fournisseurId);
  }
}
