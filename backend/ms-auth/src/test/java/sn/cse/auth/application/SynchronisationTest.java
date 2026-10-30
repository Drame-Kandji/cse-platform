package sn.cse.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sn.cse.auth.domain.Fixtures.CHANTIER_1;
import static sn.cse.auth.domain.Fixtures.ID;
import static sn.cse.auth.domain.Fixtures.IDENTITE;
import static sn.cse.auth.domain.Fixtures.utilisateur;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sn.cse.auth.application.port.out.CompteIdentite;
import sn.cse.auth.application.port.out.IdentityProviderPort;
import sn.cse.auth.application.port.out.UtilisateurEventPublisher;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.application.query.ResultatSynchronisation;
import sn.cse.auth.application.service.ImporteurComptes;
import sn.cse.auth.application.service.SynchroniserUtilisateursService;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;

class SynchronisationTest {

  private UtilisateurRepository repository;
  private UtilisateurEventPublisher events;
  private IdentityProviderPort identite;
  private ImporteurComptes importeur;

  @BeforeEach
  void initialiser() {
    repository = mock(UtilisateurRepository.class);
    events = mock(UtilisateurEventPublisher.class);
    identite = mock(IdentityProviderPort.class);
    when(repository.enregistrer(any())).thenAnswer(i -> i.getArgument(0));
    importeur =
        new ImporteurComptes(
            repository, events, Clock.fixed(Instant.parse("2026-10-30T00:00:00Z"), ZoneOffset.UTC));
  }

  private static CompteIdentite compte(UUID id, Set<String> roles, AttributsProfil attributs) {
    return new CompteIdentite(
        id,
        new Utilisateur.Identite(IDENTITE.email(), IDENTITE.prenom(), IDENTITE.nom(), null),
        true,
        roles,
        attributs,
        false);
  }

  @Test
  void should_importer_les_comptes_absents_when_premiere_synchronisation() {
    when(repository.tous()).thenReturn(List.of());
    CompteIdentite chef =
        compte(
            ID,
            Set.of("CHEF_CHANTIER", "default-roles-cse"),
            new AttributsProfil(Set.of(CHANTIER_1), null, null, null, null));
    when(identite.listerComptes()).thenReturn(List.of(chef));

    ResultatSynchronisation r =
        new SynchroniserUtilisateursService(identite, importeur).synchroniser();

    assertThat(r).isEqualTo(new ResultatSynchronisation(1, 0, 0, 0));
    ArgumentCaptor<Utilisateur> publie = ArgumentCaptor.forClass(Utilisateur.class);
    verify(events).publierSynchronise(publie.capture());
    assertThat(publie.getValue().role()).isEqualTo(Role.CHEF_CHANTIER);
    assertThat(publie.getValue().attributs().chantierIds()).containsExactly(CHANTIER_1);
  }

  @Test
  void should_etre_idempotent_when_profil_deja_a_jour() {
    Utilisateur existant = utilisateur(Role.DIRECTION_GENERALE);
    when(repository.tous()).thenReturn(List.of(existant));
    ResultatSynchronisation r =
        importeur.importer(
            List.of(compte(ID, Set.of("DIRECTION_GENERALE"), AttributsProfil.AUCUN)));
    assertThat(r).isEqualTo(new ResultatSynchronisation(0, 0, 0, 1));
    verify(events, never()).publierSynchronise(any());
    verify(repository, never()).enregistrer(any());
  }

  @Test
  void should_mettre_a_jour_et_conserver_le_telephone_when_role_change_dans_keycloak() {
    Utilisateur existant = utilisateur(Role.CHEF_CHANTIER);
    when(repository.tous()).thenReturn(List.of(existant));
    ResultatSynchronisation r =
        importeur.importer(List.of(compte(ID, Set.of("DAF"), AttributsProfil.AUCUN)));
    assertThat(r.modifies()).isEqualTo(1);
    ArgumentCaptor<Utilisateur> enregistre = ArgumentCaptor.forClass(Utilisateur.class);
    verify(repository).enregistrer(enregistre.capture());
    assertThat(enregistre.getValue().role()).isEqualTo(Role.DAF);
    assertThat(enregistre.getValue().telephone()).isEqualTo(IDENTITE.telephone());
    verify(events).publierSynchronise(enregistre.getValue());
  }

  @Test
  void should_desactiver_les_profils_absents_de_keycloak_when_compte_supprime() {
    Utilisateur orphelin = utilisateur(Role.DAF);
    when(repository.tous()).thenReturn(List.of(orphelin));
    ResultatSynchronisation r = importeur.importer(List.of());
    assertThat(r.desactives()).isEqualTo(1);
    assertThat(orphelin.actif()).isFalse();
    verify(events).publierSynchronise(orphelin);
  }

  @Test
  void should_ignorer_les_comptes_sans_role_cse_ou_incomplets_when_import() {
    when(repository.tous()).thenReturn(List.of());
    CompteIdentite sansRole = compte(UUID.randomUUID(), Set.of("offline_access"), null);
    CompteIdentite sansNom =
        new CompteIdentite(
            UUID.randomUUID(),
            new Utilisateur.Identite("x@cse.sn", "X", null, null),
            true,
            Set.of("DAF"),
            AttributsProfil.AUCUN,
            false);
    ResultatSynchronisation r = importeur.importer(List.of(sansRole, sansNom));
    assertThat(r).isEqualTo(new ResultatSynchronisation(0, 0, 0, 0));
    verify(repository, times(0)).enregistrer(any());
  }
}
