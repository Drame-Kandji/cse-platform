package sn.cse.fournisseurs.application.port.out;

import java.util.List;
import java.util.UUID;
import sn.cse.fournisseurs.domain.model.Evaluation;

/** Port de persistance des évaluations. */
public interface EvaluationRepository {

  /**
   * @param evaluation évaluation à enregistrer
   * @return évaluation enregistrée
   */
  Evaluation enregistrer(Evaluation evaluation);

  /**
   * @param fournisseurId fournisseur
   * @return évaluations, de la plus récente à la plus ancienne
   */
  List<Evaluation> parFournisseur(UUID fournisseurId);
}
