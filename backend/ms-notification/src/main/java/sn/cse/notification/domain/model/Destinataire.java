package sn.cse.notification.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Destinataire d'une notification : un utilisateur précis ou tous les détenteurs d'un rôle. Une
 * notification adressée à un rôle n'est stockée qu'une fois ; son état de lecture est suivi par
 * utilisateur.
 */
public sealed interface Destinataire {

  /**
   * @param id identifiant Keycloak de l'utilisateur
   * @return un destinataire individuel
   */
  static Destinataire utilisateur(UUID id) {
    return new Utilisateur(id);
  }

  /**
   * @param code code du rôle (voir {@code Roles})
   * @return un destinataire collectif
   */
  static Destinataire role(String code) {
    return new Role(code);
  }

  /**
   * Destinataire individuel.
   *
   * @param id identifiant Keycloak
   */
  record Utilisateur(UUID id) implements Destinataire {
    public Utilisateur {
      Objects.requireNonNull(id, "id");
    }
  }

  /**
   * Destinataire collectif (tous les détenteurs du rôle).
   *
   * @param code code du rôle
   */
  record Role(String code) implements Destinataire {
    public Role {
      if (code == null || code.isBlank()) {
        throw new IllegalArgumentException("Le code du rôle est obligatoire");
      }
    }
  }
}
