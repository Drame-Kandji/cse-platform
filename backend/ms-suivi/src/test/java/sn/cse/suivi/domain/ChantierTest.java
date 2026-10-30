package sn.cse.suivi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.DonneesChantier;
import sn.cse.suivi.domain.model.StatutChantier;

class ChantierTest {

  private static Chantier enregistre(StatutChantier statut) {
    Chantier c = Fixtures.chantier();
    return Chantier.reconstituer(new Chantier.Snapshot(c.getId(), c.donnees(), statut, 2L));
  }

  @Test
  void should_open_chantier_en_cours() {
    Chantier c = Fixtures.chantier();

    assertThat(c.getStatut()).isEqualTo(StatutChantier.EN_COURS);
    assertThat(c.estEnCours()).isTrue();
    assertThat(c.getVersion()).isNull();
    assertThat(c.snapshot().donnees()).isEqualTo(Fixtures.donneesChantier("CH-DKR-01"));
  }

  @Test
  void should_reject_incoherent_dates_and_amounts() {
    DonneesChantier d = Fixtures.donneesChantier("CH-X");
    DonneesChantier dates =
        new DonneesChantier(
            d.code(),
            d.nom(),
            d.localisation(),
            d.region(),
            d.coordonnees(),
            d.budget(),
            d.montantMarche(),
            d.dateDebut(),
            d.dateDebut().minusDays(1),
            null,
            null);
    DonneesChantier budget =
        new DonneesChantier(
            d.code(),
            d.nom(),
            d.localisation(),
            d.region(),
            d.coordonnees(),
            BigDecimal.valueOf(-1),
            d.montantMarche(),
            d.dateDebut(),
            null,
            null,
            null);

    assertThatThrownBy(() -> Chantier.ouvrir(dates)).isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> Chantier.ouvrir(budget)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_modify_when_version_matches() {
    Chantier c = enregistre(StatutChantier.EN_COURS);

    c.modifier(Fixtures.donneesChantier("CH-DKR-09"), 2L);

    assertThat(c.getCode()).isEqualTo("CH-DKR-09");
  }

  @Test
  void should_refuse_modification_when_version_obsolete() {
    Chantier c = enregistre(StatutChantier.EN_COURS);

    assertThatThrownBy(() -> c.modifier(Fixtures.donneesChantier("CH-DKR-09"), 1L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo("CONFLIT_CONCURRENCE");
  }

  @Test
  void should_refuse_modification_when_terminated() {
    Chantier c = enregistre(StatutChantier.TERMINE);

    assertThatThrownBy(() -> c.modifier(Fixtures.donneesChantier("CH-DKR-09"), 2L))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo("CHANTIER_TERMINE");
  }

  @Test
  void should_terminate_only_when_no_materiel_affected() {
    Chantier c = enregistre(StatutChantier.EN_COURS);

    assertThatThrownBy(() -> c.terminer(2))
        .isInstanceOf(BusinessRuleException.class)
        .extracting("code")
        .isEqualTo("CHANTIER_MATERIELS_AFFECTES");

    c.terminer(0);
    assertThat(c.getStatut()).isEqualTo(StatutChantier.TERMINE);
    assertThatThrownBy(() -> c.terminer(0)).isInstanceOf(ConflictException.class);
    assertThatThrownBy(c::verifierOuvertAuxAffectations)
        .isInstanceOf(BusinessRuleException.class)
        .extracting("code")
        .isEqualTo("CHANTIER_TERMINE");
  }
}
