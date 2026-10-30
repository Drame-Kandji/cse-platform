package sn.cse.auth.domain.model;

import java.util.UUID;

/**
 * Compte du portail fournisseur. Le rattachement à la fiche fournisseur (ms-fournisseurs) peut être
 * différé jusqu'à la validation de l'inscription par le Responsable des Achats (D7).
 */
public final class Fournisseur extends Utilisateur {

  private UUID fournisseurId;

  /**
   * @param etat état commun
   * @param fournisseurId fiche fournisseur rattachée (facultative)
   */
  public Fournisseur(EtatCommun etat, UUID fournisseurId) {
    super(etat);
    this.fournisseurId = fournisseurId;
  }

  @Override
  public Role role() {
    return Role.FOURNISSEUR;
  }

  @Override
  public AttributsProfil attributs() {
    return new AttributsProfil(null, null, null, null, fournisseurId);
  }

  @Override
  public void modifierAttributs(AttributsProfil attributs) {
    this.fournisseurId = attributs.fournisseurId();
  }

  /**
   * @return vrai si le compte est rattaché à une fiche fournisseur
   */
  public boolean estRattache() {
    return fournisseurId != null;
  }

  /**
   * @return fiche fournisseur rattachée, {@code null} sinon
   */
  public UUID fournisseurId() {
    return fournisseurId;
  }
}
