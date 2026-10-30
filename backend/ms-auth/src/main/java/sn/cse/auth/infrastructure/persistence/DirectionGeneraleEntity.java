package sn.cse.auth.infrastructure.persistence;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.UUID;

/** Profil persisté {@code DIRECTION_GENERALE}. */
@Entity
@DiscriminatorValue("DIRECTION_GENERALE")
public class DirectionGeneraleEntity extends UtilisateurEntity {

  protected DirectionGeneraleEntity() {}

  /**
   * @param id identifiant Keycloak
   */
  public DirectionGeneraleEntity(UUID id) {
    super(id, "DIRECTION_GENERALE");
  }
}
