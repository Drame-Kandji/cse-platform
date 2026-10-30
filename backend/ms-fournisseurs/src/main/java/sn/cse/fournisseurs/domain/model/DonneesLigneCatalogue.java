package sn.cse.fournisseurs.domain.model;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.fournisseurs.domain.exception.CodesErreur;

/**
 * Données saisies pour une ligne de catalogue.
 *
 * @param reference référence article du fournisseur (clé d'upsert)
 * @param designation désignation
 * @param categorie catégorie de matériel
 * @param prixUnitaire prix unitaire en FCFA (strictement positif, sans décimales)
 * @param unite unité de vente (unité, tonne, jour…)
 * @param delaiLivraisonJours délai de livraison annoncé
 */
public record DonneesLigneCatalogue(
    String reference,
    String designation,
    CategorieMateriel categorie,
    BigDecimal prixUnitaire,
    String unite,
    int delaiLivraisonJours) {

  /** Valide et normalise les données. */
  public DonneesLigneCatalogue {
    reference = obligatoire(reference, "référence").toUpperCase(Locale.ROOT);
    designation = obligatoire(designation, "désignation");
    unite = obligatoire(unite, "unité");
    Objects.requireNonNull(categorie, "categorie");
    if (prixUnitaire == null || prixUnitaire.signum() <= 0) {
      throw new BusinessRuleException(
          CodesErreur.DONNEE_INVALIDE, "Le prix unitaire doit être strictement positif.");
    }
    prixUnitaire = prixUnitaire.setScale(0, java.math.RoundingMode.HALF_UP);
    if (delaiLivraisonJours < 0) {
      throw new BusinessRuleException(
          CodesErreur.DONNEE_INVALIDE, "Le délai de livraison ne peut pas être négatif.");
    }
  }

  private static String obligatoire(String valeur, String champ) {
    if (valeur == null || valeur.isBlank()) {
      throw new BusinessRuleException(
          CodesErreur.DONNEE_INVALIDE, "Le champ « " + champ + " » est obligatoire.");
    }
    return valeur.trim();
  }
}
