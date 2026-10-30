package sn.cse.suivi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.command.CloturerMaintenanceCommande;
import sn.cse.suivi.application.command.PlanifierMaintenanceCommande;
import sn.cse.suivi.application.command.SignalerPanneCommande;
import sn.cse.suivi.application.fake.Memoire;
import sn.cse.suivi.application.query.MaintenanceVue;
import sn.cse.suivi.application.query.PanneVue;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.GravitePanne;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.domain.model.StatutPanne;
import sn.cse.suivi.domain.model.TypeMaintenance;

class MaintenancesPannesServicesTest extends ServiceTestSupport {

  private final GererMaintenancesService maintenances =
      new GererMaintenancesService(
          m.maintenances,
          m.materiels,
          m.affectations,
          m.pannes,
          m.mouvements,
          m.sequences,
          m.chargeur,
          m.vues,
          m.evenements,
          m.parametres,
          m.clock);
  private final ConsulterMaintenancesService consultationMaintenances =
      new ConsulterMaintenancesService(m.maintenances, m.vues);
  private final SignalerPanneService signalement =
      new SignalerPanneService(m.pannes, m.chargeur, m.evenements, m.clock);
  private final ConsulterPannesService consultationPannes =
      new ConsulterPannesService(m.pannes, m.vues);

  private MaintenanceVue planifier(Materiel materiel, UUID panne) {
    return maintenances.planifier(
        new PlanifierMaintenanceCommande(
            materiel.getId(),
            new Maintenance.Planification(
                panne == null ? TypeMaintenance.PREVENTIVE : TypeMaintenance.CORRECTIVE,
                "Vidange",
                Memoire.AUJOURDHUI,
                "Atelier",
                panne),
            rm()));
  }

  private MaintenanceVue cloturer(MaintenanceVue ot, Long heures) {
    return maintenances.cloturer(
        new CloturerMaintenanceCommande(
            ot.maintenance().getId(), new BigDecimal("450000"), "RAS", heures, rm()));
  }

  @Test
  void should_number_work_orders_per_year() {
    Materiel materiel = materiel("PEL-01");

    assertThat(planifier(materiel, null).maintenance().getNumeroOt()).isEqualTo("OT-2026-0001");
    assertThat(planifier(materiel, null).maintenance().getNumeroOt()).isEqualTo("OT-2026-0002");
    assertThat(m.evenements.types()).containsOnly(EventTypes.MAINTENANCE_PLANIFIEE);
  }

  @Test
  void should_put_materiel_back_to_park_after_closing_when_not_affected() {
    Materiel materiel = materiel("PEL-01");
    MaintenanceVue ot = planifier(materiel, null);

    MaintenanceVue enCours = maintenances.demarrer(ot.maintenance().getId(), rm());
    assertThat(enCours.maintenance().getStatut()).isEqualTo(StatutMaintenance.EN_COURS);
    assertThat(m.materiels.findById(materiel.getId()).orElseThrow().getEtat())
        .isEqualTo(EtatMateriel.EN_MAINTENANCE);

    MaintenanceVue cloturee = cloturer(ot, 1450L);

    Materiel remis = m.materiels.findById(materiel.getId()).orElseThrow();
    assertThat(cloturee.maintenance().getStatut()).isEqualTo(StatutMaintenance.CLOTUREE);
    assertThat(remis.getEtat()).isEqualTo(EtatMateriel.DISPONIBLE);
    assertThat(remis.getHeuresFonctionnement()).isEqualTo(1450);
    assertThat(remis.heuresDepuisMaintenance()).isZero();
    assertThat(m.evenements.types())
        .containsSubsequence(
            EventTypes.MAINTENANCE_DEMARREE,
            EventTypes.MATERIEL_ETAT_CHANGE,
            EventTypes.MAINTENANCE_CLOTUREE,
            EventTypes.MATERIEL_ETAT_CHANGE);
  }

  @Test
  void should_return_materiel_to_site_after_closing_when_affected() {
    Chantier chantier = chantier("CH-1");
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, chantier);
    MaintenanceVue ot = planifier(m.materiels.findById(materiel.getId()).orElseThrow(), null);
    assertThat(ot.chantierNom()).isEqualTo("Chantier CH-1");
    maintenances.demarrer(ot.maintenance().getId(), rm());

    cloturer(ot, null);

    assertThat(m.materiels.findById(materiel.getId()).orElseThrow().getEtat())
        .isEqualTo(EtatMateriel.AFFECTE);
  }

  @Test
  void should_keep_materiel_immobilised_while_another_order_runs() {
    Materiel materiel = materiel("PEL-01");
    MaintenanceVue premier = planifier(materiel, null);
    MaintenanceVue second = planifier(materiel, null);
    maintenances.demarrer(premier.maintenance().getId(), rm());
    maintenances.demarrer(second.maintenance().getId(), rm());

    cloturer(premier, null);

    assertThat(m.materiels.findById(materiel.getId()).orElseThrow().getEtat())
        .isEqualTo(EtatMateriel.EN_MAINTENANCE);
    assertThatThrownBy(() -> cloturer(premier, null)).isInstanceOf(ConflictException.class);
  }

  @Test
  void should_take_charge_of_panne_and_resolve_it_on_closing() {
    Chantier chantier = chantier("CH-1");
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, chantier);
    PanneVue panne =
        signalement.signaler(
            new SignalerPanneCommande(
                materiel.getId(), GravitePanne.MAJEURE, "Fuite", chef(chantier.getId())));
    assertThat(panne.chantierNom()).isEqualTo("Chantier CH-1");
    assertThat(panne.panne().getSignaleParNom()).isEqualTo("Mamadou Diop");

    MaintenanceVue ot =
        planifier(m.materiels.findById(materiel.getId()).orElseThrow(), panne.panne().getId());
    assertThat(m.pannes.findById(panne.panne().getId()).orElseThrow().getStatut())
        .isEqualTo(StatutPanne.PRISE_EN_CHARGE);
    assertThatThrownBy(
            () ->
                planifier(
                    m.materiels.findById(materiel.getId()).orElseThrow(), panne.panne().getId()))
        .isInstanceOf(ConflictException.class);
    maintenances.demarrer(ot.maintenance().getId(), rm());
    cloturer(ot, null);

    assertThat(m.pannes.findById(panne.panne().getId()).orElseThrow().getStatut())
        .isEqualTo(StatutPanne.RESOLUE);
    assertThat(m.evenements.types()).contains("PANNE_SIGNALEE");
  }

  @Test
  void should_forbid_chef_to_report_panne_outside_his_sites() {
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, chantier("CH-1"));

    assertThatThrownBy(
            () ->
                signalement.signaler(
                    new SignalerPanneCommande(
                        materiel.getId(), GravitePanne.MINEURE, "Bruit", chef(UUID.randomUUID()))))
        .isInstanceOf(ForbiddenOperationException.class);
    PanneVue parRm =
        signalement.signaler(
            new SignalerPanneCommande(materiel("PEL-02").getId(), GravitePanne.MINEURE, "x", rm()));
    assertThat(parRm.chantierNom()).isNull();
  }

  @Test
  void should_filter_lists_by_perimeter() {
    Chantier sien = chantier("CH-1");
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, sien);
    Materiel affecte = m.materiels.findById(materiel.getId()).orElseThrow();
    planifier(affecte, null);
    planifier(materiel("PEL-02"), null);
    signalement.signaler(
        new SignalerPanneCommande(affecte.getId(), GravitePanne.MAJEURE, "x", rm()));
    PageQuery page = new PageQuery(0, 20, "datePlanifiee", false);

    assertThat(
            consultationMaintenances
                .rechercher(null, null, null, page, chef(sien.getId()))
                .totalElements())
        .isEqualTo(1);
    assertThat(
            consultationMaintenances
                .rechercher(
                    StatutMaintenance.PLANIFIEE, TypeMaintenance.PREVENTIVE, null, page, rm())
                .totalElements())
        .isEqualTo(2);
    assertThat(
            consultationPannes
                .rechercher(StatutPanne.SIGNALEE, sien.getId(), null, page, chef(sien.getId()))
                .content())
        .hasSize(1)
        .allMatch(p -> "PEL-01".equals(p.materielCode()));
  }
}
