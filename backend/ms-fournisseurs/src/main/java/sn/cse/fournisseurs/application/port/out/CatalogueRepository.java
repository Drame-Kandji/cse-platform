package sn.cse.fournisseurs.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.LigneCatalogue;
import sn.cse.fournisseurs.domain.model.PrixHistorique;

/** Port de persistance du catalogue de prix et de son historique. */
public interface CatalogueRepository {

  /**
   * @param fournisseurId fournisseur
   * @return lignes triées par référence
   */
  List<LigneCatalogue> parFournisseur(UUID fournisseurId);

  /**
   * @param fournisseurId fournisseur
   * @param reference référence normalisée
   * @return ligne existante
   */
  Optional<LigneCatalogue> parReference(UUID fournisseurId, String reference);

  /**
   * @param ligne ligne à persister
   * @return ligne persistée
   */
  LigneCatalogue sauvegarder(LigneCatalogue ligne);

  /**
   * @param prix entrée d'historique à ajouter
   */
  void historiser(PrixHistorique prix);

  /**
   * @param fournisseurId fournisseur
   * @param reference référence (optionnelle)
   * @return historique, du plus récent au plus ancien
   */
  List<PrixHistorique> historique(UUID fournisseurId, String reference);

  /**
   * @param fournisseurIds fournisseurs concernés
   * @param categorie catégorie (optionnelle)
   * @param q texte recherché dans la désignation ou la référence (optionnel)
   * @return lignes correspondantes
   */
  List<LigneCatalogue> rechercher(List<UUID> fournisseurIds, CategorieMateriel categorie, String q);
}
