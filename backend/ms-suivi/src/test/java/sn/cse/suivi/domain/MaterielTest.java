package sn.cse.suivi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.ContexteMateriel;
import sn.cse.suivi.domain.model.DescriptionMateriel;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.TypeEngin;

class MaterielTest {

  private static final ContexteMateriel LIBRE = new ContexteMateriel(false, false);

  private static Materiel versionne(Materiel m) {
    Materiel.Snapshot s = m.snapshot();
    return Materiel.reconstituer(
        new Materiel.Snapshot(
            s.id(),
            s.description(),
            s.etat(),
            s.heuresFonctionnement(),
            s.heuresDerniereMaintenance(),
            s.dateReferenceMaintenance(),
            s.seuilMaintenanceHeures(),
            s.seuilMaintenanceJours(),
            s.dateDerniereAlerteMaintenance(),
            s.chantierActuelId(),
            s.position(),
            s.fournisseurId(),
            s.contratId(),
            s.reformeDemandee(),
            s.motifReforme(),
            4L));
  }

  @Test
  void should_register_available_with_thresholds_of_its_type() {
    TypeEngin type = Fixtures.typePelle(500, 180);

    Materiel m = Fixtures.materiel(type);

    assertThat(m.getEtat()).isEqualTo(EtatMateriel.DISPONIBLE);
    assertThat(m.getSeuilMaintenanceHeures()).isEqualTo(500);
    assertThat(m.heuresDepuisMaintenance()).isZero();
    assertThat(m.prochaineMaintenance()).isEqualTo(Fixtures.AUJOURDHUI.minusDays(30).plusDays(180));
    assertThat(m.estMaintenanceDue(Fixtures.AUJOURDHUI)).isFalse();
  }

  @Test
  void should_refuse_inactive_or_incompatible_type() {
    TypeEngin inactif =
        TypeEngin.creer(
            new DonneesTypeEngin("X", "X", CategorieMateriel.MATERIEL_ROULANT, null, null, false));
    TypeEngin informatique =
        TypeEngin.creer(
            new DonneesTypeEngin(
                "PC", "PC", CategorieMateriel.EQUIPEMENT_INFORMATIQUE, null, 365, true));

    assertThatThrownBy(() -> Fixtures.materiel(inactif))
        .extracting("code")
        .isEqualTo("TYPE_ENGIN_INACTIF");
    assertThatThrownBy(() -> Fixtures.materiel(informatique))
        .extracting("code")
        .isEqualTo("TYPE_ENGIN_INCOMPATIBLE");
    assertThatThrownBy(
            () ->
                Materiel.enregistrer(
                    Fixtures.description("X", null),
                    null,
                    -1,
                    Materiel.Origine.AUCUNE,
                    Fixtures.AUJOURDHUI))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_affect_available_materiel_and_take_site_position_R04() {
    Materiel m = Fixtures.materiel();
    Chantier c = Fixtures.chantier();

    m.affecter(c);

    assertThat(m.getEtat()).isEqualTo(EtatMateriel.AFFECTE);
    assertThat(m.getChantierActuelId()).isEqualTo(c.getId());
    assertThat(m.getPosition()).isEqualTo(c.getCoordonnees());
  }

  @Test
  void should_refuse_affectation_when_not_available_R04() {
    Materiel affecte = Fixtures.materiel();
    affecte.affecter(Fixtures.chantier());
    Materiel enMaintenance = Fixtures.materiel();
    enMaintenance.mettreEnMaintenance();
    Materiel enInstance = Fixtures.materiel();
    enInstance.demanderReforme("Usure");

    for (Materiel m : new Materiel[] {affecte, enMaintenance, enInstance}) {
      assertThatThrownBy(() -> m.affecter(Fixtures.chantier()))
          .isInstanceOf(BusinessRuleException.class)
          .extracting("code")
          .isEqualTo("MATERIEL_NON_DISPONIBLE");
    }
  }

  @Test
  void should_return_to_park_when_released_unless_in_maintenance() {
    Materiel m = Fixtures.materiel();
    m.affecter(Fixtures.chantier());
    m.liberer();
    assertThat(m.getEtat()).isEqualTo(EtatMateriel.DISPONIBLE);
    assertThat(m.getChantierActuelId()).isNull();

    Materiel immobilise = Fixtures.materiel();
    immobilise.affecter(Fixtures.chantier());
    immobilise.mettreEnMaintenance();
    immobilise.liberer();
    assertThat(immobilise.getEtat()).isEqualTo(EtatMateriel.EN_MAINTENANCE);
  }

  @Test
  void should_return_to_site_after_maintenance_when_affectation_in_progress() {
    Materiel m = Fixtures.materiel();
    m.affecter(Fixtures.chantier());
    m.mettreEnMaintenance();
    m.mettreEnMaintenance();
    assertThat(m.getEtat()).isEqualTo(EtatMateriel.EN_MAINTENANCE);

    m.remettreEnService(true);
    assertThat(m.getEtat()).isEqualTo(EtatMateriel.AFFECTE);

    m.remettreEnService(false);
    assertThat(m.getEtat()).isEqualTo(EtatMateriel.AFFECTE);
  }

  @Test
  void should_allow_only_maintenance_or_service_in_manual_change() {
    Materiel m = Fixtures.materiel();

    assertThatThrownBy(() -> m.changerEtat(EtatMateriel.REFORME, LIBRE))
        .extracting("code")
        .isEqualTo("ETAT_CIBLE_INTERDIT");
    assertThatThrownBy(() -> m.changerEtat(EtatMateriel.DISPONIBLE, LIBRE))
        .isInstanceOf(ConflictException.class);

    m.changerEtat(EtatMateriel.EN_MAINTENANCE, LIBRE);
    assertThat(m.getEtat()).isEqualTo(EtatMateriel.EN_MAINTENANCE);
    assertThatThrownBy(
            () -> m.changerEtat(EtatMateriel.DISPONIBLE, new ContexteMateriel(false, true)))
        .extracting("code")
        .isEqualTo("MAINTENANCE_EN_COURS");

    m.changerEtat(EtatMateriel.DISPONIBLE, new ContexteMateriel(true, false));
    assertThat(m.getEtat()).isEqualTo(EtatMateriel.AFFECTE);
    assertThatThrownBy(() -> m.changerEtat(EtatMateriel.DISPONIBLE, LIBRE))
        .extracting("code")
        .isEqualTo("AFFECTATION_EN_COURS");
  }

  @Test
  void should_refuse_hour_reading_lower_than_counter() {
    Materiel m = Fixtures.materiel();

    m.releverHeures(1500);
    assertThat(m.getHeuresFonctionnement()).isEqualTo(1500);
    assertThat(m.heuresDepuisMaintenance()).isEqualTo(500);
    assertThatThrownBy(() -> m.releverHeures(1400))
        .extracting("code")
        .isEqualTo("RELEVE_HEURES_INVALIDE");
  }

  @Test
  void should_detect_preventive_maintenance_due_by_hours_or_days() {
    Materiel heures = Fixtures.materiel();
    heures.releverHeures(1500);
    assertThat(heures.estMaintenanceDue(Fixtures.AUJOURDHUI)).isTrue();

    Materiel jours = Fixtures.materiel();
    assertThat(jours.estMaintenanceDue(Fixtures.AUJOURDHUI.plusDays(150))).isTrue();

    heures.enregistrerMaintenance(Fixtures.AUJOURDHUI);
    assertThat(heures.heuresDepuisMaintenance()).isZero();
    assertThat(heures.estMaintenanceDue(Fixtures.AUJOURDHUI)).isFalse();
    assertThat(heures.prochaineMaintenance()).isEqualTo(Fixtures.AUJOURDHUI.plusDays(180));
  }

  @Test
  void should_not_repeat_maintenance_alert_before_reminder_delay() {
    Materiel m = Fixtures.materiel();
    m.releverHeures(1600);

    assertThat(m.doitEtreAlerte(Fixtures.AUJOURDHUI, 7)).isTrue();
    m.marquerAlerteMaintenance(Fixtures.AUJOURDHUI);
    assertThat(m.doitEtreAlerte(Fixtures.AUJOURDHUI.plusDays(6), 7)).isFalse();
    assertThat(m.doitEtreAlerte(Fixtures.AUJOURDHUI.plusDays(7), 7)).isTrue();
  }

  @Test
  void should_never_flag_reformed_materiel_for_maintenance() {
    Materiel m = Fixtures.materiel();
    m.releverHeures(5000);
    m.demanderReforme("Usure");
    m.deciderReforme(true, LIBRE);

    assertThat(m.estMaintenanceDue(Fixtures.AUJOURDHUI)).isFalse();
  }

  @Test
  void should_reform_in_two_steps_R13() {
    Materiel m = Fixtures.materiel();

    assertThatThrownBy(() -> m.deciderReforme(true, LIBRE))
        .extracting("code")
        .isEqualTo("REFORME_NON_DEMANDEE");
    assertThatThrownBy(() -> m.demanderReforme(" "))
        .extracting("code")
        .isEqualTo("MOTIF_OBLIGATOIRE");
    m.demanderReforme("Châssis fissuré");
    assertThat(m.isReformeDemandee()).isTrue();
    assertThatThrownBy(() -> m.demanderReforme("bis"))
        .extracting("code")
        .isEqualTo("REFORME_DEJA_DEMANDEE");

    m.deciderReforme(true, LIBRE);

    assertThat(m.getEtat()).isEqualTo(EtatMateriel.REFORME);
    assertThat(m.isReformeDemandee()).isFalse();
    assertThat(m.getMotifReforme()).isEqualTo("Châssis fissuré");
  }

  @Test
  void should_lift_request_when_dg_refuses_R13() {
    Materiel m = Fixtures.materiel();
    m.demanderReforme("Usure");

    m.deciderReforme(false, LIBRE);

    assertThat(m.getEtat()).isEqualTo(EtatMateriel.DISPONIBLE);
    assertThat(m.isReformeDemandee()).isFalse();
    assertThat(m.getMotifReforme()).isNull();
  }

  @Test
  void should_refuse_reform_of_busy_materiel_R13() {
    Materiel m = Fixtures.materiel();
    m.demanderReforme("Usure");

    assertThatThrownBy(() -> m.deciderReforme(true, new ContexteMateriel(true, false)))
        .extracting("code")
        .isEqualTo("REFORME_IMPOSSIBLE");
    assertThatThrownBy(() -> m.deciderReforme(true, new ContexteMateriel(false, true)))
        .extracting("code")
        .isEqualTo("REFORME_IMPOSSIBLE");
  }

  @Test
  void should_freeze_reformed_materiel_R13() {
    Materiel m = Fixtures.materiel();
    m.demanderReforme("Usure");
    m.deciderReforme(true, LIBRE);

    assertThatThrownBy(() -> m.releverHeures(2000))
        .extracting("code")
        .isEqualTo("MATERIEL_REFORME");
    assertThatThrownBy(m::mettreEnMaintenance).extracting("code").isEqualTo("MATERIEL_REFORME");
    assertThatThrownBy(() -> m.demanderReforme("x"))
        .extracting("code")
        .isEqualTo("MATERIEL_REFORME");
    assertThatThrownBy(() -> m.affecter(Fixtures.chantier()))
        .extracting("code")
        .isEqualTo("MATERIEL_NON_DISPONIBLE");
  }

  @Test
  void should_modify_description_with_version_and_new_type_thresholds() {
    TypeEngin pelle = Fixtures.typePelle(500, 180);
    Materiel m = versionne(Fixtures.materiel(pelle));
    TypeEngin autre = Fixtures.typePelle(250, 90);

    assertThatThrownBy(() -> m.modifier(Fixtures.description("A", pelle.getId()), pelle, 3L))
        .extracting("code")
        .isEqualTo("CONFLIT_CONCURRENCE");

    m.modifier(Fixtures.description("PEL-NEW", autre.getId()), autre, 4L);

    assertThat(m.getCode()).isEqualTo("PEL-NEW");
    assertThat(m.getSeuilMaintenanceHeures()).isEqualTo(250);
    assertThat(m.getSeuilMaintenanceJours()).isEqualTo(90);

    m.modifier(
        new DescriptionMateriel(
            "PEL-NEW",
            null,
            "Pelle",
            CategorieMateriel.MATERIEL_ROULANT,
            null,
            null,
            null,
            BigDecimal.TEN,
            null,
            null),
        null,
        4L);
    assertThat(m.getSeuilMaintenanceHeures()).isNull();
  }

  @Test
  void should_keep_origin_of_delivered_materiel() {
    UUID fournisseur = UUID.randomUUID();
    UUID contrat = UUID.randomUUID();

    Materiel m =
        Materiel.enregistrer(
            Fixtures.description("ENG-2026-0001", null),
            null,
            0,
            new Materiel.Origine(fournisseur, contrat),
            Fixtures.AUJOURDHUI);

    assertThat(m.getFournisseurId()).isEqualTo(fournisseur);
    assertThat(m.getContratId()).isEqualTo(contrat);
    assertThat(m.prochaineMaintenance()).isNull();
  }
}
