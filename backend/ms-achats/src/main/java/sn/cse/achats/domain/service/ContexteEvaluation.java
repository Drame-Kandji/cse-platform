package sn.cse.achats.domain.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import sn.cse.achats.domain.model.Offre;

/**
 * Meilleures valeurs parmi les offres comparées, référence des notations relatives.
 *
 * @param meilleurPrix plus petit montant proposé
 * @param meilleurDelai plus court délai de livraison (jours)
 */
public record ContexteEvaluation(BigDecimal meilleurPrix, int meilleurDelai) {

  /**
   * @param offres offres comparées (non vide)
   * @return le contexte correspondant
   */
  public static ContexteEvaluation de(List<Offre> offres) {
    BigDecimal prix =
        offres.stream()
            .map(Offre::getMontantPropose)
            .min(Comparator.naturalOrder())
            .orElse(BigDecimal.ONE);
    int delai = offres.stream().mapToInt(Offre::getDelaiLivraisonJours).min().orElse(1);
    return new ContexteEvaluation(prix, delai);
  }
}
