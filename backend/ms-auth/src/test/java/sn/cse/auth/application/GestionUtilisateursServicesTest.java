package sn.cse.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static sn.cse.auth.domain.Fixtures.CHANTIER_1;
import static sn.cse.auth.domain.Fixtures.ID;
import static sn.cse.auth.domain.Fixtures.IDENTITE;
import static sn.cse.auth.domain.Fixtures.utilisateur;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sn.cse.auth.application.command.ChangerStatutCommand;
import sn.cse.auth.application.command.CreerUtilisateurCommand;
import sn.cse.auth.application.command.IdentiteJwt;
import sn.cse.auth.application.command.ModifierUtilisateurCommand;
import sn.cse.auth.application.command.ReinitialiserMotDePasseCommand;
import sn.cse.auth.application.port.out.CompensationTransactionnelle;
import sn.cse.auth.application.port.out.IdentityProviderPort;
import sn.cse.auth.application.port.out.NouveauCompte;
import sn.cse.auth.application.port.out.ProfilIdentite;
import sn.cse.auth.application.port.out.UtilisateurEventPublisher;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.application.query.CriteresRecherche;
import sn.cse.auth.application.query.RoleResume;
import sn.cse.auth.application.service.ChangerStatutUtilisateurService;
import sn.cse.auth.application.service.ConsulterMonProfilService;
import sn.cse.auth.application.service.CreerUtilisateurService;
import sn.cse.auth.application.service.ListerRolesService;
import sn.cse.auth.application.service.ModifierUtilisateurService;
import sn.cse.auth.application.service.RechercherUtilisateursService;
import sn.cse.auth.application.service.ReinitialiserMotDePasseService;
import sn.cse.auth.domain.exception.AucunRoleCseException;
import sn.cse.auth.domain.exception.EmailDejaUtiliseException;
import sn.cse.auth.domain.exception.OperationSurSoiInterditeException;
import sn.cse.auth.domain.exception.UtilisateurIntrouvableException;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.ChefDeChantier;
import sn.cse.auth.domain.model.PolitiqueOtp;
import sn.cse.auth.domain.model.ResponsableMateriel;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

class GestionUtilisateursServicesTest {

  private static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000601");
  private static final Instant MAINTENANT = Instant.parse("2026-10-30T09:15:00Z");
  private static final PolitiqueOtp POLITIQUE =
      new PolitiqueOtp(
          Set.of(Role.DAF, Role.DIRECTION_GENERALE, Role.RESPONSABLE_ACHATS, Role.ADMIN_SYSTEME));

  private UtilisateurRepository repository;
  private IdentityProviderPort identite;
  private UtilisateurEventPublisher events;
  private CompensationTransactionnelle compensation;
  private final Clock clock = Clock.fixed(MAINTENANT, ZoneOffset.UTC);

  @BeforeEach
  void initialiser() {
    repository = mock(UtilisateurRepository.class);
    identite = mock(IdentityProviderPort.class);
    events = mock(UtilisateurEventPublisher.class);
    compensation = mock(CompensationTransactionnelle.class);
    when(repository.enregistrer(any())).thenAnswer(i -> i.getArgument(0));
  }

  // --- Création ---

  @Test
  void should_creer_compte_keycloak_role_et_profil_when_creation() {
    when(repository.parEmail("daf2@cse.sn")).thenReturn(Optional.empty());
    UUID nouvelId = UUID.randomUUID();
    when(identite.creerCompte(any())).thenReturn(nouvelId);
    CreerUtilisateurService service =
        new CreerUtilisateurService(repository, identite, events, compensation, POLITIQUE, clock);
    Utilisateur.Identite id = new Utilisateur.Identite("daf2@cse.sn", "Awa", "Diallo", null);

    Utilisateur cree =
        service.creer(
            new CreerUtilisateurCommand(id, Role.DAF, AttributsProfil.AUCUN, "Temp@2026!"));

    ArgumentCaptor<NouveauCompte> compte = ArgumentCaptor.forClass(NouveauCompte.class);
    verify(identite).creerCompte(compte.capture());
    assertThat(compte.getValue().otpObligatoire()).isTrue();
    assertThat(compte.getValue().motDePasseTemporaire()).isEqualTo("Temp@2026!");
    assertThat(compte.getValue().toString()).doesNotContain("Temp@2026!");
    verify(identite).assignerRole(nouvelId, Role.DAF);
    verify(compensation).siAnnulation(any());
    verify(events).publierCree(cree);
    assertThat(cree.id()).isEqualTo(nouvelId);
    assertThat(cree.createdAt()).isEqualTo(MAINTENANT);
    assertThat(cree.actif()).isTrue();
  }

  @Test
  void should_ne_pas_exiger_otp_when_role_non_sensible() {
    when(repository.parEmail(any())).thenReturn(Optional.empty());
    when(identite.creerCompte(any())).thenReturn(UUID.randomUUID());
    new CreerUtilisateurService(repository, identite, events, compensation, POLITIQUE, clock)
        .creer(
            new CreerUtilisateurCommand(
                IDENTITE,
                Role.CHEF_CHANTIER,
                new AttributsProfil(Set.of(CHANTIER_1), null, null, null, null),
                "Temp@2026!"));
    ArgumentCaptor<NouveauCompte> compte = ArgumentCaptor.forClass(NouveauCompte.class);
    verify(identite).creerCompte(compte.capture());
    assertThat(compte.getValue().otpObligatoire()).isFalse();
  }

  @Test
  void should_supprimer_le_compte_keycloak_when_compensation_declenchee() {
    when(repository.parEmail(any())).thenReturn(Optional.empty());
    UUID nouvelId = UUID.randomUUID();
    when(identite.creerCompte(any())).thenReturn(nouvelId);
    new CreerUtilisateurService(repository, identite, events, compensation, POLITIQUE, clock)
        .creer(new CreerUtilisateurCommand(IDENTITE, Role.DAF, null, "Temp@2026!"));
    ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);
    verify(compensation).siAnnulation(action.capture());
    action.getValue().run();
    verify(identite).supprimerCompte(nouvelId);
  }

  @Test
  void should_refuser_la_creation_when_email_deja_utilise() {
    when(repository.parEmail(IDENTITE.email())).thenReturn(Optional.of(utilisateur(Role.DAF)));
    CreerUtilisateurService service =
        new CreerUtilisateurService(repository, identite, events, compensation, POLITIQUE, clock);
    assertThatThrownBy(
            () ->
                service.creer(new CreerUtilisateurCommand(IDENTITE, Role.DAF, null, "Temp@2026!")))
        .isInstanceOf(EmailDejaUtiliseException.class);
    verifyNoInteractions(identite, events);
  }

  // --- Modification ---

  @Test
  void should_mettre_a_jour_profil_et_keycloak_when_modification_sans_changement_de_role() {
    when(repository.parId(ID)).thenReturn(Optional.of(utilisateur(Role.CHEF_CHANTIER)));
    when(repository.parEmail(any())).thenReturn(Optional.empty());
    ModifierUtilisateurService service =
        new ModifierUtilisateurService(repository, identite, events, POLITIQUE);
    Utilisateur.Identite nouvelle =
        new Utilisateur.Identite("m.diop@cse.sn", "Mamadou", "Diop", null);

    Utilisateur modifie =
        service.modifier(
            new ModifierUtilisateurCommand(
                ID,
                nouvelle,
                Role.CHEF_CHANTIER,
                new AttributsProfil(Set.of(CHANTIER_1), null, null, null, null),
                ADMIN));

    assertThat(modifie).isInstanceOf(ChefDeChantier.class);
    assertThat(modifie.email()).isEqualTo("m.diop@cse.sn");
    verify(identite)
        .mettreAJourProfil(
            ID,
            new ProfilIdentite(
                nouvelle, new AttributsProfil(Set.of(CHANTIER_1), null, null, null, null)));
    verify(identite, never()).assignerRole(any(), any());
    verify(events).publierModifie(modifie);
  }

  @Test
  void should_changer_role_et_exiger_otp_when_nouveau_role_sensible() {
    when(repository.parId(ID)).thenReturn(Optional.of(utilisateur(Role.CHEF_CHANTIER)));
    when(repository.parEmail(IDENTITE.email()))
        .thenReturn(Optional.of(utilisateur(Role.CHEF_CHANTIER)));
    Utilisateur modifie =
        new ModifierUtilisateurService(repository, identite, events, POLITIQUE)
            .modifier(
                new ModifierUtilisateurCommand(
                    ID, IDENTITE, Role.DIRECTION_GENERALE, AttributsProfil.AUCUN, ADMIN));
    assertThat(modifie.role()).isEqualTo(Role.DIRECTION_GENERALE);
    verify(identite).assignerRole(ID, Role.DIRECTION_GENERALE);
    verify(identite).exigerConfigurationOtp(ID);
  }

  @Test
  void should_changer_role_sans_otp_when_nouveau_role_non_sensible() {
    when(repository.parId(ID)).thenReturn(Optional.of(utilisateur(Role.CHEF_CHANTIER)));
    when(repository.parEmail(any())).thenReturn(Optional.empty());
    Utilisateur modifie =
        new ModifierUtilisateurService(repository, identite, events, POLITIQUE)
            .modifier(
                new ModifierUtilisateurCommand(
                    ID, IDENTITE, Role.RESPONSABLE_MATERIEL, AttributsProfil.AUCUN, ADMIN));
    assertThat(modifie).isInstanceOf(ResponsableMateriel.class);
    verify(identite).assignerRole(ID, Role.RESPONSABLE_MATERIEL);
    verify(identite, never()).exigerConfigurationOtp(any());
  }

  @Test
  void should_refuser_when_email_appartient_a_un_autre_compte() {
    when(repository.parId(ID)).thenReturn(Optional.of(utilisateur(Role.CHEF_CHANTIER)));
    Utilisateur autre =
        sn.cse.auth.domain.model.Utilisateurs.creer(
            Role.DAF, sn.cse.auth.domain.Fixtures.etat(UUID.randomUUID()), null);
    when(repository.parEmail(IDENTITE.email())).thenReturn(Optional.of(autre));
    ModifierUtilisateurService service =
        new ModifierUtilisateurService(repository, identite, events, POLITIQUE);
    assertThatThrownBy(
            () ->
                service.modifier(
                    new ModifierUtilisateurCommand(
                        ID, IDENTITE, Role.CHEF_CHANTIER, AttributsProfil.AUCUN, ADMIN)))
        .isInstanceOf(EmailDejaUtiliseException.class);
    verifyNoInteractions(identite);
  }

  @Test
  void should_refuser_when_admin_change_son_propre_role() {
    when(repository.parId(ADMIN)).thenReturn(Optional.of(utilisateur(Role.ADMIN_SYSTEME)));
    ModifierUtilisateurService service =
        new ModifierUtilisateurService(repository, identite, events, POLITIQUE);
    assertThatThrownBy(
            () ->
                service.modifier(
                    new ModifierUtilisateurCommand(
                        ADMIN, IDENTITE, Role.DAF, AttributsProfil.AUCUN, ADMIN)))
        .isInstanceOf(OperationSurSoiInterditeException.class);
  }

  @Test
  void should_lever_404_when_modification_compte_inconnu() {
    when(repository.parId(ID)).thenReturn(Optional.empty());
    ModifierUtilisateurService service =
        new ModifierUtilisateurService(repository, identite, events, POLITIQUE);
    assertThatThrownBy(
            () ->
                service.modifier(
                    new ModifierUtilisateurCommand(ID, IDENTITE, Role.DAF, null, ADMIN)))
        .isInstanceOf(UtilisateurIntrouvableException.class);
  }

  // --- Statut ---

  @Test
  void should_desactiver_dans_keycloak_et_publier_when_changement_de_statut() {
    when(repository.parId(ID)).thenReturn(Optional.of(utilisateur(Role.DAF)));
    ChangerStatutUtilisateurService service =
        new ChangerStatutUtilisateurService(repository, identite, events);
    Utilisateur u = service.changerStatut(new ChangerStatutCommand(ID, false, ADMIN));
    assertThat(u.actif()).isFalse();
    verify(identite).definirActif(ID, false);
    verify(events).publierModifie(u);
  }

  @Test
  void should_ne_rien_faire_when_statut_deja_atteint() {
    when(repository.parId(ID)).thenReturn(Optional.of(utilisateur(Role.DAF)));
    new ChangerStatutUtilisateurService(repository, identite, events)
        .changerStatut(new ChangerStatutCommand(ID, true, ADMIN));
    verifyNoInteractions(identite, events);
  }

  @Test
  void should_refuser_when_admin_se_desactive() {
    ChangerStatutUtilisateurService service =
        new ChangerStatutUtilisateurService(repository, identite, events);
    assertThatThrownBy(() -> service.changerStatut(new ChangerStatutCommand(ADMIN, false, ADMIN)))
        .isInstanceOf(OperationSurSoiInterditeException.class);
  }

  @Test
  void should_lever_404_when_statut_compte_inconnu() {
    when(repository.parId(ID)).thenReturn(Optional.empty());
    ChangerStatutUtilisateurService service =
        new ChangerStatutUtilisateurService(repository, identite, events);
    assertThatThrownBy(() -> service.changerStatut(new ChangerStatutCommand(ID, false, ADMIN)))
        .isInstanceOf(UtilisateurIntrouvableException.class);
  }

  // --- Mot de passe ---

  @Test
  void should_definir_mot_de_passe_temporaire_when_reinitialisation() {
    when(repository.parId(ID)).thenReturn(Optional.of(utilisateur(Role.DAF)));
    ReinitialiserMotDePasseCommand commande =
        new ReinitialiserMotDePasseCommand(ID, "Nouveau@2026");
    new ReinitialiserMotDePasseService(repository, identite).reinitialiser(commande);
    verify(identite).reinitialiserMotDePasse(ID, "Nouveau@2026");
    assertThat(commande.toString()).doesNotContain("Nouveau@2026");
  }

  @Test
  void should_lever_404_when_reinitialisation_compte_inconnu() {
    when(repository.parId(ID)).thenReturn(Optional.empty());
    ReinitialiserMotDePasseService service =
        new ReinitialiserMotDePasseService(repository, identite);
    assertThatThrownBy(
            () -> service.reinitialiser(new ReinitialiserMotDePasseCommand(ID, "Nouveau@2026")))
        .isInstanceOf(UtilisateurIntrouvableException.class);
    verifyNoInteractions(identite);
  }

  // --- Consultation ---

  @Test
  void should_deleguer_au_depot_when_recherche_et_consultation() {
    PageQuery page = new PageQuery(0, 20, "nom", true);
    CriteresRecherche criteres = new CriteresRecherche(Role.DAF, true, "  ba ");
    assertThat(criteres.texte()).isEqualTo("ba");
    PageResponse<Utilisateur> resultat = PageResponse.of(List.of(utilisateur(Role.DAF)), 0, 20, 1);
    when(repository.rechercher(criteres, page)).thenReturn(resultat);
    when(repository.parId(ID)).thenReturn(Optional.of(utilisateur(Role.DAF)));
    RechercherUtilisateursService service = new RechercherUtilisateursService(repository);
    assertThat(service.rechercher(criteres, page)).isSameAs(resultat);
    assertThat(service.parId(ID).role()).isEqualTo(Role.DAF);
    when(repository.parId(ADMIN)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.parId(ADMIN))
        .isInstanceOf(UtilisateurIntrouvableException.class);
  }

  @Test
  void should_lister_tous_les_roles_avec_effectifs_when_roles() {
    when(repository.compterParRole()).thenReturn(Map.of(Role.CHEF_CHANTIER, 2L, Role.DAF, 1L));
    List<RoleResume> roles = new ListerRolesService(repository, POLITIQUE).lister();
    assertThat(roles).hasSize(Role.values().length);
    assertThat(roles)
        .contains(
            new RoleResume(Role.CHEF_CHANTIER, 2, false),
            new RoleResume(Role.DAF, 1, true),
            new RoleResume(Role.FOURNISSEUR, 0, false));
  }

  // --- Mon profil ---

  @Test
  void should_retourner_le_profil_enregistre_when_existant() {
    Utilisateur existant = utilisateur(Role.CHEF_CHANTIER);
    when(repository.parId(ID)).thenReturn(Optional.of(existant));
    Utilisateur moi =
        new ConsulterMonProfilService(repository, events, clock)
            .consulter(jwt(Set.of("CHEF_CHANTIER")));
    assertThat(moi).isSameAs(existant);
    verifyNoInteractions(events);
  }

  @Test
  void should_provisionner_depuis_le_jeton_when_profil_absent() {
    when(repository.parId(ID)).thenReturn(Optional.empty());
    Utilisateur moi =
        new ConsulterMonProfilService(repository, events, clock)
            .consulter(jwt(Set.of("default-roles-cse", "CHEF_CHANTIER")));
    assertThat(moi).isInstanceOf(ChefDeChantier.class);
    assertThat(moi.attributs().chantierIds()).containsExactly(CHANTIER_1);
    assertThat(moi.createdAt()).isEqualTo(MAINTENANT);
    verify(events).publierSynchronise(moi);
  }

  @Test
  void should_utiliser_email_when_noms_absents_du_jeton() {
    when(repository.parId(ID)).thenReturn(Optional.empty());
    Utilisateur moi =
        new ConsulterMonProfilService(repository, events, clock)
            .consulter(new IdentiteJwt(ID, "x@cse.sn", null, " ", Set.of("DAF"), null, null));
    assertThat(moi.prenom()).isEqualTo("x@cse.sn");
    assertThat(moi.nom()).isEqualTo("x@cse.sn");
  }

  @Test
  void should_refuser_when_jeton_sans_role_cse() {
    when(repository.parId(ID)).thenReturn(Optional.empty());
    ConsulterMonProfilService service = new ConsulterMonProfilService(repository, events, clock);
    assertThatThrownBy(() -> service.consulter(jwt(Set.of("offline_access"))))
        .isInstanceOf(AucunRoleCseException.class);
    verify(repository, never()).enregistrer(any());
  }

  @Test
  void should_propager_erreur_keycloak_when_assignation_echoue() {
    when(repository.parEmail(any())).thenReturn(Optional.empty());
    UUID nouvelId = UUID.randomUUID();
    when(identite.creerCompte(any())).thenReturn(nouvelId);
    doThrow(new IllegalStateException("boom")).when(identite).assignerRole(eq(nouvelId), any());
    CreerUtilisateurService service =
        new CreerUtilisateurService(repository, identite, events, compensation, POLITIQUE, clock);
    assertThatThrownBy(
            () ->
                service.creer(new CreerUtilisateurCommand(IDENTITE, Role.DAF, null, "Temp@2026!")))
        .isInstanceOf(IllegalStateException.class);
    verify(compensation).siAnnulation(any());
    verifyNoInteractions(events);
  }

  private static IdentiteJwt jwt(Set<String> roles) {
    return new IdentiteJwt(
        ID, IDENTITE.email(), "Mamadou", "Diop", roles, Set.of(CHANTIER_1), null);
  }
}
