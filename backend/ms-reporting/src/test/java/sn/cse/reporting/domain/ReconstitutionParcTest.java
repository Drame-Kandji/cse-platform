package sn.cse.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static sn.cse.reporting.domain.JeuDeDonnees.AUJOURDHUI;
import static sn.cse.reporting.domain.JeuDeDonnees.C1;

import java.time.LocalDate;
import java.util.List;
import java.util.function.ToLongFunction;
import org.junit.jupiter.api.Test;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.DonneesReporting;
import sn.cse.reporting.domain.model.HistoriqueParc;
import sn.cse.reporting.domain.model.vue.ParcInstantane;
import sn.cse.reporting.domain.service.ReconstitutionParc;

/** Reconstitution des instantanés passés sur {@link JeuDeDonnees}. */
class ReconstitutionParcTest {

  private final DonneesReporting d = JeuDeDonnees.donnees();
  private final ReconstitutionParc reconstitution =
      new ReconstitutionParc(
          new HistoriqueParc(d.materiels(), d.affectations(), d.maintenances()), AUJOURDHUI);

  private static long somme(List<ParcInstantane> lignes, ToLongFunction<ParcInstantane> f) {
    return lignes.stream().mapToLong(f).sum();
  }

  @Test
  void should_ignorer_materiels_avant_leur_premier_fait_connu() {
    List<ParcInstantane> photo = reconstitution.au(LocalDate.of(2026, 10, 6));

    // M1 (1re affectation le 11/10) et M4 (connu le 29/10) ne sont pas encore comptés.
    assertThat(somme(photo, ParcInstantane::total)).isEqualTo(2);
    // M2 en maintenance (OT-2 du 06/10), hors chantier : son affectation à C2 s'est terminée le
    // 05/10.
    assertThat(photo)
        .filteredOn(i -> i.categorie() == CategorieMateriel.MATERIEL_ROULANT)
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.chantierId()).isNull();
              assertThat(i.enMaintenance()).isEqualTo(1);
            });
    // M3 affecté à C1.
    assertThat(photo)
        .filteredOn(i -> i.categorie() == CategorieMateriel.OUTILLAGE)
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.chantierId()).isEqualTo(C1);
              assertThat(i.affectes()).isEqualTo(1);
            });
  }

  @Test
  void should_prioriser_maintenance_sur_affectation_et_dater_la_reforme() {
    List<ParcInstantane> avantReforme = reconstitution.au(LocalDate.of(2026, 10, 25));
    // M1 affecté, M2 disponible, M3 immobilisé pendant son affectation (chantier C1).
    assertThat(somme(avantReforme, ParcInstantane::total)).isEqualTo(3);
    assertThat(somme(avantReforme, ParcInstantane::affectes)).isEqualTo(1);
    assertThat(somme(avantReforme, ParcInstantane::disponibles)).isEqualTo(1);
    assertThat(avantReforme)
        .filteredOn(i -> i.enMaintenance() == 1)
        .singleElement()
        .extracting(ParcInstantane::chantierId)
        .isEqualTo(C1);
    assertThat(somme(avantReforme, ParcInstantane::reformes)).isZero();

    List<ParcInstantane> apresReforme = reconstitution.au(LocalDate.of(2026, 10, 29));
    assertThat(somme(apresReforme, ParcInstantane::total)).isEqualTo(4);
    assertThat(somme(apresReforme, ParcInstantane::reformes)).isEqualTo(1);
    assertThat(somme(apresReforme, ParcInstantane::operationnels)).isEqualTo(2);
  }

  @Test
  void should_ne_rien_reconstituer_when_historique_vide() {
    assertThat(new ReconstitutionParc(HistoriqueParc.vide(), AUJOURDHUI).au(AUJOURDHUI)).isEmpty();
  }
}
