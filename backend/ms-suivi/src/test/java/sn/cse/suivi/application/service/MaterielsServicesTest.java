package sn.cse.suivi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.fake.Memoire;
import sn.cse.suivi.application.port.in.ConsulterMaterielsUseCase;
import sn.cse.suivi.application.query.CarteVue;
import sn.cse.suivi.application.query.CritereDisponibilite;
import sn.cse.suivi.application.query.DisponibiliteVue;
import sn.cse.suivi.application.query.MaterielDetailVue;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.EtatMateriel;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.TypeEngin;
import sn.cse.suivi.domain.model.TypeMaintenance;
import sn.cse.suivi.domain.model.TypeMouvement;

class MaterielsServicesTest extends ServiceTestSupport {

  private static final PageQuery PAGE = new PageQuery(0, 20, "code", true);

  private final GererMaterielsService gestion =
      new GererMaterielsService(
          m.materiels, m.mouvements, m.chargeur, m.vues, m.evenements, m.clock);
  private final ConsulterMaterielsService consultation =
      new ConsulterMaterielsService(
          m.materiels,
          m.affectations,
          m.maintenances,
          m.pannes,
          m.mouvements,
          m.chargeur,
          m.vues,
          m.parametres,
          m.clock);
  private final VerifierDisponibiliteService disponibilite =
      new VerifierDisponibiliteService(m.materiels, m.maintenances, m.vues, m.parametres);
  private final ConsulterCarteService carte =
      new ConsulterCarteService(m.chantiers, m.materiels, m.vues);

  @Test
  void should_register_materiel_with_type_and_trace_it() {
    TypeEngin type = type(500, 180);

    MaterielVue vue = gestion.enregistrer(description("PEL-01", type.getId()), 1200L, rm());

    assertThat(vue.materiel().getEtat()).isEqualTo(EtatMateriel.DISPONIBLE);
    assertThat(vue.typeEnginLibelle()).isEqualTo("Pelle hydraulique");
    assertThat(vue.materiel().getHeuresFonctionnement()).isEqualTo(1200);
    assertThat(m.evenements.types()).containsExactly(EventTypes.MATERIEL_ENREGISTRE);
    assertThat(m.mouvements.journal)
        .extracting(mv -> mv.type())
        .containsExactly(TypeMouvement.ENREGISTREMENT);
  }

  @Test
  void should_refuse_duplicate_code_or_unknown_type() {
    materiel("PEL-01");

    assertThatThrownBy(() -> gestion.enregistrer(description("PEL-01", null), null, rm()))
        .extracting("code")
        .isEqualTo("CODE_DEJA_UTILISE");
    assertThatThrownBy(
            () -> gestion.enregistrer(description("PEL-02", UUID.randomUUID()), null, rm()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void should_modify_description() {
    Materiel materiel = materiel("PEL-01");

    MaterielVue vue = gestion.modifier(materiel.getId(), description("PEL-09", null), 0L, rm());

    assertThat(vue.materiel().getCode()).isEqualTo("PEL-09");
    assertThat(vue.materiel().getSeuilMaintenanceHeures()).isNull();
  }

  @Test
  void should_list_only_chef_materiels_and_forbid_other_site_filter() {
    Chantier sien = chantier("CH-1");
    Chantier autre = chantier("CH-2");
    affecter(materiel("PEL-01"), sien);
    affecter(materiel("PEL-02"), autre);
    materiel("PEL-03");

    var chefVue =
        consultation.rechercher(
            new ConsulterMaterielsUseCase.Criteres(null, null, null, null, null),
            PAGE,
            chef(sien.getId()));
    var tous =
        consultation.rechercher(
            new ConsulterMaterielsUseCase.Criteres(
                EtatMateriel.AFFECTE, CategorieMateriel.MATERIEL_ROULANT, null, "PEL", false),
            PAGE,
            rm());

    assertThat(chefVue.content()).extracting(v -> v.materiel().getCode()).containsExactly("PEL-01");
    assertThat(chefVue.content().get(0).chantierActuelNom()).isEqualTo("Chantier CH-1");
    assertThat(tous.totalElements()).isEqualTo(2);
    assertThatThrownBy(
            () ->
                consultation.rechercher(
                    new ConsulterMaterielsUseCase.Criteres(null, null, autre.getId(), null, null),
                    PAGE,
                    chef(sien.getId())))
        .isInstanceOf(ForbiddenOperationException.class);
  }

  @Test
  void should_build_detail_with_histories_and_restrict_chef() {
    Chantier c = chantier("CH-1");
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, c);
    m.maintenances.save(
        Maintenance.planifier(
            "OT-2026-0001",
            m.materiels.findById(materiel.getId()).orElseThrow(),
            new Maintenance.Planification(
                TypeMaintenance.PREVENTIVE, "Vidange", Memoire.AUJOURDHUI, null, null)));

    MaterielDetailVue detail = consultation.detail(materiel.getId(), chef(c.getId()));

    assertThat(detail.affectations()).hasSize(1);
    assertThat(detail.affectations().get(0).chantierNom()).isEqualTo("Chantier CH-1");
    assertThat(detail.maintenances()).hasSize(1);
    assertThat(detail.pannes()).isEmpty();
    assertThatThrownBy(() -> consultation.detail(materiel.getId(), chef(UUID.randomUUID())))
        .isInstanceOf(ForbiddenOperationException.class);
    Materiel auParc = materiel("PEL-02");
    assertThatThrownBy(() -> consultation.detail(auParc.getId(), chef(c.getId())))
        .isInstanceOf(ForbiddenOperationException.class);
  }

  @Test
  void should_report_availability_excluding_planned_maintenance() {
    Materiel planifie = materiel("PEL-01");
    materiel("PEL-02");
    affecter(materiel("PEL-03"), chantier("CH-1"));
    m.maintenances.save(
        Maintenance.planifier(
            "OT-2026-0001",
            planifie,
            new Maintenance.Planification(
                TypeMaintenance.PREVENTIVE,
                "Vidange",
                Memoire.AUJOURDHUI.plusDays(5),
                null,
                null)));

    DisponibiliteVue sansPeriode =
        disponibilite.verifier(
            new CritereDisponibilite(CategorieMateriel.MATERIEL_ROULANT, "pelle", 2, null, null));
    DisponibiliteVue avecPeriode =
        disponibilite.verifier(
            new CritereDisponibilite(
                null, null, 2, Memoire.AUJOURDHUI, Memoire.AUJOURDHUI.plusDays(10)));
    DisponibiliteVue parDefaut =
        disponibilite.verifier(new CritereDisponibilite(null, null, null, null, null));

    assertThat(sansPeriode.disponible()).isTrue();
    assertThat(sansPeriode.quantiteDisponible()).isEqualTo(2);
    assertThat(avecPeriode.disponible()).isFalse();
    assertThat(avecPeriode.materiels())
        .extracting(v -> v.materiel().getCode())
        .containsExactly("PEL-02");
    assertThat(parDefaut.quantiteDemandee()).isEqualTo(1);
  }

  @Test
  void should_build_map_within_chef_perimeter() {
    Chantier sien = chantier("CH-1");
    chantier("CH-2");
    affecter(materiel("PEL-01"), sien);

    CarteVue chefCarte = carte.carte(null, null, chef(sien.getId()));
    CarteVue rmCarte = carte.carte(CategorieMateriel.MATERIEL_ROULANT, EtatMateriel.AFFECTE, rm());

    assertThat(chefCarte.chantiers()).hasSize(1);
    assertThat(chefCarte.chantiers().get(0).nombreMateriels()).isEqualTo(1);
    assertThat(chefCarte.materiels()).hasSize(1);
    assertThat(rmCarte.chantiers()).hasSize(2);
    assertThat(rmCarte.materiels()).hasSize(1);
  }
}
