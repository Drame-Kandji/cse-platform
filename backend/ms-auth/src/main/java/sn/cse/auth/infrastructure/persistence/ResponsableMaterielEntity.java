package sn.cse.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.UUID;

/** Profil persisté {@code RESPONSABLE_MATERIEL}. */
@Entity
@DiscriminatorValue("RESPONSABLE_MATERIEL")
public class ResponsableMaterielEntity extends UtilisateurEntity {

  @Column(name = "perimetre_parc", length = 100)
  private String perimetreParc;

  protected ResponsableMaterielEntity() {}

  /**
   * @param id identifiant Keycloak
   */
  public ResponsableMaterielEntity(UUID id) {
    super(id, "RESPONSABLE_MATERIEL");
  }

  public String getPerimetreParc() {
    return perimetreParc;
  }

  public void setPerimetreParc(String perimetreParc) {
    this.perimetreParc = perimetreParc;
  }
}
