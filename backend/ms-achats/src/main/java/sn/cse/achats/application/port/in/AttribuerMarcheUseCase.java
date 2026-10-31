package sn.cse.achats.application.port.in;

import sn.cse.achats.application.command.DecisionCommand;
import sn.cse.achats.application.command.ProposerAttributionCommand;
import sn.cse.achats.domain.model.AppelOffre;

/** Circuit d'attribution d'un marché (R11). */
public interface AttribuerMarcheUseCase {

  /**
   * @param commande proposition du Responsable des Achats
   * @return l'appel d'offres et sa proposition
   */
  AppelOffre proposer(ProposerAttributionCommand commande);

  /**
   * @param commande co-validation du Responsable Matériel
   * @return l'appel d'offres après décision
   */
  AppelOffre coValider(DecisionCommand commande);

  /**
   * @param commande approbation de la Direction Générale
   * @return l'appel d'offres après décision
   */
  AppelOffre approuver(DecisionCommand commande);
}
