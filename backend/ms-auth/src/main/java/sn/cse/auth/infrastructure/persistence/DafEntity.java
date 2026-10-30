package sn.cse.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.util.UUID;

/** Profil persisté {@code DAF}. */
@Entity
@DiscriminatorValue("DAF")
public class DafEntity extends UtilisateurEntity {

  @Column(name = "budget_gere", precision = 19, scale = 0)
  private BigDecimal budgetGere;

  protected DafEntity() {}

  /**
   * @param id identifiant Keycloak
   */
  public DafEntity(UUID id) {
    super(id, "DAF");
  }

  public BigDecimal getBudgetGere() {
    return budgetGere;
  }

  public void setBudgetGere(BigDecimal budgetGere) {
    this.budgetGere = budgetGere;
  }
}
