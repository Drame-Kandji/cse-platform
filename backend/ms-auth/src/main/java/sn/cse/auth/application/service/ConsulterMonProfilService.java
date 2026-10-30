package sn.cse.auth.application.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.auth.application.command.IdentiteJwt;
import sn.cse.auth.application.port.in.ConsulterMonProfilUseCase;
import sn.cse.auth.application.port.out.UtilisateurEventPublisher;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.domain.exception.AucunRoleCseException;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.auth.domain.model.Utilisateurs;

/**
 * Profil courant. Le profil enregistré fait foi ; à défaut (premier appel avant la
 * synchronisation), il est provisionné depuis le jeton et publié comme {@code
 * UTILISATEUR_SYNCHRONISE} afin d'alimenter l'annuaire de ms-notification.
 */
@Service
@Transactional
public class ConsulterMonProfilService implements ConsulterMonProfilUseCase {

  private final UtilisateurRepository repository;
  private final UtilisateurEventPublisher events;
  private final Clock clock;

  /**
   * @param repository profils
   * @param events publication des événements
   * @param clock horloge
   */
  public ConsulterMonProfilService(
      UtilisateurRepository repository, UtilisateurEventPublisher events, Clock clock) {
    this.repository = repository;
    this.events = events;
    this.clock = clock;
  }

  @Override
  public Utilisateur consulter(IdentiteJwt jwt) {
    return repository.parId(jwt.id()).orElseGet(() -> provisionner(jwt));
  }

  private Utilisateur provisionner(IdentiteJwt jwt) {
    Role role = Role.principal(jwt.roles()).orElseThrow(AucunRoleCseException::new);
    String email = jwt.email();
    Utilisateur.Identite identite =
        new Utilisateur.Identite(
            email, valeurOu(jwt.prenom(), email), valeurOu(jwt.nom(), email), null);
    Utilisateur profil =
        Utilisateurs.creer(
            role,
            new Utilisateur.EtatCommun(jwt.id(), identite, true, false, Instant.now(clock)),
            new AttributsProfil(jwt.chantierIds(), null, null, null, jwt.fournisseurId()));
    Utilisateur enregistre = repository.enregistrer(profil);
    events.publierSynchronise(enregistre);
    return enregistre;
  }

  private static String valeurOu(String valeur, String defaut) {
    return valeur == null || valeur.isBlank() ? defaut : valeur;
  }
}
