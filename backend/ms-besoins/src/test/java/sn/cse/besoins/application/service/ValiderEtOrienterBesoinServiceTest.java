package sn.cse.besoins.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sn.cse.besoins.Fixtures.MAINTENANT;
import static sn.cse.besoins.Fixtures.USER_CHEF;
import static sn.cse.besoins.Fixtures.USER_RM;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionOperations;
import sn.cse.besoins.Fixtures;
import sn.cse.besoins.application.command.OrienterBesoinCommand;
import sn.cse.besoins.application.command.RejeterBesoinCommand;
import sn.cse.besoins.application.command.ValiderBesoinCommand;
import sn.cse.besoins.application.exception.ServiceSuiviIndisponibleException;
import sn.cse.besoins.application.port.out.AffectationMaterielPort;
import sn.cse.besoins.application.port.out.AffectationMaterielPort.DemandeAffectation;
import sn.cse.besoins.application.port.out.AffectationMaterielPort.ResultatAffectation;
import sn.cse.besoins.application.port.out.DisponibiliteMaterielPort;
import sn.cse.besoins.application.port.out.DisponibiliteMaterielPort.DemandeDisponibilite;
import sn.cse.besoins.application.port.out.DisponibiliteMaterielPort.Disponibilite;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.domain.exception.ValidationNonAutoriseeException;
import sn.cse.besoins.domain.model.ActionHistorique;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.HistoriqueBesoin;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.besoins.domain.model.TypeEvenementBesoin;
import sn.cse.besoins.domain.validation.ChaineValidation;
import sn.cse.besoins.domain.validation.CritereDeclenchement;
import sn.cse.besoins.domain.validation.EtapeValidation;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.security.Roles;

class ValiderEtOrienterBesoinServiceTest {

  private static final UUID M1 = UUID.randomUUID();
  private static final UUID M2 = UUID.randomUUID();
  private static final UUID M3 = UUID.randomUUID();

  private final Fakes.Depot depot = new Fakes.Depot();
  private final Fakes.Publication publication = new Fakes.Publication();
  private final DisponibiliteMaterielPort disponibilites = mock(DisponibiliteMaterielPort.class);
  private final AffectationMaterielPort affectations = mock(AffectationMaterielPort.class);
  private final Clock clock = Clock.fixed(MAINTENANT, ZoneOffset.UTC);
  private EnregistrementBesoin enregistrement;
  private OrienterBesoinService orientation;

  @BeforeEach
  void preparer() {
    enregistrement = new EnregistrementBesoin(depot, publication);
    orientation =
        new OrienterBesoinService(
            enregistrement,
            disponibilites,
            affectations,
            TransactionOperations.withoutTransaction(),
            clock);
  }

  private ValiderBesoinService validation(ChaineValidation chaine) {
    return new ValiderBesoinService(
        enregistrement, chaine, orientation, TransactionOperations.withoutTransaction(), clock);
  }

  private BesoinMateriel enAttente(int quantite) {
    return depot.ajouter(Fixtures.besoinEnAttente(quantite));
  }

  private void disponibles(UUID... ids) {
    when(disponibilites.verifier(any())).thenReturn(new Disponibilite(ids.length, List.of(ids)));
  }

  private void affectationsAcceptees() {
    when(affectations.affecter(any()))
        .thenReturn(new ResultatAffectation.Affecte(UUID.randomUUID()));
  }

  @Test
  void should_affecter_directement_when_parc_suffisant_R01() {
    BesoinMateriel b = enAttente(2);
    disponibles(M1, M2, M3);
    affectationsAcceptees();

    BesoinVue vue =
        validation(ChaineValidation.parDefaut())
            .valider(new ValiderBesoinCommand(b.id(), "OK", List.of(M3), USER_RM));

    assertThat(vue.statut()).isEqualTo(StatutBesoin.AFFECTE_DIRECT);
    assertThat(vue.orientationEnAttente()).isFalse();
    assertThat(vue.materielIdsAffectes()).containsExactly(M3, M1);
    assertThat(publication.types)
        .containsExactly(TypeEvenementBesoin.VALIDE, TypeEvenementBesoin.AFFECTE_DIRECT);

    ArgumentCaptor<DemandeDisponibilite> dispo =
        ArgumentCaptor.forClass(DemandeDisponibilite.class);
    verify(disponibilites).verifier(dispo.capture());
    assertThat(dispo.getValue().quantite()).isEqualTo(2);
    assertThat(dispo.getValue().du()).isEqualTo(b.description().dateBesoin());
    assertThat(dispo.getValue().au()).isEqualTo(b.description().dateBesoin().plusDays(30));
    ArgumentCaptor<DemandeAffectation> demande = ArgumentCaptor.forClass(DemandeAffectation.class);
    verify(affectations, times(2)).affecter(demande.capture());
    assertThat(demande.getAllValues().get(0).besoinId()).isEqualTo(b.id());
    assertThat(demande.getAllValues().get(0).dateFinPrevue())
        .isEqualTo(b.description().dateBesoin().plusDays(30));
  }

  @Test
  void should_passer_en_achat_when_parc_insuffisant() {
    BesoinMateriel b = enAttente(3);
    disponibles(M1);

    BesoinVue vue =
        validation(ChaineValidation.parDefaut())
            .valider(new ValiderBesoinCommand(b.id(), null, null, USER_RM));

    assertThat(vue.statut()).isEqualTo(StatutBesoin.EN_ACHAT);
    assertThat(publication.types)
        .containsExactly(TypeEvenementBesoin.VALIDE, TypeEvenementBesoin.EN_ACHAT);
    verify(affectations, never()).affecter(any());
  }

  @Test
  void should_rester_valide_en_attente_when_ms_suivi_indisponible_puis_relancer() {
    BesoinMateriel b = enAttente(1);
    when(disponibilites.verifier(any())).thenThrow(new ServiceSuiviIndisponibleException("test"));

    BesoinVue vue =
        validation(ChaineValidation.parDefaut())
            .valider(new ValiderBesoinCommand(b.id(), null, null, USER_RM));

    assertThat(vue.statut()).isEqualTo(StatutBesoin.VALIDE);
    assertThat(vue.orientationEnAttente()).isTrue();
    assertThat(depot.trouver(b.id()).orElseThrow().historique())
        .extracting(HistoriqueBesoin::action)
        .endsWith(ActionHistorique.VALIDATION, ActionHistorique.ORIENTATION_EN_ATTENTE);

    // Relance : ms-suivi est revenu.
    org.mockito.Mockito.reset(disponibilites);
    disponibles(M1);
    affectationsAcceptees();
    BesoinVue relance = orientation.orienter(new OrienterBesoinCommand(b.id(), null, USER_RM));

    assertThat(relance.statut()).isEqualTo(StatutBesoin.AFFECTE_DIRECT);
    assertThat(publication.types)
        .containsExactly(TypeEvenementBesoin.VALIDE, TypeEvenementBesoin.AFFECTE_DIRECT);
  }

  @Test
  void should_conserver_les_affectations_partielles_when_ms_suivi_tombe_pendant_l_affectation() {
    BesoinMateriel b = enAttente(2);
    disponibles(M1, M2);
    when(affectations.affecter(any()))
        .thenReturn(new ResultatAffectation.Affecte(UUID.randomUUID()))
        .thenThrow(new ServiceSuiviIndisponibleException("test"));

    BesoinVue vue =
        validation(ChaineValidation.parDefaut())
            .valider(new ValiderBesoinCommand(b.id(), null, null, USER_RM));

    assertThat(vue.statut()).isEqualTo(StatutBesoin.VALIDE);
    assertThat(vue.orientationEnAttente()).isTrue();
    assertThat(vue.materielIdsAffectes()).containsExactly(M1);

    // Relance : seul le matériel manquant est demandé.
    org.mockito.Mockito.reset(disponibilites, affectations);
    disponibles(M1, M2);
    affectationsAcceptees();
    BesoinVue relance = orientation.orienter(new OrienterBesoinCommand(b.id(), List.of(), USER_RM));

    assertThat(relance.statut()).isEqualTo(StatutBesoin.AFFECTE_DIRECT);
    assertThat(relance.materielIdsAffectes()).containsExactly(M1, M2);
    ArgumentCaptor<DemandeDisponibilite> dispo =
        ArgumentCaptor.forClass(DemandeDisponibilite.class);
    verify(disponibilites).verifier(dispo.capture());
    assertThat(dispo.getValue().quantite()).isEqualTo(1);
    verify(affectations, times(1)).affecter(any());
  }

  @Test
  void should_essayer_les_autres_materiels_en_cas_de_conflit_puis_suspendre() {
    BesoinMateriel b = enAttente(2);
    disponibles(M1, M2, M3);
    when(affectations.affecter(any()))
        .thenReturn(new ResultatAffectation.Refuse("CONFLIT_AFFECTATION"))
        .thenReturn(new ResultatAffectation.Affecte(UUID.randomUUID()))
        .thenReturn(new ResultatAffectation.Refuse("CONFLIT_AFFECTATION"));

    BesoinVue vue =
        validation(ChaineValidation.parDefaut())
            .valider(new ValiderBesoinCommand(b.id(), null, null, USER_RM));

    assertThat(vue.statut()).isEqualTo(StatutBesoin.VALIDE);
    assertThat(vue.orientationEnAttente()).isTrue();
    assertThat(vue.materielIdsAffectes()).containsExactly(M2);
    verify(affectations, times(3)).affecter(any());
  }

  @Test
  void should_refuser_la_validation_when_pas_responsable_materiel_R01() {
    BesoinMateriel b = enAttente(1);
    assertThatThrownBy(
            () ->
                validation(ChaineValidation.parDefaut())
                    .valider(new ValiderBesoinCommand(b.id(), null, null, USER_CHEF)))
        .isInstanceOf(ValidationNonAutoriseeException.class);
    assertThat(depot.statut(b.id())).isEqualTo(StatutBesoin.EN_ATTENTE);
    verify(disponibilites, never()).verifier(any());
  }

  @Test
  void should_ne_pas_orienter_apres_une_etape_intermediaire() {
    ChaineValidation chaine =
        new ChaineValidation(
            List.of(
                new EtapeValidation("RM", Roles.RESPONSABLE_MATERIEL, null),
                new EtapeValidation("DP", "DIRECTEUR_PROJET", new CritereDeclenchement(1, null))));
    BesoinMateriel b = enAttente(1);

    BesoinVue vue =
        validation(chaine).valider(new ValiderBesoinCommand(b.id(), null, null, USER_RM));

    assertThat(vue.statut()).isEqualTo(StatutBesoin.EN_ATTENTE);
    verify(disponibilites, never()).verifier(any());
  }

  @Test
  void should_refuser_la_relance_d_un_besoin_sans_orientation_en_attente() {
    BesoinMateriel b = enAttente(1);
    assertThatThrownBy(() -> orientation.orienter(new OrienterBesoinCommand(b.id(), null, USER_RM)))
        .isInstanceOf(ConflictException.class)
        .hasFieldOrPropertyWithValue("code", "ORIENTATION_NON_REQUISE");
    assertThatThrownBy(
            () -> orientation.orienter(new OrienterBesoinCommand(UUID.randomUUID(), null, USER_RM)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasFieldOrPropertyWithValue("code", "BESOIN_INTROUVABLE");
  }

  @Test
  void should_rejeter_avec_motif_R01_R08() {
    RejeterBesoinService rejet =
        new RejeterBesoinService(enregistrement, ChaineValidation.parDefaut(), clock);
    BesoinMateriel b = enAttente(1);

    assertThatThrownBy(() -> rejet.rejeter(new RejeterBesoinCommand(b.id(), " ", USER_RM)))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", "MOTIF_REJET_OBLIGATOIRE");
    assertThatThrownBy(() -> rejet.rejeter(new RejeterBesoinCommand(b.id(), "Doublon", USER_CHEF)))
        .isInstanceOf(ValidationNonAutoriseeException.class);

    BesoinVue vue = rejet.rejeter(new RejeterBesoinCommand(b.id(), "Doublon", USER_RM));
    assertThat(vue.statut()).isEqualTo(StatutBesoin.REJETE);
    assertThat(vue.motifRejet()).isEqualTo("Doublon");
    assertThat(vue.validateurNom()).isEqualTo("Ibrahima Fall");
    assertThat(publication.types).containsExactly(TypeEvenementBesoin.REJETE);
    assertThat(Fixtures.description(1, Priorite.NORMAL).quantite()).isEqualTo(1);
  }
}
