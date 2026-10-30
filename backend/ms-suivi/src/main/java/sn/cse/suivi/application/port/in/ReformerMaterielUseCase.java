package sn.cse.suivi.application.port.in;

import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.query.MaterielVue;

/** Réforme d'un matériel en deux étapes (R13) : demande RM, décision DG. */
public interface ReformerMaterielUseCase {

  /**
   * @param materielId matériel
   * @param motif justification
   * @param auteur Responsable Matériel
   * @return le matériel ({@code reformeDemandee = true})
   */
  MaterielVue demander(UUID materielId, String motif, CurrentUser auteur);

  /**
   * @param materielId matériel
   * @param approuve décision
   * @param commentaire commentaire
   * @param auteur Direction Générale
   * @return le matériel ({@code REFORME} si approuvé)
   */
  MaterielVue decider(UUID materielId, boolean approuve, String commentaire, CurrentUser auteur);
}
