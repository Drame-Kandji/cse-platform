package sn.cse.besoins.domain.validation;

import java.util.Objects;
import sn.cse.besoins.domain.model.BesoinMateriel;

/**
 * Étape configurable de la chaîne de validation des besoins (ADR-0010).
 *
 * @param code code stable de l'étape (tracé dans l'historique)
 * @param roleRequis rôle Keycloak habilité à franchir l'étape
 * @param critere condition de déclenchement
 */
public record EtapeValidation(String code, String roleRequis, CritereDeclenchement critere) {

  public EtapeValidation {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("Le code de l'étape de validation est obligatoire");
    }
    if (roleRequis == null || roleRequis.isBlank()) {
      throw new IllegalArgumentException("Le rôle de l'étape " + code + " est obligatoire");
    }
    critere = Objects.requireNonNullElse(critere, CritereDeclenchement.toujours());
  }

  /**
   * @param besoin besoin évalué
   * @return vrai si l'étape s'applique à ce besoin
   */
  public boolean estRequisePour(BesoinMateriel besoin) {
    return critere.estSatisfaitPar(besoin);
  }
}
