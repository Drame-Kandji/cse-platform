package sn.cse.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sn.cse.reporting.domain.JeuDeDonnees.AUJOURDHUI;
import static sn.cse.reporting.domain.JeuDeDonnees.C1;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.DonneesReporting;
import sn.cse.reporting.domain.model.Granularite;
import sn.cse.reporting.domain.model.HistoriqueParc;
import sn.cse.reporting.domain.model.Indicateur;
import sn.cse.reporting.domain.model.ParametresReporting;
import sn.cse.reporting.domain.model.Perimetre;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.Point;
import sn.cse.reporting.domain.model.vue.ParcInstantane;
import sn.cse.reporting.domain.service.CalculateurSeries;

/** Séries sur {@link JeuDeDonnees} (septembre → 30 octobre 2026). */
class CalculateurSeriesTest {

  private static final Periode SEPT_OCT =
      new Periode(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 30));

  private final CalculateurSeries series =
      new CalculateurSeries(new ParametresReporting(90, 1830, 100, 5, 365));
  private final DonneesReporting donnees = JeuDeDonnees.donnees();

  private List<Point> serie(Indicateur i, Granularite g, Periode p, List<ParcInstantane> photos) {
    return series.calculer(
        i, g, p, Perimetre.tous(), donnees, photos, HistoriqueParc.vide(), AUJOURDHUI);
  }

  private static Point point(String periode, String valeur) {
    return new Point(periode, new BigDecimal(valeur));
  }

  @Test
  void should_cumuler_montant_engage_et_couts_par_mois() {
    assertThat(serie(Indicateur.MONTANT_ENGAGE, Granularite.MOIS, SEPT_OCT, List.of()))
        .usingRecursiveFieldByFieldElementComparatorOnFields("periode")
        .containsExactly(point("2026-09", "1000000"), point("2026-10", "5000000"));
    List<Point> engage = serie(Indicateur.MONTANT_ENGAGE, Granularite.MOIS, SEPT_OCT, List.of());
    assertThat(engage.get(1).valeur()).isEqualByComparingTo("5000000");
    List<Point> couts = serie(Indicateur.COUT_MAINTENANCE, Granularite.MOIS, SEPT_OCT, List.of());
    assertThat(couts.get(0).valeur()).isEqualByComparingTo("0");
    assertThat(couts.get(1).valeur()).isEqualByComparingTo("300000");
  }

  @Test
  void should_compter_besoins_soumis_par_mois() {
    List<Point> b = serie(Indicateur.BESOINS_SOUMIS, Granularite.MOIS, SEPT_OCT, List.of());
    assertThat(b).extracting(Point::periode).containsExactly("2026-09", "2026-10");
    assertThat(b).extracting(p -> p.valeur().intValue()).containsExactly(1, 3);
  }

  @Test
  void should_omettre_tranche_sans_livraison_when_moyenne() {
    List<Point> d =
        serie(Indicateur.DELAI_APPROVISIONNEMENT, Granularite.MOIS, SEPT_OCT, List.of());
    assertThat(d).hasSize(1);
    assertThat(d.get(0).periode()).isEqualTo("2026-10");
    assertThat(d.get(0).valeur()).isEqualByComparingTo("56.5");
  }

  @Test
  void should_reporter_dernier_instantane_when_disponibilite() {
    List<ParcInstantane> photos =
        List.of(
            new ParcInstantane(
                LocalDate.of(2026, 9, 15), CategorieMateriel.OUTILLAGE, null, 5, 2, 2, 1, 0),
            new ParcInstantane(
                LocalDate.of(2026, 10, 20), CategorieMateriel.OUTILLAGE, null, 3, 1, 1, 0, 1),
            new ParcInstantane(
                LocalDate.of(2026, 10, 20), CategorieMateriel.MATERIEL_ROULANT, C1, 2, 0, 1, 1, 0));
    List<Point> mois = serie(Indicateur.TAUX_DISPONIBILITE, Granularite.MOIS, SEPT_OCT, photos);
    assertThat(mois).extracting(Point::periode).containsExactly("2026-09", "2026-10");
    // Disponibilité opérationnelle : (5 − 1 en maintenance) / 5
    assertThat(mois.get(0).valeur()).isEqualByComparingTo("80.0");
    // (5 − 1 en maintenance − 1 réformé) / (5 − 1 réformé)
    assertThat(mois.get(1).valeur()).isEqualByComparingTo("75.0");

    List<Point> jours =
        serie(
            Indicateur.TAUX_DISPONIBILITE,
            Granularite.JOUR,
            new Periode(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 16)),
            photos);
    assertThat(jours).extracting(Point::periode).containsExactly("2026-09-15", "2026-09-16");

    List<Point> chef =
        series.calculer(
            Indicateur.TAUX_DISPONIBILITE,
            Granularite.MOIS,
            SEPT_OCT,
            Perimetre.chantiers(Set.of(C1)),
            donnees,
            photos,
            HistoriqueParc.vide(),
            AUJOURDHUI);
    // Septembre : aucun instantané du chantier ; octobre : 1 affecté + 1 en maintenance → 1 / 2.
    assertThat(chef).hasSize(1);
    assertThat(chef.get(0).valeur()).isEqualByComparingTo("50.0");
  }

  @Test
  void should_reconstituer_disponibilite_avant_premier_instantane() {
    HistoriqueParc historique =
        new HistoriqueParc(donnees.materiels(), donnees.affectations(), donnees.maintenances());
    List<ParcInstantane> photos =
        List.of(
            new ParcInstantane(
                LocalDate.of(2026, 10, 20), CategorieMateriel.OUTILLAGE, null, 4, 1, 1, 1, 1));

    List<Point> mois =
        series.calculer(
            Indicateur.TAUX_DISPONIBILITE,
            Granularite.MOIS,
            SEPT_OCT,
            Perimetre.tous(),
            donnees,
            photos,
            historique,
            AUJOURDHUI);

    // 30/09 reconstitué : seul M2 est connu (affecté à C2) → 100 % ; octobre : instantané
    // enregistré.
    assertThat(mois).extracting(Point::periode).containsExactly("2026-09", "2026-10");
    assertThat(mois.get(0).valeur()).isEqualByComparingTo("100.0");
    assertThat(mois.get(1).valeur()).isEqualByComparingTo("66.7");

    List<Point> sansInstantane =
        series.calculer(
            Indicateur.TAUX_DISPONIBILITE,
            Granularite.JOUR,
            new Periode(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 6)),
            Perimetre.chantiers(Set.of(C1)),
            donnees,
            List.of(),
            historique,
            AUJOURDHUI);
    // 06/10 : M3 affecté à C1 (M2 en maintenance hors chantier n'est pas dans le périmètre).
    assertThat(sansInstantane).containsExactly(point("2026-10-06", "100.0"));
  }

  @Test
  void should_calculer_utilisation_par_semaine_sans_jours_futurs() {
    List<Point> s =
        serie(
            Indicateur.TAUX_UTILISATION,
            Granularite.SEMAINE,
            new Periode(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 8)),
            List.of());
    // Semaine du 26/10 : 5 jours écoulés ; semaine du 02/11 entièrement future → pas de point.
    assertThat(s).extracting(Point::periode).containsExactly("2026-W44");
    // M1 5/5, M2 0/5, M3 5 jours immobilisés → 5 / 10
    assertThat(s.get(0).valeur()).isEqualByComparingTo("50.0");
  }

  @Test
  void should_refuser_serie_trop_longue() {
    Periode deuxAns = new Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 10, 30));
    assertThatThrownBy(() -> serie(Indicateur.BESOINS_SOUMIS, Granularite.JOUR, deuxAns, List.of()))
        .isInstanceOf(InvalidRequestException.class)
        .extracting("code")
        .isEqualTo("SERIE_TROP_LONGUE");
  }
}
