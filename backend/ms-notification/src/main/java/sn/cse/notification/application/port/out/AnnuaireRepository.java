package sn.cse.notification.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import sn.cse.notification.domain.model.UtilisateurAnnuaire;

/** Annuaire local des utilisateurs (projection de {@code cse.auth.events}). */
public interface AnnuaireRepository {

  /**
   * @param utilisateur version à enregistrer (insertion ou remplacement)
   */
  void enregistrer(UtilisateurAnnuaire utilisateur);

  /**
   * @param id identifiant
   * @return l'utilisateur, s'il est connu
   */
  Optional<UtilisateurAnnuaire> parId(UUID id);

  /**
   * @param chantierId chantier
   * @return les chefs de chantier actifs rattachés à ce chantier
   */
  List<UtilisateurAnnuaire> chefsDuChantier(UUID chantierId);

  /**
   * @param role code du rôle
   * @return les utilisateurs actifs détenant ce rôle
   */
  List<UtilisateurAnnuaire> actifsParRole(String role);
}
