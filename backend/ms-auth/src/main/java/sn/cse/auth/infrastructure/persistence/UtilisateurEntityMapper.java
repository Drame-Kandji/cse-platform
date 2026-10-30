package sn.cse.auth.infrastructure.persistence;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.auth.domain.model.Utilisateurs;

/** Conversion entre la hiérarchie du domaine et la hiérarchie JPA (une classe par rôle). */
@Component
public class UtilisateurEntityMapper {

  /**
   * @param utilisateur profil du domaine
   * @return une entité vierge de la sous-classe correspondant au rôle
   */
  public UtilisateurEntity nouvelleEntite(Utilisateur utilisateur) {
    UUID id = utilisateur.id();
    return switch (utilisateur.role()) {
      case CHEF_CHANTIER -> new ChefDeChantierEntity(id);
      case RESPONSABLE_MATERIEL -> new ResponsableMaterielEntity(id);
      case RESPONSABLE_ACHATS -> new ResponsableAchatsEntity(id);
      case DAF -> new DafEntity(id);
      case DIRECTION_GENERALE -> new DirectionGeneraleEntity(id);
      case ADMIN_SYSTEME -> new AdministrateurSystemeEntity(id);
      case FOURNISSEUR -> new FournisseurEntity(id);
    };
  }

  /**
   * Recopie l'état du domaine dans une entité de même rôle.
   *
   * @param source profil du domaine
   * @param cible entité de la sous-classe correspondant au rôle
   */
  public void copier(Utilisateur source, UtilisateurEntity cible) {
    cible.setEmail(source.email());
    cible.setPrenom(source.prenom());
    cible.setNom(source.nom());
    cible.setTelephone(source.telephone());
    cible.setActif(source.actif());
    cible.setOtpConfigure(source.otpConfigure());
    if (cible.getCreatedAt() == null) {
      cible.setCreatedAt(source.createdAt());
    }
    AttributsProfil a = source.attributs();
    if (cible instanceof ChefDeChantierEntity c) {
      c.setChantierIds(a.chantierIds().stream().sorted().toArray(UUID[]::new));
    } else if (cible instanceof ResponsableMaterielEntity r) {
      r.setPerimetreParc(a.perimetreParc());
    } else if (cible instanceof ResponsableAchatsEntity r) {
      r.setSeuilDelegation(a.seuilDelegation());
    } else if (cible instanceof DafEntity d) {
      d.setBudgetGere(a.budgetGere());
    } else if (cible instanceof FournisseurEntity f) {
      f.setFournisseurId(a.fournisseurId());
    }
  }

  /**
   * @param entite entité persistée
   * @return le profil du domaine
   */
  public Utilisateur versDomaine(UtilisateurEntity entite) {
    Role role =
        Role.depuisCode(entite.getRole())
            .orElseThrow(() -> new IllegalStateException("Rôle inconnu : " + entite.getRole()));
    Utilisateur.EtatCommun etat =
        new Utilisateur.EtatCommun(
            entite.getId(),
            new Utilisateur.Identite(
                entite.getEmail(), entite.getPrenom(), entite.getNom(), entite.getTelephone()),
            entite.isActif(),
            entite.isOtpConfigure(),
            entite.getCreatedAt());
    return Utilisateurs.creer(role, etat, attributs(entite));
  }

  private static AttributsProfil attributs(UtilisateurEntity entite) {
    if (entite instanceof ChefDeChantierEntity c && c.getChantierIds() != null) {
      return new AttributsProfil(
          Set.copyOf(Arrays.asList(c.getChantierIds())), null, null, null, null);
    }
    if (entite instanceof ResponsableMaterielEntity r) {
      return new AttributsProfil(null, r.getPerimetreParc(), null, null, null);
    }
    if (entite instanceof ResponsableAchatsEntity r) {
      return new AttributsProfil(null, null, r.getSeuilDelegation(), null, null);
    }
    if (entite instanceof DafEntity d) {
      return new AttributsProfil(null, null, null, d.getBudgetGere(), null);
    }
    if (entite instanceof FournisseurEntity f) {
      return new AttributsProfil(null, null, null, null, f.getFournisseurId());
    }
    return AttributsProfil.AUCUN;
  }
}
