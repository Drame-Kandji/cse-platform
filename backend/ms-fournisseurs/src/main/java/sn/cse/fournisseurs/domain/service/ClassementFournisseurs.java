package sn.cse.fournisseurs.domain.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;

/**
 * Classement dynamique des fournisseurs : seuls les fournisseurs {@code ACTIF} (et couvrant la
 * catégorie demandée) sont classés, par score décroissant, puis taux de ponctualité décroissant,
 * nombre d'évaluations décroissant et raison sociale.
 */
public final class ClassementFournisseurs {

  private ClassementFournisseurs() {}

  /**
   * Fournisseur candidat au classement.
   *
   * @param fournisseur fournisseur
   * @param nombreLitiges nombre total de litiges
   */
  public record Candidat(Fournisseur fournisseur, int nombreLitiges) {}

  /**
   * Position dans le classement.
   *
   * @param rang rang (1 = meilleur)
   * @param fournisseur fournisseur
   * @param nombreLitiges nombre total de litiges
   */
  public record Rang(int rang, Fournisseur fournisseur, int nombreLitiges) {}

  /**
   * Établit le classement.
   *
   * @param candidats fournisseurs candidats
   * @param categorie catégorie filtrée ({@code null} = toutes)
   * @return classement ordonné
   */
  public static List<Rang> classer(List<Candidat> candidats, CategorieMateriel categorie) {
    AtomicInteger rang = new AtomicInteger();
    return candidats.stream()
        .filter(c -> c.fournisseur().getStatut() == StatutFournisseur.ACTIF)
        .filter(c -> categorie == null || c.fournisseur().getProfil().couvre(categorie))
        .sorted(ordre())
        .map(c -> new Rang(rang.incrementAndGet(), c.fournisseur(), c.nombreLitiges()))
        .toList();
  }

  private static Comparator<Candidat> ordre() {
    Comparator<Candidat> parScore =
        Comparator.comparing(
            (Candidat c) -> c.fournisseur().getPerformance().scorePerformance(),
            Comparator.reverseOrder());
    Comparator<Candidat> parPonctualite =
        Comparator.comparing(
            (Candidat c) ->
                c.fournisseur()
                    .getPerformance()
                    .tauxLivraisonALHeure()
                    .orElse(BigDecimal.valueOf(-1)),
            Comparator.reverseOrder());
    return parScore
        .thenComparing(parPonctualite)
        .thenComparing(
            c -> c.fournisseur().getPerformance().nombreEvaluations(), Comparator.reverseOrder())
        .thenComparing(c -> c.fournisseur().getProfil().raisonSociale());
  }
}
