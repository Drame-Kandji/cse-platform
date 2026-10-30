package sn.cse.auth.api.mapper;

import java.util.Locale;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import sn.cse.auth.api.dto.CreerUtilisateurRequest;
import sn.cse.auth.api.dto.ModifierUtilisateurRequest;
import sn.cse.auth.api.dto.RoleInfoDto;
import sn.cse.auth.api.dto.UtilisateurDto;
import sn.cse.auth.application.command.CreerUtilisateurCommand;
import sn.cse.auth.application.command.ModifierUtilisateurCommand;
import sn.cse.auth.application.query.RoleResume;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;

/** Conversion DTO ↔ commandes et domaine ↔ DTO. Les libellés de rôles viennent des messages. */
@Component
public class UtilisateurApiMapper {

  private static final Locale FRANCAIS = Locale.FRENCH;

  private final MessageSource messages;

  /**
   * @param messages libellés ({@code messages_fr.properties})
   */
  public UtilisateurApiMapper(MessageSource messages) {
    this.messages = messages;
  }

  /**
   * @param u profil
   * @return DTO ; les attributs ne concernant pas le rôle valent {@code null}
   */
  public UtilisateurDto versDto(Utilisateur u) {
    AttributsProfil a = u.attributs();
    return new UtilisateurDto(
        u.id(),
        u.email(),
        u.prenom(),
        u.nom(),
        u.telephone(),
        u.role(),
        u.actif(),
        u.role() == Role.CHEF_CHANTIER ? a.chantierIds().stream().sorted().toList() : null,
        a.perimetreParc(),
        a.seuilDelegation(),
        a.budgetGere(),
        a.fournisseurId(),
        u.otpConfigure(),
        u.createdAt());
  }

  /**
   * @param r requête
   * @return commande de création
   */
  public CreerUtilisateurCommand versCommande(CreerUtilisateurRequest r) {
    return new CreerUtilisateurCommand(
        new Utilisateur.Identite(r.email(), r.prenom(), r.nom(), r.telephone()),
        r.role(),
        new AttributsProfil(
            r.chantierIds(),
            r.perimetreParc(),
            r.seuilDelegation(),
            r.budgetGere(),
            r.fournisseurId()),
        r.motDePasseTemporaire());
  }

  /**
   * @param id compte modifié
   * @param r requête
   * @param auteurId administrateur courant
   * @return commande de modification
   */
  public ModifierUtilisateurCommand versCommande(
      UUID id, ModifierUtilisateurRequest r, UUID auteurId) {
    return new ModifierUtilisateurCommand(
        id,
        new Utilisateur.Identite(r.email(), r.prenom(), r.nom(), r.telephone()),
        r.role(),
        new AttributsProfil(
            r.chantierIds(),
            r.perimetreParc(),
            r.seuilDelegation(),
            r.budgetGere(),
            r.fournisseurId()),
        auteurId);
  }

  /**
   * @param r résumé du rôle
   * @return DTO avec libellé et description localisés
   */
  public RoleInfoDto versDto(RoleResume r) {
    String cle = "role." + r.role().code();
    return new RoleInfoDto(
        r.role(),
        messages.getMessage(cle + ".libelle", null, r.role().code(), FRANCAIS),
        messages.getMessage(cle + ".description", null, "", FRANCAIS),
        r.nombreUtilisateurs(),
        r.otpObligatoire());
  }
}
