package sn.cse.achats.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.CriterePondere;
import sn.cse.achats.domain.model.Offre;
import sn.cse.achats.domain.model.PonderationCriteres;

/**
 * Évalue les offres d'un appel d'offres : chaque critère est noté par sa {@link StrategieNotation},
 * le score global est la moyenne pondérée des notes (poids sur 100) et le rang départage les
 * ex-aequo par le prix puis l'antériorité du dépôt.
 */
public final class MoteurEvaluation {

  static final BigDecimal CENT = BigDecimal.valueOf(100);
  static final int ECHELLE = 2;

  private static final Comparator<OffreEvaluee> PAR_MERITE =
      Comparator.comparing(OffreEvaluee::scoreGlobal, Comparator.reverseOrder())
          .thenComparing(e -> e.offre().getMontantPropose())
          .thenComparing(e -> e.offre().getDateDepot());

  private final Map<CritereEvaluation, StrategieNotation> strategies;

  /**
   * @param strategies une stratégie par critère
   * @throws IllegalArgumentException si un critère n'a pas de stratégie
   */
  public MoteurEvaluation(List<StrategieNotation> strategies) {
    this.strategies = new EnumMap<>(CritereEvaluation.class);
    strategies.forEach(s -> this.strategies.put(s.critere(), s));
    for (CritereEvaluation c : CritereEvaluation.values()) {
      if (!this.strategies.containsKey(c)) {
        throw new IllegalArgumentException("Aucune stratégie de notation pour " + c);
      }
    }
  }

  /**
   * @return le moteur standard : PRIX et DELAI relatifs, autres critères saisis
   */
  public static MoteurEvaluation standard() {
    List<StrategieNotation> liste = new ArrayList<>();
    liste.add(new NotationPrix());
    liste.add(new NotationDelai());
    for (CritereEvaluation c : CritereEvaluation.values()) {
      if (!c.estCalcule()) {
        liste.add(new NotationSaisie(c));
      }
    }
    return new MoteurEvaluation(liste);
  }

  /**
   * Évalue les offres. Tant que l'appel d'offres est publié (R10), aucune note n'est produite.
   *
   * @param appelOffre appel d'offres
   * @return les offres évaluées, triées par rang puis par date de dépôt
   */
  public List<OffreEvaluee> evaluer(AppelOffre appelOffre) {
    List<Offre> offres = appelOffre.getOffres();
    if (appelOffre.estAnonyme() || offres.isEmpty()) {
      return offres.stream()
          .sorted(Comparator.comparing(Offre::getDateDepot))
          .map(OffreEvaluee::sansNote)
          .toList();
    }
    PonderationCriteres ponderation = appelOffre.getPonderation();
    ContexteEvaluation contexte = ContexteEvaluation.de(offres);
    List<OffreEvaluee> completes = new ArrayList<>();
    List<OffreEvaluee> incompletes = new ArrayList<>();
    for (Offre offre : offres) {
      Map<CritereEvaluation, BigDecimal> notes = new EnumMap<>(CritereEvaluation.class);
      for (CriterePondere c : ponderation.criteres()) {
        strategies.get(c.code()).noter(offre, contexte).ifPresent(n -> notes.put(c.code(), n));
      }
      Optional<BigDecimal> score = score(ponderation, notes);
      if (score.isPresent()) {
        completes.add(new OffreEvaluee(offre, notes, score.get(), null));
      } else {
        incompletes.add(new OffreEvaluee(offre, notes, null, null));
      }
    }
    completes.sort(PAR_MERITE);
    List<OffreEvaluee> resultat = new ArrayList<>();
    for (int i = 0; i < completes.size(); i++) {
      OffreEvaluee e = completes.get(i);
      resultat.add(new OffreEvaluee(e.offre(), e.notes(), e.scoreGlobal(), i + 1));
    }
    incompletes.sort(Comparator.comparing(e -> e.offre().getDateDepot()));
    resultat.addAll(incompletes);
    return resultat;
  }

  /**
   * Construit le comparatif (anonymat levé requis).
   *
   * @param appelOffre appel d'offres clôturé ou attribué
   * @return le comparatif et la recommandation (offre classée première)
   * @throws sn.cse.common.exception.ConflictException si l'appel d'offres est encore publié
   */
  public Comparatif comparer(AppelOffre appelOffre) {
    appelOffre.exigerEvaluable();
    List<OffreEvaluee> evaluees = evaluer(appelOffre);
    return new Comparatif(
        appelOffre.getId(),
        appelOffre.getPonderation().criteres(),
        evaluees,
        evaluees.stream()
            .filter(e -> e.rang() != null && e.rang() == 1)
            .map(e -> e.offre().getId())
            .findFirst()
            .orElse(null));
  }

  private static Optional<BigDecimal> score(
      PonderationCriteres ponderation, Map<CritereEvaluation, BigDecimal> notes) {
    BigDecimal total = BigDecimal.ZERO;
    for (CriterePondere c : ponderation.criteres()) {
      BigDecimal note = notes.get(c.code());
      if (note == null) {
        return Optional.empty();
      }
      total = total.add(note.multiply(BigDecimal.valueOf(c.poids())));
    }
    return Optional.of(total.divide(CENT, ECHELLE, RoundingMode.HALF_UP));
  }
}
