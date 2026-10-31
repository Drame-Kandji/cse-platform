package sn.cse.achats.application.port.in;

import java.util.UUID;
import sn.cse.achats.domain.model.ContratAchat;

/** Génération du contrat BROUILLON d'un marché attribué (R03). */
public interface GenererContratUseCase {

  /**
   * @param appelOffreId appel d'offres attribué
   * @return le contrat généré
   */
  ContratAchat generer(UUID appelOffreId);
}
