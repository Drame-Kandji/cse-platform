package sn.cse.events.payload;

import java.util.List;
import java.util.UUID;

/**
 * Instantané d'un compte utilisateur (UTILISATEUR_*), utilisé comme annuaire par ms-notification.
 */
public record UtilisateurPayload(
    UUID utilisateurId,
    String email,
    String prenom,
    String nom,
    String role,
    boolean actif,
    List<UUID> chantierIds) {

  public UtilisateurPayload {
    chantierIds = chantierIds == null ? List.of() : List.copyOf(chantierIds);
  }
}
