package sn.cse.auth.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.auth.application.command.CreerUtilisateurCommand;
import sn.cse.auth.application.port.in.CreerUtilisateurUseCase;
import sn.cse.auth.application.port.out.CompensationTransactionnelle;
import sn.cse.auth.application.port.out.IdentityProviderPort;
import sn.cse.auth.application.port.out.NouveauCompte;
import sn.cse.auth.application.port.out.UtilisateurEventPublisher;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.domain.exception.EmailDejaUtiliseException;
import sn.cse.auth.domain.model.PolitiqueOtp;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.auth.domain.model.Utilisateurs;

/**
 * Création d'un compte : compte Keycloak (mot de passe temporaire, actions requises), rôle unique,
 * puis profil local et événement {@code UTILISATEUR_CREE}. Si la transaction échoue après la
 * création Keycloak, le compte Keycloak est supprimé (compensation).
 */
@Service
@Transactional
public class CreerUtilisateurService implements CreerUtilisateurUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(CreerUtilisateurService.class);

  private final UtilisateurRepository repository;
  private final IdentityProviderPort identite;
  private final UtilisateurEventPublisher events;
  private final CompensationTransactionnelle compensation;
  private final PolitiqueOtp politiqueOtp;
  private final Clock clock;

  /**
   * @param repository profils
   * @param identite fournisseur d'identité
   * @param events publication des événements
   * @param compensation compensation en cas d'annulation
   * @param politiqueOtp rôles soumis à l'OTP
   * @param clock horloge
   */
  public CreerUtilisateurService(
      UtilisateurRepository repository,
      IdentityProviderPort identite,
      UtilisateurEventPublisher events,
      CompensationTransactionnelle compensation,
      PolitiqueOtp politiqueOtp,
      Clock clock) {
    this.repository = repository;
    this.identite = identite;
    this.events = events;
    this.compensation = compensation;
    this.politiqueOtp = politiqueOtp;
    this.clock = clock;
  }

  @Override
  public Utilisateur creer(CreerUtilisateurCommand commande) {
    String email = commande.identite().email();
    if (repository.parEmail(email).isPresent()) {
      throw new EmailDejaUtiliseException(email);
    }
    UUID id =
        identite.creerCompte(
            new NouveauCompte(
                commande.identite(),
                commande.role(),
                commande.attributs(),
                commande.motDePasseTemporaire(),
                politiqueOtp.estObligatoire(commande.role())));
    compensation.siAnnulation(() -> supprimerCompteOrphelin(id));
    identite.assignerRole(id, commande.role());
    Utilisateur profil =
        Utilisateurs.creer(
            commande.role(),
            new Utilisateur.EtatCommun(id, commande.identite(), true, false, Instant.now(clock)),
            commande.attributs());
    Utilisateur enregistre = repository.enregistrer(profil);
    events.publierCree(enregistre);
    LOG.info("Compte {} créé avec le rôle {}", id, commande.role());
    return enregistre;
  }

  private void supprimerCompteOrphelin(UUID id) {
    LOG.warn("Création annulée : suppression du compte Keycloak {}", id);
    identite.supprimerCompte(id);
  }
}
