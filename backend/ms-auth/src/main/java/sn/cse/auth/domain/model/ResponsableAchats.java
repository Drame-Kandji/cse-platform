package sn.cse.auth.domain.model;

import java.math.BigDecimal;

/**
 * Responsable des Achats. Le seuil de délégation personnel (FCFA) borne les attributions qu'il peut
 * engager sans approbation de la Direction Générale (R11) ; absent, le seuil global de ms-achats
 * s'applique.
 */
public final class ResponsableAchats extends Utilisateur {

  private BigDecimal seuilDelegation;

  /**
   * @param etat état commun
   * @param seuilDelegation seuil de délégation (positif ou nul, facultatif)
   */
  public ResponsableAchats(EtatCommun etat, BigDecimal seuilDelegation) {
    super(etat);
    this.seuilDelegation =
        new AttributsProfil(null, null, seuilDelegation, null, null).seuilDelegation();
  }

  @Override
  public Role role() {
    return Role.RESPONSABLE_ACHATS;
  }

  @Override
  public AttributsProfil attributs() {
    return new AttributsProfil(null, null, seuilDelegation, null, null);
  }

  @Override
  public void modifierAttributs(AttributsProfil attributs) {
    this.seuilDelegation = attributs.seuilDelegation();
  }

  /**
   * @param montant montant d'une attribution
   * @return vrai si le montant dépasse le seuil personnel (faux si aucun seuil n'est défini)
   */
  public boolean depasseSeuil(BigDecimal montant) {
    return seuilDelegation != null && montant.compareTo(seuilDelegation) > 0;
  }

  /**
   * @return seuil de délégation, {@code null} si non défini
   */
  public BigDecimal seuilDelegation() {
    return seuilDelegation;
  }
}
