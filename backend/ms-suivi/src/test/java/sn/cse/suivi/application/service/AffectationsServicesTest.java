package sn.cse.suivi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.command.AffecterMaterielCommande;
import sn.cse.suivi.application.command.ConfirmerReceptionCommande;
import sn.cse.suivi.application.command.LibererMaterielCommande;
import sn.cse.suivi.application.fake.Memoire;
import sn.cse.suivi.application.query.AffectationVue;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.StatutAffectation;
import sn.cse.suivi.domain.model.TypeMouvement;

class AffectationsServicesTest extends ServiceTestSupport {

  private static final LocalDate J = Memoire.AUJOURDHUI;

  private final AffecterMaterielService affectation =
      new AffecterMaterielService(
          m.affectations, m.materiels, m.mouvements, m.chargeur, m.vues, m.evenements, m.clock);
  private final ConfirmerReceptionService reception =
      new ConfirmerReceptionService(
          m.affectations, m.mouvements, m.chargeur, m.vues, m.evenements, m.clock);
  private final LibererMaterielService liberation =
      new LibererMaterielService(
          m.affectations, m.materiels, m.mouvements, m.chargeur, m.vues, m.evenements, m.clock);
  private final ConsulterAffectationsService consultation =
      new ConsulterAffectationsService(m.affectations, m.vues, m.parametres, m.clock);

  private AffectationVue affecter(
      Materiel materiel, Chantier chantier, LocalDate debut, int jours) {
    return affectation.affecter(
        new AffecterMaterielCommande(
            materiel.getId(), chantier.getId(), null, debut, debut.plusDays(jours), rm()));
  }

  @Test
  void should_affect_available_materiel_with_events_and_movement_R04() {
    Chantier chantier = chantier("CH-1");
    Materiel materiel = materiel("PEL-01");
    UUID besoin = UUID.randomUUID();

    AffectationVue vue =
        affectation.affecter(
            new AffecterMaterielCommande(
                materiel.getId(), chantier.getId(), besoin, J, J.plusDays(30), rm()));

    assertThat(vue.affectation().getStatut()).isEqualTo(StatutAffectation.EN_ATTENTE_RECEPTION);
    assertThat(vue.affectation().getBesoinId()).isEqualTo(besoin);
    assertThat(vue.chantierNom()).isEqualTo("Chantier CH-1");
    assertThat(vue.categorie()).isEqualTo(CategorieMateriel.MATERIEL_ROULANT);
    assertThat(m.materiels.findById(materiel.getId()).orElseThrow().getEtat())
        .isEqualTo(EtatMateriel.AFFECTE);
    assertThat(m.evenements.types())
        .containsExactly(EventTypes.MATERIEL_AFFECTE, EventTypes.MATERIEL_ETAT_CHANGE);
    assertThat(m.mouvements.journal)
        .extracting(mv -> mv.type())
        .containsExactly(TypeMouvement.AFFECTATION);
  }

  @Test
  void should_return_conflict_when_periods_overlap() {
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, chantier("CH-1"), J, 30);

    assertThatThrownBy(() -> affecter(materiel, chantier("CH-2"), J.plusDays(15), 30))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo("CONFLIT_AFFECTATION");
  }

  @Test
  void should_refuse_affected_materiel_outside_overlap_R04() {
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, chantier("CH-1"), J, 30);

    assertThatThrownBy(() -> affecter(materiel, chantier("CH-2"), J.plusDays(60), 30))
        .extracting("code")
        .isEqualTo("MATERIEL_NON_DISPONIBLE");
  }

  @Test
  void should_return_not_found_for_unknown_materiel_or_chantier() {
    Chantier chantier = chantier("CH-1");

    assertThatThrownBy(
            () ->
                affectation.affecter(
                    new AffecterMaterielCommande(
                        UUID.randomUUID(), chantier.getId(), null, J, J, rm())))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(
            () ->
                affectation.affecter(
                    new AffecterMaterielCommande(
                        materiel("PEL-01").getId(), UUID.randomUUID(), null, J, J, rm())))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_let_chef_confirm_reception_only_on_his_chantier() {
    Chantier chantier = chantier("CH-1");
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, chantier, J, 30);

    assertThatThrownBy(
            () ->
                reception.confirmer(
                    new ConfirmerReceptionCommande(
                        materiel.getId(), null, null, chef(UUID.randomUUID()))))
        .isInstanceOf(ForbiddenOperationException.class);

    AffectationVue vue =
        reception.confirmer(
            new ConfirmerReceptionCommande(materiel.getId(), null, "RAS", chef(chantier.getId())));

    assertThat(vue.affectation().getStatut()).isEqualTo(StatutAffectation.EN_COURS);
    assertThat(vue.affectation().getDateReception()).isEqualTo(m.maintenant());
    assertThat(m.evenements.types()).contains(EventTypes.RECEPTION_CONFIRMEE);
    assertThatThrownBy(
            () ->
                reception.confirmer(
                    new ConfirmerReceptionCommande(
                        materiel.getId(), vue.affectation().getId(), null, rm())))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void should_reject_reception_without_active_or_matching_affectation() {
    Materiel libre = materiel("PEL-01");
    Materiel autre = materiel("PEL-02");
    AffectationVue vue = affecter(autre, chantier("CH-1"), J, 10);

    assertThatThrownBy(
            () ->
                reception.confirmer(
                    new ConfirmerReceptionCommande(libre.getId(), null, null, rm())))
        .extracting("code")
        .isEqualTo("AUCUNE_AFFECTATION_ACTIVE");
    assertThatThrownBy(
            () ->
                reception.confirmer(
                    new ConfirmerReceptionCommande(
                        libre.getId(), vue.affectation().getId(), null, rm())))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_release_materiel_and_close_affectation() {
    Chantier chantier = chantier("CH-1");
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, chantier, J.minusDays(10), 30);

    AffectationVue vue =
        liberation.liberer(new LibererMaterielCommande(materiel.getId(), null, "Fin", rm()));

    assertThat(vue.affectation().getStatut()).isEqualTo(StatutAffectation.TERMINEE);
    assertThat(vue.affectation().getDateFinReelle()).isEqualTo(J);
    Materiel libere = m.materiels.findById(materiel.getId()).orElseThrow();
    assertThat(libere.getEtat()).isEqualTo(EtatMateriel.DISPONIBLE);
    assertThat(libere.getChantierActuelId()).isNull();
    assertThat(m.evenements.types()).contains(EventTypes.AFFECTATION_TERMINEE);
    assertThatThrownBy(
            () ->
                liberation.liberer(new LibererMaterielCommande(materiel.getId(), null, null, rm())))
        .extracting("code")
        .isEqualTo("AUCUNE_AFFECTATION_ACTIVE");
  }

  @Test
  void should_refuse_future_release_date() {
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, chantier("CH-1"), J, 30);

    assertThatThrownBy(
            () ->
                liberation.liberer(
                    new LibererMaterielCommande(materiel.getId(), J.plusDays(1), null, rm())))
        .extracting("code")
        .isEqualTo("DATE_FIN_INVALIDE");
  }

  @Test
  void should_list_and_plan_within_perimeter() {
    Chantier sien = chantier("CH-1");
    Chantier autre = chantier("CH-2");
    affecter(materiel("PEL-01"), sien, J, 30);
    affecter(materiel("PEL-02"), autre, J.plusDays(200), 30);
    PageQuery page = new PageQuery(0, 20, "dateDebut", false);

    assertThat(consultation.rechercher(null, null, null, page, chef(sien.getId())).totalElements())
        .isEqualTo(1);
    assertThat(
            consultation
                .rechercher(null, null, StatutAffectation.EN_ATTENTE_RECEPTION, page, rm())
                .totalElements())
        .isEqualTo(2);
    assertThat(consultation.planning(null, null, null, null, rm())).hasSize(1);
    assertThat(consultation.planning(autre.getId(), null, J, J.plusDays(300), rm())).hasSize(1);
    assertThatThrownBy(
            () -> consultation.planning(autre.getId(), null, null, null, chef(sien.getId())))
        .isInstanceOf(ForbiddenOperationException.class);
  }

  @Test
  void should_validate_planning_period() {
    assertThatThrownBy(() -> consultation.planning(null, null, J, J.minusDays(1), rm()))
        .isInstanceOf(InvalidRequestException.class)
        .extracting("code")
        .isEqualTo("PERIODE_INVALIDE");
    assertThatThrownBy(() -> consultation.planning(null, null, J, J.plusDays(400), rm()))
        .isInstanceOf(InvalidRequestException.class)
        .extracting("code")
        .isEqualTo("PERIODE_TROP_LONGUE");
  }
}
