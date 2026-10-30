package sn.cse.auth.domain.exception;

import java.util.UUID;
import sn.cse.common.exception.ResourceNotFoundException;

/** Profil utilisateur inexistant : HTTP 404 {@code UTILISATEUR_INTROUVABLE}. */
public class UtilisateurIntrouvableException extends ResourceNotFoundException {

  /**
   * @param id identifiant recherché
   */
  public UtilisateurIntrouvableException(UUID id) {
    super("Utilisateur", id);
  }
}
