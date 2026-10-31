package sn.cse.achats.application.port.in;

import sn.cse.achats.application.command.CreerAppelOffreCommand;
import sn.cse.achats.domain.model.AppelOffre;

/** Publication d'un appel d'offres (invités vérifiés auprès de ms-fournisseurs, R14). */
public interface PublierAppelOffreUseCase {

  /**
   * @param commande données de l'appel d'offres
   * @return l'appel d'offres publié
   */
  AppelOffre publier(CreerAppelOffreCommand commande);
}
