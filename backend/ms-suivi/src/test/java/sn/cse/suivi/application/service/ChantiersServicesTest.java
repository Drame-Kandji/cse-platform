package sn.cse.suivi.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.events.EventTypes;
import sn.cse.suivi.application.query.ChantierVue;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.StatutChantier;

class ChantiersServicesTest extends ServiceTestSupport {

  private final GererChantiersService gestion =
      new GererChantiersService(m.chantiers, m.affectations, m.chargeur, m.vues, m.evenements);
  private final ConsulterChantiersService consultation =
      new ConsulterChantiersService(m.chantiers, m.chargeur, m.vues);
  private static final PageQuery PAGE = new PageQuery(0, 20, "code", true);

  @Test
  void should_create_chantier_and_publish_event() {
    ChantierVue vue = gestion.creer(donneesChantier("CH-DKR-01"), rm());

    assertThat(vue.chantier().getStatut()).isEqualTo(StatutChantier.EN_COURS);
    assertThat(vue.chantier().getVersion()).isZero();
    assertThat(vue.nombreMateriels()).isZero();
    assertThat(m.evenements.types()).containsExactly(EventTypes.CHANTIER_CREE);
  }

  @Test
  void should_refuse_duplicate_code() {
    chantier("CH-DKR-01");

    assertThatThrownBy(() -> gestion.creer(donneesChantier("CH-DKR-01"), rm()))
        .extracting("code")
        .isEqualTo("CODE_DEJA_UTILISE");
  }

  @Test
  void should_modify_with_expected_version() {
    Chantier c = chantier("CH-DKR-01");

    ChantierVue vue = gestion.modifier(c.getId(), donneesChantier("CH-DKR-02"), 0L, rm());

    assertThat(vue.chantier().getCode()).isEqualTo("CH-DKR-02");
    assertThat(vue.chantier().getVersion()).isEqualTo(1L);
    assertThat(m.evenements.types()).containsExactly(EventTypes.CHANTIER_MODIFIE);
    assertThatThrownBy(() -> gestion.modifier(c.getId(), donneesChantier("CH-X"), 0L, rm()))
        .extracting("code")
        .isEqualTo("CONFLIT_CONCURRENCE");
  }

  @Test
  void should_refuse_termination_while_materiels_affected() {
    Chantier c = chantier("CH-DKR-01");
    Materiel materiel = materiel("PEL-01");
    affecter(materiel, c);

    assertThatThrownBy(() -> gestion.terminer(c.getId(), rm()))
        .extracting("code")
        .isEqualTo("CHANTIER_MATERIELS_AFFECTES");
    assertThat(consultation.consulter(c.getId(), rm()).nombreMateriels()).isEqualTo(1);
  }

  @Test
  void should_terminate_empty_chantier() {
    Chantier c = chantier("CH-DKR-01");

    assertThat(gestion.terminer(c.getId(), rm()).chantier().getStatut())
        .isEqualTo(StatutChantier.TERMINE);
  }

  @Test
  void should_restrict_chef_to_his_chantiers() {
    Chantier sien = chantier("CH-DKR-01");
    Chantier autre = chantier("CH-THS-01");

    assertThat(consultation.rechercher(null, null, PAGE, chef(sien.getId())).content())
        .extracting(v -> v.chantier().getCode())
        .containsExactly("CH-DKR-01");
    assertThat(consultation.rechercher(null, "THS", PAGE, rm()).totalElements()).isEqualTo(1);
    assertThatThrownBy(() -> consultation.consulter(autre.getId(), chef(sien.getId())))
        .isInstanceOf(ForbiddenOperationException.class);
    assertThatThrownBy(() -> consultation.consulter(UUID.randomUUID(), rm()))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
