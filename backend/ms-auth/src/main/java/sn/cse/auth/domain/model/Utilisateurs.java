package sn.cse.auth.domain.model;

import java.util.Objects;

/**
 * Fabrique de profils : instancie la sous-classe de {@link Utilisateur} correspondant au rôle
 * (création, reconstitution depuis la persistance, changement de rôle).
 */
public final class Utilisateurs {

  private Utilisateurs() {}

  /**
   * @param role rôle du profil
   * @param etat état commun
   * @param attributs attributs spécifiques (ceux qui ne concernent pas le rôle sont ignorés)
   * @return le profil concret
   */
  public static Utilisateur creer(
      Role role, Utilisateur.EtatCommun etat, AttributsProfil attributs) {
    Objects.requireNonNull(role, "role");
    AttributsProfil a = attributs == null ? AttributsProfil.AUCUN : attributs;
    return switch (role) {
      case CHEF_CHANTIER -> new ChefDeChantier(etat, a.chantierIds());
      case RESPONSABLE_MATERIEL -> new ResponsableMateriel(etat, a.perimetreParc());
      case RESPONSABLE_ACHATS -> new ResponsableAchats(etat, a.seuilDelegation());
      case DAF -> new DAF(etat, a.budgetGere());
      case DIRECTION_GENERALE -> new DirectionGenerale(etat);
      case ADMIN_SYSTEME -> new AdministrateurSysteme(etat);
      case FOURNISSEUR -> new Fournisseur(etat, a.fournisseurId());
    };
  }
}
