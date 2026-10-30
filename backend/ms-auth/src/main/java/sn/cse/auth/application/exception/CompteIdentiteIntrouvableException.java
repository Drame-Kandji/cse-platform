package sn.cse.auth.application.exception;

import java.util.UUID;
import sn.cse.common.exception.ResourceNotFoundException;

/** Le compte Keycloak d'un profil n'existe plus : HTTP 404 {@code COMPTE_INTROUVABLE}. */
public class CompteIdentiteIntrouvableException extends ResourceNotFoundException {

  /**
   * @param id identifiant du compte
   */
  public CompteIdentiteIntrouvableException(UUID id) {
    super("Compte", id);
  }
}
