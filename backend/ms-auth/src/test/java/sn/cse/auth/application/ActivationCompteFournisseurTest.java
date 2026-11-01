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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sn.cse.auth.application.command.ActiverCompteFournisseurCommand;
import sn.cse.auth.application.exception.IdentiteIndisponibleException;
import sn.cse.auth.application.port.out.CompensationTransactionnelle;
import sn.cse.auth.application.port.out.CompteIdentite;
import sn.cse.auth.application.port.out.IdentityProviderPort;
import sn.cse.auth.application.port.out.LienActivation;
import sn.cse.auth.application.port.out.NouveauCompte;
import sn.cse.auth.application.port.out.UtilisateurEventPublisher;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.application.service.ActiverCompteFournisseurService;
import sn.cse.auth.application.service.ParametresPortailFournisseur;
import sn.cse.auth.domain.model.AttributsProfil;
import sn.cse.auth.domain.model.Fournisseur;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur;
import sn.cse.auth.domain.model.Utilisateurs;

class ActivationCompteFournisseurTest {

  private static final Instant MAINTENANT = Instant.parse("2026-11-01T10:00:00Z");
  private static final UUID FOURNISSEUR_ID =
      UUID.fromString("f0000000-0000-0000-0000-000000000007");
  private static final UUID COMPTE = UUID.fromString("00000000-0000-0000-0000-000000000777");
  private static final String EMAIL = "contact@baobab-logistique.sn";
  private static final LienActivation LIEN =
      new LienActivation("cse-portail-fournisseur", "http://localhost:5174/", Duration.ofHours(72));
  private static final ActiverCompteFournisseurCommand COMMANDE =
      new ActiverCompteFournisseurCommand(
          FOURNISSEUR_ID, " Contact@Baobab-Logistique.SN ", "Baobab Logistique");

  private UtilisateurRepository repository;
  private IdentityProviderPort identite;
  private UtilisateurEventPublisher events;
  private CompensationTransactionnelle compensation;
  private ActiverCompteFournisseurService service;

  @BeforeEach
  void initialiser() {
    repository = mock(UtilisateurRepository.class);
    identite = mock(IdentityProviderPort.class);
    events = mock(UtilisateurEventPublisher.class);
    compensation = mock(CompensationTransactionnelle.class);
    when(repository.enregistrer(any())).thenAnswer(i -> i.getArgument(0));
    service =
        new ActiverCompteFournisseurService(
            repository,
            identite,
            events,
            compensation,
            new ParametresPortailFournisseur("Contact", LIEN),
            Clock.fixed(MAINTENANT, ZoneOffset.UTC));
  }

  private static CompteIdentite compte(Set<String> roles, UUID fournisseurId) {
    return new CompteIdentite(
        COMPTE,
        new Utilisateur.Identite(EMAIL, "Seynabou", "Gueye", null),
        true,
        roles,
        new AttributsProfil(null, null, null, null, fournisseurId),
        false);
  }

  @Test
  void should_creer_compte_sans_mot_de_passe_role_attribut_et_envoyer_l_e_mail() {
    when(identite.compteParEmail(EMAIL)).thenReturn(Optional.empty());
    when(identite.creerCompte(any())).thenReturn(COMPTE);

    service.activer(COMMANDE);

    ArgumentCaptor<NouveauCompte> nouveau = ArgumentCaptor.forClass(NouveauCompte.class);
    verify(identite).creerCompte(nouveau.capture());
    assertThat(nouveau.getValue().identite().email()).isEqualTo(EMAIL);
    assertThat(nouveau.getValue().identite().prenom()).isEqualTo("Contact");
    assertThat(nouveau.getValue().identite().nom()).isEqualTo("Baobab Logistique");
    assertThat(nouveau.getValue().motDePasseTemporaire()).isNull();
    assertThat(nouveau.getValue().otpObligatoire()).isFalse();
    assertThat(nouveau.getValue().attributs().fournisseurId()).isEqualTo(FOURNISSEUR_ID);
    verify(identite).assignerRole(COMPTE, Role.FOURNISSEUR);
    verify(identite).envoyerEmailActivation(COMPTE, LIEN);
    ArgumentCaptor<Utilisateur> profil = ArgumentCaptor.forClass(Utilisateur.class);
    verify(events).publierCree(profil.capture());
    assertThat(profil.getValue())
        .isInstanceOfSatisfying(
            Fournisseur.class,
            f -> {
              assertThat(f.id()).isEqualTo(COMPTE);
              assertThat(f.fournisseurId()).isEqualTo(FOURNISSEUR_ID);
              assertThat(f.actif()).isTrue();
              assertThat(f.createdAt()).isEqualTo(MAINTENANT);
            });
  }

  @Test
  void should_supprimer_le_compte_keycloak_when_transaction_annulee() {
    when(identite.compteParEmail(EMAIL)).thenReturn(Optional.empty());
    when(identite.creerCompte(any())).thenReturn(COMPTE);
    service.activer(COMMANDE);
    ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);
    verify(compensation).siAnnulation(action.capture());
    action.getValue().run();
    verify(identite).supprimerCompte(COMPTE);
  }

  @Test
  void should_propager_l_erreur_keycloak_sans_creer_de_profil_when_e_mail_impossible() {
    when(identite.compteParEmail(EMAIL)).thenReturn(Optional.empty());
    when(identite.creerCompte(any())).thenReturn(COMPTE);
    doThrow(new IdentiteIndisponibleException("Erreur Keycloak 500", null))
        .when(identite)
        .envoyerEmailActivation(COMPTE, LIEN);

    assertThatThrownBy(() -> service.activer(COMMANDE))
        .isInstanceOf(IdentiteIndisponibleException.class);
    verify(compensation).siAnnulation(any());
    verify(repository, never()).enregistrer(any());
    verifyNoInteractions(events);
  }

  @Test
  void should_completer_compte_existant_sans_le_recreer_when_attribut_et_mot_de_passe_absents() {
    when(identite.compteParEmail(EMAIL))
        .thenReturn(Optional.of(compte(Set.of(Role.FOURNISSEUR.code()), null)));
    when(identite.motDePasseDefini(COMPTE)).thenReturn(false);
    when(repository.parId(COMPTE)).thenReturn(Optional.empty());

    service.activer(COMMANDE);

    verify(identite, never()).creerCompte(any());
    verify(identite, never()).assignerRole(any(), any());
    verify(identite).rattacherFournisseur(COMPTE, FOURNISSEUR_ID);
    verify(identite).envoyerEmailActivation(COMPTE, LIEN);
    ArgumentCaptor<Utilisateur> profil = ArgumentCaptor.forClass(Utilisateur.class);
    verify(events).publierCree(profil.capture());
    assertThat(profil.getValue().nomComplet()).isEqualTo("Seynabou Gueye");
    assertThat(profil.getValue().attributs().fournisseurId()).isEqualTo(FOURNISSEUR_ID);
  }

  @Test
  void should_attribuer_le_role_when_compte_existant_sans_role_cse() {
    when(identite.compteParEmail(EMAIL)).thenReturn(Optional.of(compte(Set.of(), FOURNISSEUR_ID)));
    when(identite.motDePasseDefini(COMPTE)).thenReturn(true);
    when(repository.parId(COMPTE)).thenReturn(Optional.empty());

    service.activer(COMMANDE);

    verify(identite).assignerRole(COMPTE, Role.FOURNISSEUR);
    verify(identite, never()).rattacherFournisseur(any(), any());
    verify(identite, never()).envoyerEmailActivation(any(), any());
    verify(events).publierCree(any());
  }

  @Test
  void should_ne_rien_faire_when_compte_deja_conforme() {
    when(identite.compteParEmail(EMAIL))
        .thenReturn(Optional.of(compte(Set.of(Role.FOURNISSEUR.code()), FOURNISSEUR_ID)));
    when(identite.motDePasseDefini(COMPTE)).thenReturn(true);
    Utilisateur existant =
        Utilisateurs.creer(
            Role.FOURNISSEUR,
            new Utilisateur.EtatCommun(
                COMPTE,
                new Utilisateur.Identite(EMAIL, "Seynabou", "Gueye", null),
                true,
                false,
                MAINTENANT),
            new AttributsProfil(null, null, null, null, FOURNISSEUR_ID));
    when(repository.parId(COMPTE)).thenReturn(Optional.of(existant));

    service.activer(COMMANDE);

    verify(identite, never()).creerCompte(any());
    verify(identite, never()).assignerRole(any(), any());
    verify(identite, never()).rattacherFournisseur(any(), any());
    verify(identite, never()).envoyerEmailActivation(any(), any());
    verify(repository, never()).enregistrer(any());
    verifyNoInteractions(events);
  }

  @Test
  void should_rattacher_le_profil_local_when_fournisseur_different() {
    UUID ancien = UUID.fromString("f0000000-0000-0000-0000-000000000001");
    when(identite.compteParEmail(EMAIL))
        .thenReturn(Optional.of(compte(Set.of(Role.FOURNISSEUR.code()), ancien)));
    when(identite.motDePasseDefini(COMPTE)).thenReturn(true);
    Utilisateur existant =
        Utilisateurs.creer(
            Role.FOURNISSEUR,
            new Utilisateur.EtatCommun(
                COMPTE,
                new Utilisateur.Identite(EMAIL, "Seynabou", "Gueye", null),
                true,
                false,
                MAINTENANT),
            new AttributsProfil(null, null, null, null, ancien));
    when(repository.parId(COMPTE)).thenReturn(Optional.of(existant));

    service.activer(COMMANDE);

    verify(identite).rattacherFournisseur(COMPTE, FOURNISSEUR_ID);
    ArgumentCaptor<Utilisateur> profil = ArgumentCaptor.forClass(Utilisateur.class);
    verify(events).publierSynchronise(profil.capture());
    assertThat(profil.getValue().attributs().fournisseurId()).isEqualTo(FOURNISSEUR_ID);
  }

  @Test
  void should_ne_jamais_convertir_un_compte_interne_portant_la_meme_adresse() {
    when(identite.compteParEmail(EMAIL))
        .thenReturn(Optional.of(compte(Set.of(Role.RESPONSABLE_ACHATS.code()), null)));

    service.activer(COMMANDE);

    verify(identite, never()).assignerRole(any(), any());
    verify(identite, never()).rattacherFournisseur(any(), any());
    verify(identite, never()).envoyerEmailActivation(any(), any());
    verifyNoInteractions(repository, events);
  }

  @Test
  void should_refuser_une_commande_sans_e_mail_ni_fournisseur() {
    assertThatThrownBy(() -> new ActiverCompteFournisseurCommand(FOURNISSEUR_ID, " ", "X"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ActiverCompteFournisseurCommand(null, EMAIL, "X"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new LienActivation("c", "http://x/", Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ParametresPortailFournisseur(" ", LIEN))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_nommer_le_compte_avec_l_interlocuteur_when_contact_connu() {
    when(identite.compteParEmail(EMAIL)).thenReturn(Optional.empty());
    when(identite.creerCompte(any())).thenReturn(COMPTE);

    service.activer(
        new ActiverCompteFournisseurCommand(
            FOURNISSEUR_ID, EMAIL, "Baobab Logistique", " Ndeye Awa Fall "));

    ArgumentCaptor<NouveauCompte> nouveau = ArgumentCaptor.forClass(NouveauCompte.class);
    verify(identite).creerCompte(nouveau.capture());
    assertThat(nouveau.getValue().identite().prenom()).isEqualTo("Ndeye Awa");
    assertThat(nouveau.getValue().identite().nom()).isEqualTo("Fall");
  }

  @Test
  void should_retirer_les_caracteres_refuses_par_keycloak_when_raison_sociale() {
    when(identite.compteParEmail(EMAIL)).thenReturn(Optional.empty());
    when(identite.creerCompte(any())).thenReturn(COMPTE);

    service.activer(
        new ActiverCompteFournisseurCommand(FOURNISSEUR_ID, EMAIL, "Diop & Fils (BTP) / Dakar"));

    ArgumentCaptor<NouveauCompte> nouveau = ArgumentCaptor.forClass(NouveauCompte.class);
    verify(identite).creerCompte(nouveau.capture());
    assertThat(nouveau.getValue().identite().nom()).isEqualTo("Diop Fils BTP Dakar");
    verify(identite).envoyerEmailActivation(eq(COMPTE), any());
  }

  @Test
  void should_utiliser_un_nom_par_defaut_when_raison_sociale_inexploitable() {
    when(identite.compteParEmail(EMAIL)).thenReturn(Optional.empty());
    when(identite.creerCompte(any())).thenReturn(COMPTE);

    service.activer(new ActiverCompteFournisseurCommand(FOURNISSEUR_ID, EMAIL, "&&"));

    ArgumentCaptor<NouveauCompte> nouveau = ArgumentCaptor.forClass(NouveauCompte.class);
    verify(identite).creerCompte(nouveau.capture());
    assertThat(nouveau.getValue().identite().nom()).isEqualTo("Fournisseur");
  }
}
