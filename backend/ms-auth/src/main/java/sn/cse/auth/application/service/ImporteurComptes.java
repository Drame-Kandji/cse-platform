package sn.cse.auth.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.auth.application.port.out.CompteIdentite;
import sn.cse.auth.application.port.out.UtilisateurEventPublisher;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.application.query.ResultatSynchronisation;
import sn.cse.auth.domain.exception.AttributProfilInvalideException;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.auth.domain.model.Utilisateurs;

/**
 * Import idempotent de comptes Keycloak : crée les profils absents, met à jour ceux qui ont changé
 * (Keycloak fait foi pour l'identité, le rôle, le statut et les attributs ; le téléphone reste
 * propre à ms-auth) et désactive les profils dont le compte a disparu. Un événement {@code
 * UTILISATEUR_SYNCHRONISE} n'est publié que pour un profil réellement créé ou modifié.
 */
@Service
@Transactional
public class ImporteurComptes {

  private static final Logger LOG = LoggerFactory.getLogger(ImporteurComptes.class);

  private final UtilisateurRepository repository;
  private final UtilisateurEventPublisher events;
  private final Clock clock;

  /**
   * @param repository profils
   * @param events publication des événements
   * @param clock horloge
   */
  public ImporteurComptes(
      UtilisateurRepository repository, UtilisateurEventPublisher events, Clock clock) {
    this.repository = repository;
    this.events = events;
    this.clock = clock;
  }

  private enum Issue {
    CREE,
    MODIFIE,
    INCHANGE,
    IGNORE
  }

  /**
   * @param comptes comptes lus dans Keycloak (liste complète du realm)
   * @return le bilan
   */
  public ResultatSynchronisation importer(List<CompteIdentite> comptes) {
    Map<UUID, Utilisateur> existants =
        repository.tous().stream().collect(Collectors.toMap(Utilisateur::id, Function.identity()));
    int crees = 0;
    int modifies = 0;
    int inchanges = 0;
    for (CompteIdentite compte : comptes) {
      switch (importer(compte, Optional.ofNullable(existants.get(compte.id())))) {
        case CREE -> crees++;
        case MODIFIE -> modifies++;
        case INCHANGE -> inchanges++;
        case IGNORE -> LOG.debug("Compte {} ignoré", compte.id());
      }
    }
    int desactives = desactiverAbsents(existants, comptes);
    return new ResultatSynchronisation(crees, modifies, desactives, inchanges);
  }

  private Issue importer(CompteIdentite compte, Optional<Utilisateur> existant) {
    Optional<Role> role = Role.principal(compte.roles());
    if (role.isEmpty()) {
      return Issue.IGNORE;
    }
    try {
      return existant.isPresent()
          ? mettreAJour(existant.get(), compte, role.get())
          : creer(compte, role.get());
    } catch (AttributProfilInvalideException e) {
      // Un compte Keycloak incomplet (ex. sans nom) ne doit pas bloquer tout l'annuaire.
      LOG.warn("Compte Keycloak {} non importé : {}", compte.id(), e.getMessage());
      return Issue.IGNORE;
    }
  }

  private Issue creer(CompteIdentite compte, Role role) {
    Utilisateur profil =
        Utilisateurs.creer(
            role,
            new Utilisateur.EtatCommun(
                compte.id(),
                compte.identite(),
                compte.actif(),
                compte.otpConfigure(),
                Instant.now(clock)),
            compte.attributs());
    events.publierSynchronise(repository.enregistrer(profil));
    return Issue.CREE;
  }

  private Issue mettreAJour(Utilisateur actuel, CompteIdentite compte, Role role) {
    Utilisateur.Instantane avant = actuel.instantane();
    Utilisateur profil = actuel.convertirEn(role, compte.attributs());
    Utilisateur.Identite identite = compte.identite();
    profil.modifierIdentite(
        new Utilisateur.Identite(
            identite.email(), identite.prenom(), identite.nom(), actuel.telephone()));
    profil.definirActif(compte.actif());
    profil.definirOtpConfigure(compte.otpConfigure());
    if (profil.instantane().equals(avant)) {
      return Issue.INCHANGE;
    }
    events.publierSynchronise(repository.enregistrer(profil));
    return Issue.MODIFIE;
  }

  private int desactiverAbsents(Map<UUID, Utilisateur> existants, List<CompteIdentite> comptes) {
    Set<UUID> presents = comptes.stream().map(CompteIdentite::id).collect(Collectors.toSet());
    int desactives = 0;
    for (Utilisateur profil : existants.values()) {
      if (profil.actif() && !presents.contains(profil.id())) {
        profil.definirActif(false);
        events.publierSynchronise(repository.enregistrer(profil));
        desactives++;
      }
    }
    return desactives;
  }
}
