package sn.cse.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Profil persisté {@code CHEF_CHANTIER}. */
@Entity
@DiscriminatorValue("CHEF_CHANTIER")
public class ChefDeChantierEntity extends UtilisateurEntity {

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "chantier_ids", columnDefinition = "uuid[]")
  private UUID[] chantierIds;

  protected ChefDeChantierEntity() {}

  /**
   * @param id identifiant Keycloak
   */
  public ChefDeChantierEntity(UUID id) {
    super(id, "CHEF_CHANTIER");
  }

  public UUID[] getChantierIds() {
    return chantierIds;
  }

  public void setChantierIds(UUID[] chantierIds) {
    this.chantierIds = chantierIds;
  }
}
