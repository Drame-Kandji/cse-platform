package sn.cse.reporting.domain.model.vue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import sn.cse.reporting.domain.model.StatutFournisseur;

/**
 * Vue d'un fournisseur (statut et score de performance R07, 0..100).
 *
 * @param id identifiant
 * @param raisonSociale raison sociale
 * @param statut statut (inconnu pour une référence)
 * @param scorePerformance score 0..100
 * @param complet vrai si la vue provient d'un événement du fournisseur lui-même
 * @param majLe instant du dernier événement appliqué
 */
public record FournisseurVue(
    UUID id,
    String raisonSociale,
    StatutFournisseur statut,
    BigDecimal scorePerformance,
    boolean complet,
    Instant majLe) {

  public FournisseurVue {
    Objects.requireNonNull(id, "id");
    scorePerformance = scorePerformance == null ? BigDecimal.ZERO : scorePerformance;
  }

  /**
   * @param id identifiant
   * @param raisonSociale raison sociale connue par un litige ou un contrat
   * @param quand instant de cet événement
   * @return une référence incomplète
   */
  public static FournisseurVue reference(UUID id, String raisonSociale, Instant quand) {
    return new FournisseurVue(id, raisonSociale, null, null, false, quand);
  }

  /**
   * @param recu vue reçue
   * @return la vue fusionnée (même règle que {@link ChantierVue#fusionnerAvec})
   */
  public FournisseurVue fusionnerAvec(FournisseurVue recu) {
    if (!recu.complet) {
      return raisonSociale != null || recu.raisonSociale == null
          ? this
          : new FournisseurVue(id, recu.raisonSociale, statut, scorePerformance, complet, majLe);
    }
    return !complet || Horodatage.plusRecent(recu.majLe, majLe) ? recu : this;
  }

  /**
   * @return vrai si le fournisseur est actif
   */
  public boolean actif() {
    return statut == StatutFournisseur.ACTIF;
  }
}
