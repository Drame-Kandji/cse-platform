package sn.cse.fournisseurs.application.command;

import java.math.BigDecimal;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.DonneesLigneCatalogue;

/**
 * Publication (upsert) d'une ligne de catalogue.
 *
 * @param reference référence article
 * @param designation désignation
 * @param categorie catégorie
 * @param prixUnitaire prix unitaire FCFA
 * @param unite unité
 * @param delaiLivraisonJours délai de livraison
 */
public record LigneCatalogueCommand(
    String reference,
    String designation,
    CategorieMateriel categorie,
    BigDecimal prixUnitaire,
    String unite,
    int delaiLivraisonJours) {

  /**
   * @return données de domaine validées
   */
  public DonneesLigneCatalogue donnees() {
    return new DonneesLigneCatalogue(
        reference, designation, categorie, prixUnitaire, unite, delaiLivraisonJours);
  }
}
