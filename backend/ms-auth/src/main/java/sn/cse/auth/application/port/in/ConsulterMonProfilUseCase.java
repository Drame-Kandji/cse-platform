package sn.cse.auth.application.port.in;

import sn.cse.auth.application.command.IdentiteJwt;
import sn.cse.auth.domain.model.Utilisateur;

/** Profil de l'utilisateur connecté ({@code GET /api/me}). */
public interface ConsulterMonProfilUseCase {

  /**
   * Retourne le profil enregistré ; s'il n'existe pas encore (synchronisation pas encore passée),
   * il est créé à partir du jeton (provisionnement à la volée).
   *
   * @param identite identité issue du JWT
   * @return le profil courant
   */
  Utilisateur consulter(IdentiteJwt identite);
}
