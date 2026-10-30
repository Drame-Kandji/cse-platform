package sn.cse.fournisseurs.application.port.in;

import java.util.List;
import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.fournisseurs.application.query.CriteresFournisseurs;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.application.query.StatistiquesFournisseurs;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.service.ClassementFournisseurs;

/** Consultation du référentiel fournisseurs. */
public interface ConsulterFournisseursUseCase {

  /**
   * Recherche paginée (R14 : utilisée par ms-achats avec {@code statut=ACTIF&specialite=X}).
   *
   * @param criteres filtres
   * @param page pagination
   * @return page de fournisseurs
   */
  PageResponse<FournisseurVue> rechercher(CriteresFournisseurs criteres, PageQuery page);

  /**
   * Détail d'un fournisseur ; les coordonnées bancaires ne sont visibles que du RA et de la DAF.
   *
   * @param id fournisseur
   * @param utilisateur utilisateur courant
   * @return fournisseur détaillé
   */
  FournisseurVue detail(UUID id, CurrentUser utilisateur);

  /**
   * Classement dynamique des fournisseurs actifs.
   *
   * @param categorie catégorie (optionnelle)
   * @return classement
   */
  List<ClassementFournisseurs.Rang> classement(CategorieMateriel categorie);

  /**
   * @return statistiques globales
   */
  StatistiquesFournisseurs statistiques();
}
