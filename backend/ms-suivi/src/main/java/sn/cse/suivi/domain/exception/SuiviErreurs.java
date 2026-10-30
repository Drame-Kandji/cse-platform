package sn.cse.suivi.domain.exception;

import java.util.UUID;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.ForbiddenOperationException;

/**
 * Fabrique des erreurs métier du suivi du parc : codes stables exposés au client et messages en
 * français. Centraliser les codes évite les divergences entre le domaine et la documentation.
 */
public final class SuiviErreurs {

  /** Matériel non affectable (règle R04). */
  public static final String MATERIEL_NON_DISPONIBLE = "MATERIEL_NON_DISPONIBLE";

  /** Chevauchement de périodes d'affectation. */
  public static final String CONFLIT_AFFECTATION = "CONFLIT_AFFECTATION";

  /** Version obsolète fournie par le client. */
  public static final String CONFLIT_CONCURRENCE = "CONFLIT_CONCURRENCE";

  /** Transition d'état interdite par une machine à états. */
  public static final String TRANSITION_INVALIDE = "TRANSITION_INVALIDE";

  private SuiviErreurs() {}

  /**
   * @param code code du matériel
   * @param raison raison lisible
   * @return erreur 422 (R04)
   */
  public static BusinessRuleException materielNonDisponible(String code, String raison) {
    return new BusinessRuleException(
        MATERIEL_NON_DISPONIBLE,
        "Le matériel " + code + " ne peut pas être affecté : " + raison + " (règle R04).");
  }

  /**
   * @param code code du matériel
   * @return erreur 409 de chevauchement
   */
  public static ConflictException conflitAffectation(String code) {
    return new ConflictException(
        CONFLIT_AFFECTATION,
        "Le matériel " + code + " est déjà affecté sur une période qui chevauche celle demandée.");
  }

  /**
   * @param entite libellé de l'entité
   * @return erreur 409 de version obsolète
   */
  public static ConflictException versionObsolete(String entite) {
    return new ConflictException(
        CONFLIT_CONCURRENCE,
        entite + " a été modifié(e) par un autre utilisateur. Rechargez puis réessayez.");
  }

  /**
   * @param entite libellé de l'entité
   * @param depuis état courant
   * @param vers état demandé
   * @return erreur 409 de transition interdite
   */
  public static ConflictException transitionInvalide(String entite, Object depuis, Object vers) {
    return new ConflictException(
        TRANSITION_INVALIDE,
        "Transition interdite pour " + entite + " : " + depuis + " → " + vers + ".");
  }

  /**
   * @param code code stable
   * @param message message en français
   * @return erreur 422
   */
  public static BusinessRuleException regle(String code, String message) {
    return new BusinessRuleException(code, message);
  }

  /**
   * @param code code stable
   * @param message message en français
   * @return erreur 409
   */
  public static ConflictException conflit(String code, String message) {
    return new ConflictException(code, message);
  }

  /**
   * @param chantierId chantier non attribué à l'utilisateur
   * @return erreur 403
   */
  public static ForbiddenOperationException chantierNonAutorise(UUID chantierId) {
    return new ForbiddenOperationException(
        "CHANTIER_NON_AUTORISE",
        "Vous n'êtes pas affecté au chantier " + chantierId + " : opération refusée.");
  }

  /**
   * @param code code du matériel
   * @return erreur 422 : opération impossible sur un matériel réformé (R13)
   */
  public static BusinessRuleException materielReforme(String code) {
    return new BusinessRuleException(
        "MATERIEL_REFORME",
        "Le matériel " + code + " est réformé : aucune opération n'est plus possible (R13).");
  }
}
