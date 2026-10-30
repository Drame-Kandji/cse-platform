package sn.cse.auth.domain.model;

/** Responsable de la Direction du Matériel, éventuellement limité à un périmètre de parc. */
public final class ResponsableMateriel extends Utilisateur {

  private String perimetreParc;

  /**
   * @param etat état commun
   * @param perimetreParc périmètre du parc géré (facultatif)
   */
  public ResponsableMateriel(EtatCommun etat, String perimetreParc) {
    super(etat);
    this.perimetreParc = new AttributsProfil(null, perimetreParc, null, null, null).perimetreParc();
  }

  @Override
  public Role role() {
    return Role.RESPONSABLE_MATERIEL;
  }

  @Override
  public AttributsProfil attributs() {
    return new AttributsProfil(null, perimetreParc, null, null, null);
  }

  @Override
  public void modifierAttributs(AttributsProfil attributs) {
    this.perimetreParc = attributs.perimetreParc();
  }

  /**
   * @return périmètre du parc, {@code null} si non restreint
   */
  public String perimetreParc() {
    return perimetreParc;
  }
}
