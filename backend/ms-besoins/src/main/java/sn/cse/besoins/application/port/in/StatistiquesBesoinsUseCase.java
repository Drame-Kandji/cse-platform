package sn.cse.besoins.application.port.in;

import sn.cse.besoins.application.query.StatistiquesBesoins;
import sn.cse.common.security.CurrentUser;

/** Indicateurs des besoins (filtrés sur ses chantiers pour un chef de chantier). */
public interface StatistiquesBesoinsUseCase {

  /**
   * @param utilisateur lecteur
   * @return les statistiques visibles
   */
  StatistiquesBesoins statistiques(CurrentUser utilisateur);
}
