package sn.cse.suivi.application.port.in;

import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.query.CarteVue;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.EtatMateriel;

/** Carte du parc : chantiers et matériels géolocalisés. */
public interface ConsulterCarteUseCase {

  /**
   * @param categorie catégorie (optionnelle)
   * @param etat état (optionnel)
   * @param lecteur utilisateur
   * @return chantiers et matériels visibles
   */
  CarteVue carte(CategorieMateriel categorie, EtatMateriel etat, CurrentUser lecteur);
}
