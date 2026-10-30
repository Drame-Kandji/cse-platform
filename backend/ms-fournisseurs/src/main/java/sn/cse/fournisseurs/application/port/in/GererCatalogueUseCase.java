package sn.cse.fournisseurs.application.port.in;

import java.util.List;
import java.util.UUID;
import sn.cse.fournisseurs.application.command.LigneCatalogueCommand;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.LigneCatalogue;
import sn.cse.fournisseurs.domain.model.PrixHistorique;
import sn.cse.fournisseurs.domain.service.ComparateurPrix;

/** Catalogue de prix des fournisseurs. */
public interface GererCatalogueUseCase {

  /**
   * @param fournisseurId fournisseur
   * @return lignes du catalogue
   */
  List<LigneCatalogue> catalogue(UUID fournisseurId);

  /**
   * Upsert par référence ; tout changement de prix est historisé.
   *
   * @param fournisseurId fournisseur
   * @param commande ligne
   * @return ligne publiée
   */
  LigneCatalogue publierLigne(UUID fournisseurId, LigneCatalogueCommand commande);

  /**
   * @param fournisseurId fournisseur
   * @param reference référence (optionnelle)
   * @return historique des prix
   */
  List<PrixHistorique> historique(UUID fournisseurId, String reference);

  /**
   * Compare les prix des fournisseurs actifs.
   *
   * @param categorie catégorie (optionnelle)
   * @param q texte recherché (optionnel)
   * @return comparaisons
   */
  List<ComparateurPrix.ComparaisonPrix> comparer(CategorieMateriel categorie, String q);
}
