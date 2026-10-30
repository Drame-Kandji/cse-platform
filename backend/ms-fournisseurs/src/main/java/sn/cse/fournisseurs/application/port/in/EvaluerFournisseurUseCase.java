package sn.cse.fournisseurs.application.port.in;

import java.util.List;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.fournisseurs.application.command.EvaluationCommand;
import sn.cse.fournisseurs.domain.model.Evaluation;

/** Évaluation des fournisseurs (R07). */
public interface EvaluerFournisseurUseCase {

  /**
   * Enregistre une évaluation puis recalcule le score (R07).
   *
   * @param fournisseurId fournisseur
   * @param commande notes
   * @param utilisateur évaluateur
   * @return évaluation créée
   */
  Evaluation evaluer(UUID fournisseurId, EvaluationCommand commande, CurrentUser utilisateur);

  /**
   * @param fournisseurId fournisseur
   * @return évaluations, de la plus récente à la plus ancienne
   */
  List<Evaluation> evaluations(UUID fournisseurId);
}
