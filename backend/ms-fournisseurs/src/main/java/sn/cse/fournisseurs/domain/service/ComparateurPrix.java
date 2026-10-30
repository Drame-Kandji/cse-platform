package sn.cse.fournisseurs.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.LigneCatalogue;

/**
 * Compare les prix catalogue de plusieurs fournisseurs : les lignes sont regroupées par catégorie
 * et désignation normalisée (casse, accents et espaces ignorés).
 */
public final class ComparateurPrix {

  private ComparateurPrix() {}

  /**
   * Ligne de catalogue accompagnée des informations du fournisseur.
   *
   * @param ligne ligne de catalogue
   * @param raisonSociale raison sociale du fournisseur
   * @param scorePerformance score du fournisseur
   */
  public record OffreCatalogue(
      LigneCatalogue ligne, String raisonSociale, BigDecimal scorePerformance) {}

  /**
   * Offre d'un fournisseur dans une comparaison.
   *
   * @param fournisseurId fournisseur
   * @param raisonSociale raison sociale
   * @param prixUnitaire prix unitaire
   * @param delaiLivraisonJours délai annoncé
   * @param scorePerformance score du fournisseur
   */
  public record OffrePrix(
      UUID fournisseurId,
      String raisonSociale,
      BigDecimal prixUnitaire,
      int delaiLivraisonJours,
      BigDecimal scorePerformance) {}

  /**
   * Résultat de comparaison pour un article.
   *
   * @param designation désignation (celle de l'offre la moins chère)
   * @param reference référence commune à toutes les offres, sinon {@code null}
   * @param categorie catégorie
   * @param prixMin prix minimal
   * @param prixMax prix maximal
   * @param prixMoyen prix moyen (arrondi au FCFA)
   * @param offres offres triées par prix croissant
   */
  public record ComparaisonPrix(
      String designation,
      String reference,
      CategorieMateriel categorie,
      BigDecimal prixMin,
      BigDecimal prixMax,
      BigDecimal prixMoyen,
      List<OffrePrix> offres) {}

  /**
   * Regroupe et compare les offres.
   *
   * @param offres lignes de catalogue des fournisseurs à comparer
   * @return comparaisons triées par catégorie puis désignation
   */
  public static List<ComparaisonPrix> comparer(List<OffreCatalogue> offres) {
    Map<String, List<OffreCatalogue>> groupes = new LinkedHashMap<>();
    for (OffreCatalogue offre : offres) {
      String cle = offre.ligne().getCategorie() + "|" + normaliser(offre.ligne().getDesignation());
      groupes.computeIfAbsent(cle, k -> new java.util.ArrayList<>()).add(offre);
    }
    return groupes.values().stream()
        .map(ComparateurPrix::comparerGroupe)
        .sorted(
            Comparator.comparing(ComparaisonPrix::categorie)
                .thenComparing(c -> normaliser(c.designation())))
        .toList();
  }

  private static ComparaisonPrix comparerGroupe(List<OffreCatalogue> groupe) {
    List<OffreCatalogue> tries =
        groupe.stream()
            .sorted(
                Comparator.comparing((OffreCatalogue o) -> o.ligne().getPrixUnitaire())
                    .thenComparing(OffreCatalogue::raisonSociale))
            .toList();
    LigneCatalogue moinsChere = tries.get(0).ligne();
    BigDecimal min = moinsChere.getPrixUnitaire();
    BigDecimal max = tries.get(tries.size() - 1).ligne().getPrixUnitaire();
    BigDecimal moyen =
        tries.stream()
            .map(o -> o.ligne().getPrixUnitaire())
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(tries.size()), 0, RoundingMode.HALF_UP);
    boolean referenceCommune =
        tries.stream().allMatch(o -> o.ligne().getReference().equals(moinsChere.getReference()));
    return new ComparaisonPrix(
        moinsChere.getDesignation(),
        referenceCommune ? moinsChere.getReference() : null,
        moinsChere.getCategorie(),
        min,
        max,
        moyen,
        tries.stream()
            .map(
                o ->
                    new OffrePrix(
                        o.ligne().getFournisseurId(),
                        o.raisonSociale(),
                        o.ligne().getPrixUnitaire(),
                        o.ligne().getDonnees().delaiLivraisonJours(),
                        o.scorePerformance()))
            .toList());
  }

  static String normaliser(String texte) {
    String sansAccents = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return sansAccents.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
  }
}
