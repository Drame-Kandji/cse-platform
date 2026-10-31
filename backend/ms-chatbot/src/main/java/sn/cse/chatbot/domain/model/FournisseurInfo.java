package sn.cse.chatbot.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fournisseur classé (lecture ms-fournisseurs).
 *
 * @param id identifiant
 * @param rang rang dans le classement
 * @param raisonSociale raison sociale
 * @param scorePerformance score 0..100
 * @param tauxLivraisonALHeure taux 0..100 ou {@code null}
 * @param nombreLitiges litiges
 * @param specialites catégories
 */
public record FournisseurInfo(
    String id,
    int rang,
    String raisonSociale,
    BigDecimal scorePerformance,
    BigDecimal tauxLivraisonALHeure,
    int nombreLitiges,
    List<String> specialites) {

  public FournisseurInfo {
    specialites = specialites == null ? List.of() : List.copyOf(specialites);
  }
}
