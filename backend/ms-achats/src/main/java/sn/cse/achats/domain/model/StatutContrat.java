package sn.cse.achats.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.common.exception.ConflictException;

/**
 * Cycle de vie d'un contrat d'achat : BROUILLON → SIGNE → ENGAGEMENT_VALIDE → EN_LIVRAISON → LIVRE
 * → CLOTURE. Seule exception au sens unique : un engagement refusé par la DAF renvoie le contrat
 * signé en BROUILLON pour renégociation (sinon le contrat resterait bloqué).
 */
public enum StatutContrat {
  /** Contrat généré, clauses modifiables. */
  BROUILLON,
  /** Signé, engagement financier en attente de la DAF. */
  SIGNE,
  /** Engagement validé par la DAF (R06). */
  ENGAGEMENT_VALIDE,
  /** Livraison en cours. */
  EN_LIVRAISON,
  /** Livraison confirmée (bon de livraison). */
  LIVRE,
  /** Contrat soldé. */
  CLOTURE;

  private static final Map<StatutContrat, Set<StatutContrat>> TRANSITIONS =
      new EnumMap<>(StatutContrat.class);

  static {
    TRANSITIONS.put(BROUILLON, EnumSet.of(SIGNE));
    TRANSITIONS.put(SIGNE, EnumSet.of(ENGAGEMENT_VALIDE, BROUILLON));
    TRANSITIONS.put(ENGAGEMENT_VALIDE, EnumSet.of(EN_LIVRAISON));
    TRANSITIONS.put(EN_LIVRAISON, EnumSet.of(LIVRE));
    TRANSITIONS.put(LIVRE, EnumSet.of(CLOTURE));
    TRANSITIONS.put(CLOTURE, EnumSet.noneOf(StatutContrat.class));
  }

  /**
   * @param cible statut visé
   * @return vrai si la transition figure dans la table
   */
  public boolean peutPasserA(StatutContrat cible) {
    return TRANSITIONS.get(this).contains(cible);
  }

  /**
   * @param cible statut visé
   * @throws ConflictException si la transition est interdite
   */
  public void exigerTransition(StatutContrat cible) {
    if (!peutPasserA(cible)) {
      throw new ConflictException(
          CodesErreur.TRANSITION_INTERDITE,
          "Transition interdite du contrat : " + this + " → " + cible + ".");
    }
  }

  /**
   * @return vrai entre la signature et la livraison (jalons, retards, pénalités)
   */
  public boolean estEnExecution() {
    return this == SIGNE || this == ENGAGEMENT_VALIDE || this == EN_LIVRAISON;
  }

  /**
   * @return vrai si une facture peut être reçue (engagement validé, contrat non clôturé)
   */
  public boolean estFacturable() {
    return this == ENGAGEMENT_VALIDE || this == EN_LIVRAISON || this == LIVRE;
  }
}
