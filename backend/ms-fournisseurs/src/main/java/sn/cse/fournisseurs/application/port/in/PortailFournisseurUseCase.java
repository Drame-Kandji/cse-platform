package sn.cse.fournisseurs.application.port.in;

import java.util.List;
import sn.cse.common.security.CurrentUser;
import sn.cse.fournisseurs.application.command.FournisseurCommand;
import sn.cse.fournisseurs.application.command.LigneCatalogueCommand;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.domain.model.LigneCatalogue;

/**
 * Portail fournisseur (rôle FOURNISSEUR) : chaque opération porte sur le fournisseur désigné par le
 * claim {@code fournisseur_id} du jeton.
 */
public interface PortailFournisseurUseCase {

  /**
   * @param utilisateur compte fournisseur
   * @return profil du fournisseur (coordonnées bancaires incluses : ce sont les siennes)
   */
  FournisseurVue profil(CurrentUser utilisateur);

  /**
   * @param utilisateur compte fournisseur
   * @param version version connue
   * @param commande nouvelles informations
   * @return profil modifié
   */
  FournisseurVue modifierProfil(CurrentUser utilisateur, long version, FournisseurCommand commande);

  /**
   * @param utilisateur compte fournisseur
   * @return lignes du catalogue
   */
  List<LigneCatalogue> catalogue(CurrentUser utilisateur);

  /**
   * @param utilisateur compte fournisseur
   * @param commande ligne à publier (upsert par référence, prix historisé)
   * @return ligne publiée
   */
  LigneCatalogue publierLigne(CurrentUser utilisateur, LigneCatalogueCommand commande);
}
