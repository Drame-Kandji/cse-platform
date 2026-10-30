package sn.cse.suivi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.suivi.domain.model.ContexteMateriel;
import sn.cse.suivi.domain.model.GravitePanne;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Panne;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.domain.model.StatutPanne;
import sn.cse.suivi.domain.model.TypeMaintenance;

class MaintenancePanneTest {

  private static final Instant MAINTENANT = Instant.parse("2026-10-30T09:00:00Z");

  private static Maintenance.Planification planification(UUID panne) {
    return new Maintenance.Planification(
        TypeMaintenance.PREVENTIVE, "Vidange", Fixtures.AUJOURDHUI, "Atelier", panne);
  }

  private static Materiel reforme() {
    Materiel m = Fixtures.materiel();
    m.demanderReforme("Usure");
    m.deciderReforme(true, new ContexteMateriel(false, false));
    return m;
  }

  @Test
  void should_follow_maintenance_lifecycle() {
    Materiel materiel = Fixtures.materiel();
    materiel.affecter(Fixtures.chantier());

    Maintenance m = Maintenance.planifier("OT-2026-0001", materiel, planification(null));
    assertThat(m.getStatut()).isEqualTo(StatutMaintenance.PLANIFIEE);
    assertThat(m.getChantierId()).isEqualTo(materiel.getChantierActuelId());
    assertThatThrownBy(() -> m.cloturer(Fixtures.AUJOURDHUI, BigDecimal.TEN, null, null))
        .isInstanceOf(ConflictException.class);

    m.demarrer(Fixtures.AUJOURDHUI);
    assertThat(m.getStatut()).isEqualTo(StatutMaintenance.EN_COURS);
    assertThat(m.getDateDebut()).isEqualTo(Fixtures.AUJOURDHUI);
    assertThatThrownBy(() -> m.demarrer(Fixtures.AUJOURDHUI)).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> m.cloturer(Fixtures.AUJOURDHUI, BigDecimal.valueOf(-1), null, null))
        .isInstanceOf(BusinessRuleException.class);

    m.cloturer(Fixtures.AUJOURDHUI.plusDays(1), new BigDecimal("685000"), "RAS", 1010L);
    assertThat(m.getStatut()).isEqualTo(StatutMaintenance.CLOTUREE);
    assertThat(m.getCout()).isEqualByComparingTo("685000");
    assertThat(m.getDateFin()).isEqualTo(Fixtures.AUJOURDHUI.plusDays(1));
    assertThat(m.snapshot().heuresCompteur()).isEqualTo(1010L);
    assertThat(Maintenance.reconstituer(m.snapshot()).snapshot()).isEqualTo(m.snapshot());
  }

  @Test
  void should_not_plan_maintenance_on_reformed_materiel_R13() {
    assertThatThrownBy(() -> Maintenance.planifier("OT-2026-0002", reforme(), planification(null)))
        .extracting("code")
        .isEqualTo("MATERIEL_REFORME");
  }

  @Test
  void should_link_panne_to_maintenance_then_resolve() {
    Materiel materiel = Fixtures.materiel();
    Panne panne =
        Panne.signaler(
            materiel, GravitePanne.MAJEURE, "Fuite", UUID.randomUUID(), "Diop", MAINTENANT);
    assertThat(panne.getStatut()).isEqualTo(StatutPanne.SIGNALEE);
    UUID maintenance = UUID.randomUUID();

    assertThatThrownBy(() -> panne.prendreEnCharge(maintenance, UUID.randomUUID()))
        .extracting("code")
        .isEqualTo("PANNE_INCOHERENTE");
    assertThatThrownBy(panne::resoudre).isInstanceOf(ConflictException.class);

    panne.prendreEnCharge(maintenance, materiel.getId());
    assertThat(panne.getStatut()).isEqualTo(StatutPanne.PRISE_EN_CHARGE);
    assertThat(panne.snapshot().maintenanceId()).isEqualTo(maintenance);
    assertThatThrownBy(() -> panne.prendreEnCharge(maintenance, materiel.getId()))
        .isInstanceOf(ConflictException.class);

    panne.resoudre();
    assertThat(panne.getStatut()).isEqualTo(StatutPanne.RESOLUE);
    assertThat(Panne.reconstituer(panne.snapshot()).getSignaleParNom()).isEqualTo("Diop");
  }

  @Test
  void should_not_report_panne_on_reformed_materiel_R13() {
    assertThatThrownBy(
            () -> Panne.signaler(reforme(), GravitePanne.MINEURE, "x", null, "Système", MAINTENANT))
        .extracting("code")
        .isEqualTo("MATERIEL_REFORME");
  }
}
