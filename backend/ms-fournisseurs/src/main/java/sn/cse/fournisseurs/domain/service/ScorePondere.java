package sn.cse.fournisseurs.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import sn.cse.fournisseurs.domain.model.NotesEvaluation;

/**
 * Politique de score par moyenne pondérée (règle R07).
 *
 * <ol>
 *   <li>Note globale d'une évaluation = Σ(poids × note) / Σ poids × 10 (échelle 0..100).
 *   <li>Composante évaluations = moyenne des notes globales (score initial si aucune évaluation).
 *   <li>Si des livraisons sont suivies : score = composante × (100 − p)/100 + taux × p/100, où p
 *       est le poids de la ponctualité.
 *   <li>Pénalités : par litige ouvert et par litige résolu ; résultat borné à [0, 100].
 * </ol>
 */
public class ScorePondere implements ScorePolicy {

  private static final BigDecimal CENT = BigDecimal.valueOf(100);
  private static final BigDecimal DIX = BigDecimal.TEN;
  private static final int ECHELLE = 1;
  private static final int ECHELLE_CALCUL = 6;

  private final ParametresScore parametres;

  /**
   * @param parametres poids, pénalités et seuils
   */
  public ScorePondere(ParametresScore parametres) {
    this.parametres = Objects.requireNonNull(parametres, "parametres");
  }

  @Override
  public BigDecimal noteGlobale(NotesEvaluation notes) {
    return arrondir(noteBrute(notes));
  }

  @Override
  public BigDecimal calculer(IndicateursPerformance indicateurs) {
    BigDecimal composanteEvaluations =
        indicateurs.evaluations().isEmpty()
            ? parametres.scoreInitial()
            : indicateurs.evaluations().stream()
                .map(this::noteBrute)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                    BigDecimal.valueOf(indicateurs.evaluations().size()),
                    ECHELLE_CALCUL,
                    RoundingMode.HALF_UP);
    BigDecimal score =
        indicateurs
            .tauxLivraisonALHeure()
            .map(taux -> combinerPonctualite(composanteEvaluations, taux))
            .orElse(composanteEvaluations);
    BigDecimal penalites =
        parametres
            .penaliteLitigeOuvert()
            .multiply(BigDecimal.valueOf(indicateurs.litigesOuverts()))
            .add(
                parametres
                    .penaliteLitigeResolu()
                    .multiply(BigDecimal.valueOf(indicateurs.litigesResolus())));
    return arrondir(borner(score.subtract(penalites)));
  }

  @Override
  public boolean estSousSeuilAlerte(BigDecimal score) {
    return score.compareTo(parametres.seuilAlerte()) < 0;
  }

  @Override
  public BigDecimal seuilAlerte() {
    return parametres.seuilAlerte();
  }

  @Override
  public BigDecimal scoreInitial() {
    return arrondir(parametres.scoreInitial());
  }

  private BigDecimal combinerPonctualite(BigDecimal composante, BigDecimal taux) {
    BigDecimal poids = BigDecimal.valueOf(parametres.poidsLivraisonALHeure());
    return composante
        .multiply(CENT.subtract(poids))
        .add(taux.multiply(poids))
        .divide(CENT, ECHELLE_CALCUL, RoundingMode.HALF_UP);
  }

  private BigDecimal noteBrute(NotesEvaluation n) {
    long somme =
        (long) parametres.poidsQualite() * n.qualite()
            + (long) parametres.poidsDelais() * n.delais()
            + (long) parametres.poidsConformite() * n.conformite()
            + (long) parametres.poidsSav() * n.sav()
            + (long) parametres.poidsPrix() * n.prix();
    return BigDecimal.valueOf(somme)
        .multiply(DIX)
        .divide(
            BigDecimal.valueOf(parametres.sommePoidsCriteres()),
            ECHELLE_CALCUL,
            RoundingMode.HALF_UP);
  }

  private static BigDecimal borner(BigDecimal valeur) {
    return valeur.max(BigDecimal.ZERO).min(CENT);
  }

  private static BigDecimal arrondir(BigDecimal valeur) {
    return valeur.setScale(ECHELLE, RoundingMode.HALF_UP);
  }
}
