package sn.cse.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.util.UUID;

/** Profil persisté {@code RESPONSABLE_ACHATS}. */
@Entity
@DiscriminatorValue("RESPONSABLE_ACHATS")
public class ResponsableAchatsEntity extends UtilisateurEntity {

  @Column(name = "seuil_delegation", precision = 19, scale = 0)
  private BigDecimal seuilDelegation;

  protected ResponsableAchatsEntity() {}

  /**
   * @param id identifiant Keycloak
   */
  public ResponsableAchatsEntity(UUID id) {
    super(id, "RESPONSABLE_ACHATS");
  }

  public BigDecimal getSeuilDelegation() {
    return seuilDelegation;
  }

  public void setSeuilDelegation(BigDecimal seuilDelegation) {
    this.seuilDelegation = seuilDelegation;
  }
}
