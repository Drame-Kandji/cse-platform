package sn.cse.reporting.domain.model.vue;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import sn.cse.reporting.domain.model.GraviteLitige;
import sn.cse.reporting.domain.model.StatutLitige;

/**
 * Vue d'un litige fournisseur.
 *
 * @param id identifiant
 * @param reference référence
 * @param fournisseurId fournisseur
 * @param contratId contrat concerné
 * @param gravite gravité
 * @param statut statut courant
 * @param ouvertLe instant d'ouverture (premier événement reçu)
 * @param majLe instant du dernier événement appliqué
 */
public record LitigeVue(
    UUID id,
    String reference,
    UUID fournisseurId,
    UUID contratId,
    GraviteLitige gravite,
    StatutLitige statut,
    Instant ouvertLe,
    Instant majLe) {

  public LitigeVue {
    Objects.requireNonNull(id, "id");
  }

  /**
   * @param recu vue reçue
   * @return l'instantané le plus récent, l'instant d'ouverture étant le premier connu
   */
  public LitigeVue fusionnerAvec(LitigeVue recu) {
    LitigeVue r = Horodatage.plusRecent(recu.majLe, majLe) ? recu : this;
    return new LitigeVue(
        id,
        r.reference,
        r.fournisseurId,
        r.contratId,
        r.gravite,
        r.statut,
        Horodatage.min(ouvertLe, recu.ouvertLe),
        Horodatage.max(majLe, recu.majLe));
  }

  /**
   * @return vrai si le litige n'est ni résolu ni classé
   */
  public boolean ouvert() {
    return statut == null || statut == StatutLitige.OUVERT || statut == StatutLitige.EN_COURS;
  }
}
