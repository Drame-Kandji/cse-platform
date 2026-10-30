package sn.cse.auth.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import sn.cse.auth.application.query.CriteresRecherche;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Port de persistance des profils utilisateurs. */
public interface UtilisateurRepository {

  /**
   * @param id identifiant Keycloak
   * @return le profil, s'il existe
   */
  Optional<Utilisateur> parId(UUID id);

  /**
   * @param email adresse e-mail (comparaison insensible à la casse)
   * @return le profil portant cette adresse, s'il existe
   */
  Optional<Utilisateur> parEmail(String email);

  /**
   * Crée ou met à jour un profil (y compris un changement de rôle, donc de sous-classe).
   *
   * @param utilisateur profil à enregistrer
   * @return le profil enregistré
   */
  Utilisateur enregistrer(Utilisateur utilisateur);

  /**
   * @param criteres filtres
   * @param page pagination et tri (champ déjà validé)
   * @return la page de profils
   */
  PageResponse<Utilisateur> rechercher(CriteresRecherche criteres, PageQuery page);

  /**
   * @return tous les profils (table de petite taille : annuaire interne)
   */
  List<Utilisateur> tous();

  /**
   * @return nombre de profils par rôle (les rôles sans profil sont absents)
   */
  Map<Role, Long> compterParRole();
}
