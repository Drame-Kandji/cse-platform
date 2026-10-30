package sn.cse.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.UUID;

/** Profil persisté {@code FOURNISSEUR}. */
@Entity
@DiscriminatorValue("FOURNISSEUR")
public class FournisseurEntity extends UtilisateurEntity {

  @Column(name = "fournisseur_id")
  private UUID fournisseurId;

  protected FournisseurEntity() {}

  /**
   * @param id identifiant Keycloak
   */
  public FournisseurEntity(UUID id) {
    super(id, "FOURNISSEUR");
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public void setFournisseurId(UUID fournisseurId) {
    this.fournisseurId = fournisseurId;
  }
}
