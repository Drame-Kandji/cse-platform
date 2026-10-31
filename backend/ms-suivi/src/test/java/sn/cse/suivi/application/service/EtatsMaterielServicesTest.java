package sn.cse.suivi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.TypeMouvement;

class EtatsMaterielServicesTest extends ServiceTestSupport {

  private final ReleverHeuresService releve =
      new ReleverHeuresService(m.materiels, m.chargeur, m.vues);
  private final ChangerEtatMaterielService etat =
      new ChangerEtatMaterielService(
          m.materiels, m.mouvements, m.chargeur, m.vues, m.evenements, m.clock);
  private final ReformerMaterielService reforme =
      new ReformerMaterielService(
          m.materiels, m.mouvements, m.chargeur, m.vues, m.evenements, m.clock);

  @Test
  void should_record_hours_and_flag_maintenance_due() {
    Chantier chantier = chantier("CH-1");
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, chantier);

    MaterielVue vue = releve.relever(materiel.getId(), 1600, chef(chantier.getId()));

    assertThat(vue.materiel().getHeuresFonctionnement()).isEqualTo(1600);
    assertThat(vue.maintenanceDue()).isTrue();
    assertThatThrownBy(() -> releve.relever(materiel.getId(), 1700, chef(UUID.randomUUID())))
        .isInstanceOf(ForbiddenOperationException.class);
    assertThatThrownBy(
            () -> releve.relever(materiel("PEL-02").getId(), 1700, chef(chantier.getId())))
        .isInstanceOf(ForbiddenOperationException.class);
  }

  @Test
  void should_switch_to_maintenance_and_back_with_events() {
    Materiel materiel = materiel("PEL-01");

    MaterielVue immobilise =
        etat.changerEtat(materiel.getId(), EtatMateriel.EN_MAINTENANCE, "Contrôle", rm());
    MaterielVue remis = etat.changerEtat(materiel.getId(), EtatMateriel.DISPONIBLE, "OK", rm());

    assertThat(immobilise.materiel().getEtat()).isEqualTo(EtatMateriel.EN_MAINTENANCE);
    assertThat(remis.materiel().getEtat()).isEqualTo(EtatMateriel.DISPONIBLE);
    assertThat(m.evenements.publies)
        .extracting(e -> e.type() + ":" + e.motif())
        .containsExactly(
            EventTypes.MATERIEL_ETAT_CHANGE + ":Contrôle", EventTypes.MATERIEL_ETAT_CHANGE + ":OK");
    assertThat(m.mouvements.journal.get(0).commentaire())
        .isEqualTo("DISPONIBLE → EN_MAINTENANCE : Contrôle");
  }

  @Test
  void should_return_to_affected_after_manual_maintenance_on_site() {
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, chantier("CH-1"));

    etat.changerEtat(materiel.getId(), EtatMateriel.EN_MAINTENANCE, "Fuite", rm());
    MaterielVue vue = etat.changerEtat(materiel.getId(), EtatMateriel.DISPONIBLE, "Réparé", rm());

    assertThat(vue.materiel().getEtat()).isEqualTo(EtatMateriel.AFFECTE);
    assertThat(vue.chantierActuelNom()).isEqualTo("Chantier CH-1");
  }

  @Test
  void should_reform_after_dg_approval_R13() {
    Materiel materiel = materiel("PEL-01");

    MaterielVue demande = reforme.demander(materiel.getId(), "Usure", rm());
    MaterielVue decision = reforme.decider(materiel.getId(), true, "Vente", dg());

    assertThat(demande.materiel().isReformeDemandee()).isTrue();
    assertThat(decision.materiel().getEtat()).isEqualTo(EtatMateriel.REFORME);
    assertThat(m.evenements.publies)
        .extracting(e -> e.type() + ":" + e.motif())
        .containsExactly(
            EventTypes.MATERIEL_REFORME_DEMANDEE + ":Usure",
            EventTypes.MATERIEL_REFORME + ":Usure · Vente");
    assertThat(m.mouvements.journal)
        .extracting(mv -> mv.type())
        .containsOnly(TypeMouvement.REFORME);
  }

  @Test
  void should_lift_request_when_dg_refuses_R13() {
    Materiel materiel = materiel("PEL-01");
    reforme.demander(materiel.getId(), "Usure", rm());

    MaterielVue decision = reforme.decider(materiel.getId(), false, null, dg());

    assertThat(decision.materiel().getEtat()).isEqualTo(EtatMateriel.DISPONIBLE);
    assertThat(decision.materiel().isReformeDemandee()).isFalse();
    assertThat(m.evenements.publies.get(1).motif()).isEqualTo("Réforme refusée");
  }

  @Test
  void should_refuse_reform_of_affected_materiel_R13() {
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, chantier("CH-1"));
    reforme.demander(materiel.getId(), "Usure", rm());

    assertThatThrownBy(() -> reforme.decider(materiel.getId(), true, null, dg()))
        .extracting("code")
        .isEqualTo("REFORME_IMPOSSIBLE");
  }
}
