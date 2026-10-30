package sn.cse.auth.domain.model;

import java.math.BigDecimal;

/** Direction Administrative et Financière : engagements et paiements, sur un budget géré. */
public final class DAF extends Utilisateur {

  private BigDecimal budgetGere;

  /**
   * @param etat état commun
   * @param budgetGere budget géré en FCFA (positif ou nul, facultatif)
   */
  public DAF(EtatCommun etat, BigDecimal budgetGere) {
    super(etat);
    this.budgetGere = new AttributsProfil(null, null, null, budgetGere, null).budgetGere();
  }

  @Override
  public Role role() {
    return Role.DAF;
  }

  @Override
  public AttributsProfil attributs() {
    return new AttributsProfil(null, null, null, budgetGere, null);
  }

  @Override
  public void modifierAttributs(AttributsProfil attributs) {
    this.budgetGere = attributs.budgetGere();
  }

  /**
   * @return budget géré, {@code null} si non défini
   */
  public BigDecimal budgetGere() {
    return budgetGere;
  }
}
