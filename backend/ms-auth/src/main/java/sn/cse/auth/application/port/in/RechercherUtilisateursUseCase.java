package sn.cse.auth.application.port.in;

import java.util.UUID;
import sn.cse.auth.application.query.CriteresRecherche;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Consultation des comptes par l'administrateur. */
public interface RechercherUtilisateursUseCase {

  /**
   * @param criteres filtres
   * @param page pagination
   * @return page de profils
   */
  PageResponse<Utilisateur> rechercher(CriteresRecherche criteres, PageQuery page);

  /**
   * @param id identifiant
   * @return le profil
   * @throws sn.cse.auth.domain.exception.UtilisateurIntrouvableException si absent
   */
  Utilisateur parId(UUID id);
}
