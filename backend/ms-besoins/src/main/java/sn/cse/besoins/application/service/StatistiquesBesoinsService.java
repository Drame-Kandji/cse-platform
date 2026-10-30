package sn.cse.besoins.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.besoins.application.port.in.StatistiquesBesoinsUseCase;
import sn.cse.besoins.application.port.out.BesoinQueryPort;
import sn.cse.besoins.application.query.StatistiquesBesoins;
import sn.cse.common.security.CurrentUser;

/** Statistiques des besoins, restreintes à ses chantiers pour un chef de chantier. */
@Service
@Transactional(readOnly = true)
public class StatistiquesBesoinsService implements StatistiquesBesoinsUseCase {

  private final BesoinQueryPort requetes;

  StatistiquesBesoinsService(BesoinQueryPort requetes) {
    this.requetes = requetes;
  }

  @Override
  public StatistiquesBesoins statistiques(CurrentUser utilisateur) {
    return requetes.statistiques(
        utilisateur.estRestreintAuxChantiers() ? utilisateur.chantierIds() : null);
  }
}
