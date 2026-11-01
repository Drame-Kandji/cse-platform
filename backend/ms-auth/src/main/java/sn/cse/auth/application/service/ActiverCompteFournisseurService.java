package sn.cse.auth.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.auth.application.command.ActiverCompteFournisseurCommand;
import sn.cse.auth.application.port.in.ActiverCompteFournisseurUseCase;
import sn.cse.auth.application.port.out.CompensationTransactionnelle;
import sn.cse.auth.application.port.out.CompteIdentite;
import sn.cse.auth.application.port.out.IdentityProviderPort;
import sn.cse.auth.application.port.out.NouveauCompte;
import sn.cse.auth.application.port.out.UtilisateurEventPublisher;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.auth.domain.model.Utilisateurs;

/**
 * Compte du portail d'un fournisseur validé ({@code FOURNISSEUR_VALIDE}) : compte Keycloak sans mot
 * de passe (identifiant = e-mail, rôle {@code FOURNISSEUR}, attribut {@code fournisseur_id}, action
 * {@code UPDATE_PASSWORD}), e-mail « définir votre mot de passe », puis profil local et {@code
 * UTILISATEUR_CREE}.
 *
 * <p>Idempotent : un compte existant est seulement complété (rôle, attribut, e-mail si aucun mot de
 * passe n'a encore été défini, profil local). Un compte portant un autre rôle CSE (compte interne)
 * n'est jamais converti. Toute erreur Keycloak est propagée : le consommateur Kafka rejoue puis
 * publie en DLT ; une création suivie d'un échec est compensée (suppression du compte Keycloak).
 */
@Service
@Transactional
public class ActiverCompteFournisseurService implements ActiverCompteFournisseurUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(ActiverCompteFournisseurService.class);

  /** Caractères refusés par le validateur Keycloak {@code person-name-prohibited-characters}. */
  private static final Pattern CARACTERES_INTERDITS =
      Pattern.compile("[<>&\"$%!#?§;*~/\\\\|^=\\[\\]{}()\\p{Cntrl}]+");

  private static final String NOM_PAR_DEFAUT = "Fournisseur";

  private final UtilisateurRepository repository;
  private final IdentityProviderPort identite;
  private final UtilisateurEventPublisher events;
  private final CompensationTransactionnelle compensation;
  private final ParametresPortailFournisseur parametres;
  private final Clock clock;

  /**
   * @param repository profils
   * @param identite fournisseur d'identité
   * @param events publication des événements
   * @param compensation compensation en cas d'annulation
   * @param parametres paramètres des comptes du portail
   * @param clock horloge
   */
  public ActiverCompteFournisseurService(
      UtilisateurRepository repository,
      IdentityProviderPort identite,
      UtilisateurEventPublisher events,
      CompensationTransactionnelle compensation,
      ParametresPortailFournisseur parametres,
      Clock clock) {
    this.repository = repository;
    this.identite = identite;
    this.events = events;
    this.compensation = compensation;
    this.parametres = parametres;
    this.clock = clock;
  }

  @Override
  public void activer(ActiverCompteFournisseurCommand commande) {
    Optional<CompteIdentite> existant = identite.compteParEmail(commande.email());
    if (existant.isPresent()) {
      completer(existant.get(), commande);
    } else {
      creer(commande);
    }
  }

  private void creer(ActiverCompteFournisseurCommand commande) {
    Utilisateur.Identite id = identiteParDefaut(commande);
    AttributsProfil attributs = attributs(commande);
    UUID compte =
        identite.creerCompte(new NouveauCompte(id, Role.FOURNISSEUR, attributs, null, false));
    compensation.siAnnulation(() -> supprimerCompteOrphelin(compte));
    identite.assignerRole(compte, Role.FOURNISSEUR);
    identite.envoyerEmailActivation(compte, parametres.lien());
    Utilisateur profil =
        Utilisateurs.creer(
            Role.FOURNISSEUR,
            new Utilisateur.EtatCommun(compte, id, true, false, Instant.now(clock)),
            attributs);
    events.publierCree(repository.enregistrer(profil));
    LOG.info(
        "Compte du portail {} créé pour le fournisseur {} : e-mail d'activation envoyé",
        compte,
        commande.fournisseurId());
  }

  private void completer(CompteIdentite compte, ActiverCompteFournisseurCommand commande) {
    Optional<Role> role = Role.principal(compte.roles());
    if (role.isPresent() && role.get() != Role.FOURNISSEUR) {
      // Règle de sécurité : une inscription fournisseur ne doit jamais retirer le rôle d'un compte
      // interne portant la même adresse ; l'administrateur arbitre manuellement.
      LOG.warn(
          "Compte {} ({}) porte le rôle {} : rattachement au fournisseur {} refusé",
          compte.id(),
          commande.email(),
          role.get(),
          commande.fournisseurId());
      return;
    }
    if (role.isEmpty()) {
      identite.assignerRole(compte.id(), Role.FOURNISSEUR);
    }
    if (!commande.fournisseurId().equals(compte.attributs().fournisseurId())) {
      identite.rattacherFournisseur(compte.id(), commande.fournisseurId());
    }
    if (!identite.motDePasseDefini(compte.id())) {
      // Reprise après un échec (ou lien expiré) : le fournisseur n'a pas encore de mot de passe.
      identite.envoyerEmailActivation(compte.id(), parametres.lien());
    }
    synchroniserProfil(compte, commande);
  }

  private void synchroniserProfil(CompteIdentite compte, ActiverCompteFournisseurCommand commande) {
    AttributsProfil attributs = attributs(commande);
    Optional<Utilisateur> local = repository.parId(compte.id());
    if (local.isEmpty()) {
      Utilisateur profil =
          Utilisateurs.creer(
              Role.FOURNISSEUR,
              new Utilisateur.EtatCommun(
                  compte.id(),
                  identiteDuCompte(compte, commande),
                  compte.actif(),
                  compte.otpConfigure(),
                  Instant.now(clock)),
              attributs);
      events.publierCree(repository.enregistrer(profil));
      return;
    }
    Utilisateur actuel = local.get();
    Utilisateur.Instantane avant = actuel.instantane();
    Utilisateur profil = actuel.convertirEn(Role.FOURNISSEUR, attributs);
    if (!profil.instantane().equals(avant)) {
      events.publierSynchronise(repository.enregistrer(profil));
    }
  }

  /**
   * Identité du compte : l'interlocuteur déclaré à l'inscription (« Ndeye Fall » donne le prénom «
   * Ndeye » et le nom « Fall ») ; à défaut, le prénom configuré et la raison sociale.
   */
  private Utilisateur.Identite identiteParDefaut(ActiverCompteFournisseurCommand commande) {
    String contact = commande.contactNom() == null ? "" : nomAffichable(commande.contactNom());
    if (contact.isBlank() || NOM_PAR_DEFAUT.equals(contact)) {
      return new Utilisateur.Identite(
          commande.email(),
          parametres.prenomContact(),
          nomAffichable(commande.raisonSociale()),
          null);
    }
    int espace = contact.lastIndexOf(' ');
    if (espace < 0) {
      return new Utilisateur.Identite(
          commande.email(), contact, nomAffichable(commande.raisonSociale()), null);
    }
    return new Utilisateur.Identite(
        commande.email(), contact.substring(0, espace), contact.substring(espace + 1), null);
  }

  private Utilisateur.Identite identiteDuCompte(
      CompteIdentite compte, ActiverCompteFournisseurCommand commande) {
    Utilisateur.Identite kc = compte.identite();
    Utilisateur.Identite defaut = identiteParDefaut(commande);
    return new Utilisateur.Identite(
        commande.email(),
        kc.prenom() == null || kc.prenom().isBlank() ? defaut.prenom() : kc.prenom(),
        kc.nom() == null || kc.nom().isBlank() ? defaut.nom() : kc.nom(),
        null);
  }

  private static AttributsProfil attributs(ActiverCompteFournisseurCommand commande) {
    return new AttributsProfil(null, null, null, null, commande.fournisseurId());
  }

  /**
   * @param raisonSociale raison sociale
   * @return nom accepté par Keycloak (caractères interdits remplacés par des espaces)
   */
  static String nomAffichable(String raisonSociale) {
    if (raisonSociale == null) {
      return NOM_PAR_DEFAUT;
    }
    String nom =
        CARACTERES_INTERDITS.matcher(raisonSociale).replaceAll(" ").replaceAll("\\s+", " ");
    return nom.isBlank() ? NOM_PAR_DEFAUT : nom.strip();
  }

  private void supprimerCompteOrphelin(UUID id) {
    LOG.warn("Activation annulée : suppression du compte Keycloak {}", id);
    identite.supprimerCompte(id);
  }
}
