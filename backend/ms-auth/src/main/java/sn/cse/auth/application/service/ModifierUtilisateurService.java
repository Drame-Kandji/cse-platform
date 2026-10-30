package sn.cse.auth.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.auth.application.command.ModifierUtilisateurCommand;
import sn.cse.auth.application.port.in.ModifierUtilisateurUseCase;
import sn.cse.auth.application.port.out.IdentityProviderPort;
import sn.cse.auth.application.port.out.ProfilIdentite;
import sn.cse.auth.application.port.out.UtilisateurEventPublisher;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.domain.exception.EmailDejaUtiliseException;
import sn.cse.auth.domain.exception.OperationSurSoiInterditeException;
import sn.cse.auth.domain.exception.UtilisateurIntrouvableException;
import sn.cse.auth.domain.model.PolitiqueOtp;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;

/**
 * Modification d'un compte. Le profil est mis à jour dans la transaction, puis Keycloak : un échec
 * Keycloak annule la transaction ; un échec au commit est corrigé par la synchronisation suivante
 * (Keycloak fait foi pour l'identité).
 */
@Service
@Transactional
public class ModifierUtilisateurService implements ModifierUtilisateurUseCase {

  private final UtilisateurRepository repository;
  private final IdentityProviderPort identite;
  private final UtilisateurEventPublisher events;
  private final PolitiqueOtp politiqueOtp;

  /**
   * @param repository profils
   * @param identite fournisseur d'identité
   * @param events publication des événements
   * @param politiqueOtp rôles soumis à l'OTP
   */
  public ModifierUtilisateurService(
      UtilisateurRepository repository,
      IdentityProviderPort identite,
      UtilisateurEventPublisher events,
      PolitiqueOtp politiqueOtp) {
    this.repository = repository;
    this.identite = identite;
    this.events = events;
    this.politiqueOtp = politiqueOtp;
  }

  @Override
  public Utilisateur modifier(ModifierUtilisateurCommand commande) {
    Utilisateur actuel =
        repository
            .parId(commande.id())
            .orElseThrow(() -> new UtilisateurIntrouvableException(commande.id()));
    Role ancienRole = actuel.role();
    boolean changementRole = ancienRole != commande.role();
    if (changementRole && commande.id().equals(commande.auteurId())) {
      throw new OperationSurSoiInterditeException();
    }
    String email = commande.identite().email();
    repository
        .parEmail(email)
        .filter(autre -> !autre.id().equals(commande.id()))
        .ifPresent(
            autre -> {
              throw new EmailDejaUtiliseException(email);
            });

    Utilisateur modifie = actuel.convertirEn(commande.role(), commande.attributs());
    modifie.modifierIdentite(commande.identite());
    Utilisateur enregistre = repository.enregistrer(modifie);

    identite.mettreAJourProfil(
        commande.id(), new ProfilIdentite(enregistre.identite(), enregistre.attributs()));
    if (changementRole) {
      identite.assignerRole(commande.id(), commande.role());
      if (enregistre.otpObligatoire(politiqueOtp) && !enregistre.otpConfigure()) {
        identite.exigerConfigurationOtp(commande.id());
      }
    }
    events.publierModifie(enregistre);
    return enregistre;
  }
}
