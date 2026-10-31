package sn.cse.achats.application.port.in;

import java.util.UUID;
import sn.cse.achats.application.command.EvaluerOffreCommand;
import sn.cse.achats.domain.service.Comparatif;

/** Évaluation multicritère des offres. */
public interface EvaluerOffresUseCase {

  /**
   * @param commande notes saisies
   * @return l'offre réévaluée
   */
  OffreVue evaluer(EvaluerOffreCommand commande);

  /**
   * @param appelOffreId appel d'offres
   * @return le comparatif (scores, rangs, recommandation)
   */
  Comparatif comparatif(UUID appelOffreId);
}
