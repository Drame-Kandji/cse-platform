package sn.cse.achats.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sn.cse.achats.Donnees.F1;
import static sn.cse.achats.Donnees.F2;
import static sn.cse.achats.Donnees.F3;
import static sn.cse.achats.Donnees.T0;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.achats.Donnees;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.Offre;
import sn.cse.achats.domain.service.Comparatif;
import sn.cse.achats.domain.service.ContexteEvaluation;
import sn.cse.achats.domain.service.MoteurEvaluation;
import sn.cse.achats.domain.service.NotationDelai;
import sn.cse.achats.domain.service.NotationPrix;
import sn.cse.achats.domain.service.NotationSaisie;
import sn.cse.achats.domain.service.OffreEvaluee;
import sn.cse.common.exception.ConflictException;

class MoteurEvaluationTest {

  private final MoteurEvaluation moteur = MoteurEvaluation.standard();

  private static Offre offre(String montant, int delai, Map<CritereEvaluation, Integer> notes) {
    return new Offre(
        UUID.randomUUID(),
        "OFR-000001",
        F1.id(),
        F1.raisonSociale(),
        new BigDecimal(montant),
        delai,
        null,
        null,
        T0,
        notes,
        false);
  }

  @Test
  void should_noter_prix_relativement_a_la_meilleure_offre() {
    ContexteEvaluation contexte = new ContexteEvaluation(new BigDecimal("90000000"), 30);

    assertThat(new NotationPrix().noter(offre("90000000", 30, Map.of()), contexte))
        .contains(new BigDecimal("100.00"));
    assertThat(new NotationPrix().noter(offre("120000000", 30, Map.of()), contexte))
        .contains(new BigDecimal("75.00"));
    assertThat(new NotationPrix().critere()).isEqualTo(CritereEvaluation.PRIX);
  }

  @Test
  void should_noter_delai_relativement_au_delai_le_plus_court() {
    ContexteEvaluation contexte = new ContexteEvaluation(BigDecimal.ONE, 45);

    assertThat(new NotationDelai().noter(offre("1", 45, Map.of()), contexte))
        .contains(new BigDecimal("100.00"));
    assertThat(new NotationDelai().noter(offre("1", 60, Map.of()), contexte))
        .contains(new BigDecimal("75.00"));
    assertThat(new NotationDelai().critere()).isEqualTo(CritereEvaluation.DELAI);
  }

  @Test
  void should_reprendre_note_saisie_ou_rien() {
    NotationSaisie sav = new NotationSaisie(CritereEvaluation.SAV);
    ContexteEvaluation contexte = new ContexteEvaluation(BigDecimal.ONE, 1);

    assertThat(sav.noter(offre("1", 1, Map.of(CritereEvaluation.SAV, 70)), contexte))
        .contains(new BigDecimal("70.00"));
    assertThat(sav.noter(offre("1", 1, Map.of()), contexte)).isEmpty();
    assertThatThrownBy(() -> new NotationSaisie(CritereEvaluation.PRIX))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_exiger_une_strategie_par_critere() {
    assertThatThrownBy(() -> new MoteurEvaluation(List.of(new NotationPrix())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_ne_produire_aucune_note_when_ao_publie_R10() {
    AppelOffre ao = Donnees.aoPublie();
    ao.deposerOffre(F1, new BigDecimal("100"), 10, null, null, T0.plusSeconds(5));
    ao.deposerOffre(F2, new BigDecimal("90"), 10, null, null, T0);

    List<OffreEvaluee> evaluees = moteur.evaluer(ao);

    assertThat(evaluees)
        .allSatisfy(
            e -> {
              assertThat(e.notes()).isEmpty();
              assertThat(e.scoreGlobal()).isNull();
              assertThat(e.rang()).isNull();
            });
    assertThat(evaluees.get(0).offre().getFournisseurId()).isEqualTo(F2.id());
    assertThatThrownBy(() -> moteur.comparer(ao)).isInstanceOf(ConflictException.class);
  }

  @Test
  void should_calculer_score_pondere_rang_et_recommandation() {
    // Grille : PRIX 40, DELAI 20, QUALITE_TECHNIQUE 40.
    AppelOffre ao = Donnees.aoCloture("300000000", "250000000");
    Offre o1 = ao.offreDu(F1.id()).orElseThrow();
    Offre o2 = ao.offreDu(F2.id()).orElseThrow();
    ao.evaluerOffre(o1.getId(), Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 90));
    ao.evaluerOffre(o2.getId(), Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 60));

    Comparatif comparatif = moteur.comparer(ao);

    // F1 : prix 83,33 ; délai 75 ; QT 90 → 33,33 + 15 + 36 = 84,33
    // F2 : prix 100 ; délai 100 ; QT 60 → 40 + 20 + 24 = 84,00
    assertThat(comparatif.offres())
        .extracting(e -> e.offre().getId())
        .containsExactly(o1.getId(), o2.getId());
    assertThat(comparatif.offres().get(0).scoreGlobal()).isEqualByComparingTo("84.33");
    assertThat(comparatif.offres().get(0).notes())
        .containsEntry(CritereEvaluation.PRIX, new BigDecimal("83.33"));
    assertThat(comparatif.offres().get(1).scoreGlobal()).isEqualByComparingTo("84.00");
    assertThat(comparatif.offres()).extracting(OffreEvaluee::rang).containsExactly(1, 2);
    assertThat(comparatif.recommandationOffreId()).isEqualTo(o1.getId());
    assertThat(comparatif.criteres()).hasSize(3);
  }

  @Test
  void should_classer_en_dernier_les_offres_incompletes_sans_recommandation() {
    AppelOffre ao = Donnees.aoCloture("300000000", "250000000");
    ao.evaluerOffre(
        ao.offreDu(F1.id()).orElseThrow().getId(), Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 90));

    List<OffreEvaluee> evaluees = moteur.evaluer(ao);

    assertThat(evaluees.get(0).rang()).isEqualTo(1);
    assertThat(evaluees.get(1).rang()).isNull();
    assertThat(evaluees.get(1).scoreGlobal()).isNull();
    assertThat(evaluees.get(1).notes())
        .containsKeys(CritereEvaluation.PRIX, CritereEvaluation.DELAI);

    AppelOffre aucuneNote = Donnees.aoCloture("300000000", "250000000");
    assertThat(moteur.comparer(aucuneNote).recommandationOffreId()).isNull();
  }

  @Test
  void should_departager_ex_aequo_par_le_prix_puis_le_depot() {
    AppelOffre ao = Donnees.aoPublie();
    // Même prix et même délai : départage par l'antériorité du dépôt.
    ao.deposerOffre(F1, new BigDecimal("100"), 10, null, null, T0.plusSeconds(60));
    ao.deposerOffre(F3, new BigDecimal("100"), 10, null, null, T0.plusSeconds(30));
    ao.cloturer(Donnees.LIMITE);
    ao.evaluerOffre(
        ao.offreDu(F1.id()).orElseThrow().getId(), Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 50));
    ao.evaluerOffre(
        ao.offreDu(F3.id()).orElseThrow().getId(), Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 50));

    List<OffreEvaluee> evaluees = moteur.evaluer(ao);

    assertThat(evaluees.get(0).offre().getFournisseurId()).isEqualTo(F3.id());
    assertThat(evaluees.get(0).scoreGlobal()).isEqualByComparingTo(evaluees.get(1).scoreGlobal());
  }

  @Test
  void should_evaluer_liste_vide() {
    AppelOffre ao = Donnees.aoPublie();
    ao.cloturer(Donnees.LIMITE);

    assertThat(moteur.evaluer(ao)).isEmpty();
    assertThat(ContexteEvaluation.de(List.of()))
        .isEqualTo(new ContexteEvaluation(BigDecimal.ONE, 1));
  }
}
