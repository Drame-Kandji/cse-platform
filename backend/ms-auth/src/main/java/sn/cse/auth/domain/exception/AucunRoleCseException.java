package sn.cse.auth.domain.exception;

import sn.cse.common.exception.ForbiddenOperationException;

/** Jeton valide mais sans aucun rôle CSE : aucun profil ne peut être associé (HTTP 403). */
public class AucunRoleCseException extends ForbiddenOperationException {

  /** Construit l'exception. */
  public AucunRoleCseException() {
    super("AUCUN_ROLE_CSE", "Votre compte ne possède aucun rôle de la plateforme CSE.");
  }
}
