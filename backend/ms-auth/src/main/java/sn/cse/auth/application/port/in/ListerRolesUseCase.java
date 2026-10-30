package sn.cse.auth.application.port.in;

import java.util.List;
import sn.cse.auth.application.query.RoleResume;

/** Liste des rôles de la plateforme avec leurs effectifs. */
public interface ListerRolesUseCase {

  /**
   * @return un résumé par rôle, dans l'ordre de déclaration
   */
  List<RoleResume> lister();
}
