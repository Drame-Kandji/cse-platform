package sn.cse.auth.infrastructure.persistence;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.UUID;

/** Profil persisté {@code ADMIN_SYSTEME}. */
@Entity
@DiscriminatorValue("ADMIN_SYSTEME")
public class AdministrateurSystemeEntity extends UtilisateurEntity {

  protected AdministrateurSystemeEntity() {}

  /**
   * @param id identifiant Keycloak
   */
  public AdministrateurSystemeEntity(UUID id) {
    super(id, "ADMIN_SYSTEME");
  }
}
