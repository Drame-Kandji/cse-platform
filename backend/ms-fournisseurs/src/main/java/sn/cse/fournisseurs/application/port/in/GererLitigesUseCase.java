package sn.cse.fournisseurs.application.port.in;

import java.util.UUID;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CurrentUser;
import sn.cse.fournisseurs.application.command.LitigeCommand;
import sn.cse.fournisseurs.domain.model.Litige;
import sn.cse.fournisseurs.domain.model.StatutLitige;
import sn.cse.fournisseurs.domain.model.TypeActionLitige;

/** Gestion des litiges fournisseurs. */
public interface GererLitigesUseCase {

  /**
   * Ouvre un litige (référence LIT-AAAA-NNN) et recalcule le score.
   *
   * @param fournisseurId fournisseur
   * @param commande données
   * @param utilisateur auteur
   * @return litige ouvert
   */
  Litige ouvrir(UUID fournisseurId, LitigeCommand commande, CurrentUser utilisateur);

  /**
   * Ajoute une action ; une résolution ou un classement recalcule le score (R07).
   *
   * @param litigeId litige
   * @param type ACTION, RESOLUTION ou CLASSEMENT
   * @param commentaire commentaire obligatoire
   * @param utilisateur auteur
   * @return litige
   */
  Litige agir(UUID litigeId, TypeActionLitige type, String commentaire, CurrentUser utilisateur);

  /**
   * @param statut filtre (optionnel)
   * @param fournisseurId filtre (optionnel)
   * @param page pagination
   * @return page de litiges
   */
  PageResponse<Litige> rechercher(StatutLitige statut, UUID fournisseurId, PageQuery page);
}
