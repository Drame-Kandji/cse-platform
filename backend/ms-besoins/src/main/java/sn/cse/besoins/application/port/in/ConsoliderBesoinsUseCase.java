package sn.cse.besoins.application.port.in;

import java.util.List;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.besoins.domain.service.LigneConsolidation;

/** Consolidation des besoins pour le Responsable Matériel. */
public interface ConsoliderBesoinsUseCase {

  /**
   * @param statut {@code EN_ATTENTE} ou {@code VALIDE} ; {@code null} pour les deux
   * @return les regroupements
   */
  List<LigneConsolidation> consolider(StatutBesoin statut);
}
