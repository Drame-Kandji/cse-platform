package sn.cse.suivi.domain.service;

import java.util.Collection;
import java.util.UUID;
import sn.cse.suivi.domain.exception.SuiviErreurs;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Periode;

/**
 * Politique d'affectation d'un matériel à un chantier. Ordre des contrôles :
 *
 * <ol>
 *   <li>matériel immobilisé, réformé ou en instance de réforme → 422 (R04) ;
 *   <li>chantier terminé → 422 ;
 *   <li>affectation active dont la période chevauche celle demandée → 409 {@code
 *       CONFLIT_AFFECTATION} ;
 *   <li>matériel non {@code DISPONIBLE} → 422 (R04).
 * </ol>
 */
public final class PolitiqueAffectation {

  private PolitiqueAffectation() {}

  /**
   * Vérifie l'éligibilité puis crée l'affectation et fait passer le matériel à {@code AFFECTE}.
   *
   * @param materiel matériel à affecter
   * @param chantier chantier de destination
   * @param besoinId besoin d'origine (optionnel)
   * @param periode période demandée
   * @param affectationsDuMateriel affectations existantes du matériel
   * @return la nouvelle affectation
   */
  public static Affectation affecter(
      Materiel materiel,
      Chantier chantier,
      UUID besoinId,
      Periode periode,
      Collection<Affectation> affectationsDuMateriel) {
    materiel.verifierAffectable();
    chantier.verifierOuvertAuxAffectations();
    if (affectationsDuMateriel.stream().anyMatch(a -> a.bloque(periode))) {
      throw SuiviErreurs.conflitAffectation(materiel.getCode());
    }
    // Règle R04 : le matériel doit être DISPONIBLE (contrôlé par l'agrégat).
    materiel.affecter(chantier);
    return Affectation.creer(materiel, chantier, besoinId, periode);
  }
}
