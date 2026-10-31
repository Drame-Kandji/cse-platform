package sn.cse.fournisseurs.application.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.port.out.EvaluationRepository;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.application.port.out.LitigeRepository;
import sn.cse.fournisseurs.application.query.CompteurLitiges;
import sn.cse.fournisseurs.domain.model.Evaluation;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.service.IndicateursPerformance;
import sn.cse.fournisseurs.domain.service.ScorePolicy;

/**
 * Règle R07 : recalcule le score de performance d'un fournisseur à partir de toutes ses
 * évaluations, de son taux de livraison à l'heure et de ses litiges, via la {@link ScorePolicy}
 * configurée. S'exécute dans la transaction de l'appelant.
 */
@Component
public class RecalculScore {

  private final FournisseurRepository fournisseurs;
  private final EvaluationRepository evaluations;
  private final LitigeRepository litiges;
  private final ScorePolicy politique;
  private final FournisseurEventPublisher evenements;

  /**
   * @param fournisseurs dépôt des fournisseurs
   * @param evaluations dépôt des évaluations
   * @param litiges dépôt des litiges
   * @param politique stratégie de score
   * @param evenements publication outbox
   */
  public RecalculScore(
      FournisseurRepository fournisseurs,
      EvaluationRepository evaluations,
      LitigeRepository litiges,
      ScorePolicy politique,
      FournisseurEventPublisher evenements) {
    this.fournisseurs = fournisseurs;
    this.evaluations = evaluations;
    this.litiges = litiges;
    this.politique = politique;
    this.evenements = evenements;
  }

  /**
   * Recalcule, persiste et publie {@code SCORE_RECALCULE} si le score a changé.
   *
   * @param fournisseur fournisseur (état à jour, éventuellement non persisté)
   * @param declencheur cause du recalcul (inscrite dans le motif de l'événement)
   * @return fournisseur persisté
   */
  public Fournisseur recalculer(Fournisseur fournisseur, String declencheur) {
    List<Evaluation> historique = evaluations.parFournisseur(fournisseur.getId());
    CompteurLitiges compteur =
        litiges
            .compterParFournisseur(List.of(fournisseur.getId()))
            .getOrDefault(fournisseur.getId(), CompteurLitiges.AUCUN);
    IndicateursPerformance indicateurs =
        new IndicateursPerformance(
            historique.stream().map(Evaluation::notes).toList(),
            fournisseur.getPerformance().tauxLivraisonALHeure(),
            compteur.ouverts(),
            compteur.resolus());
    BigDecimal score = politique.calculer(indicateurs);
    boolean change = fournisseur.actualiserScore(score, historique.size());
    Fournisseur persiste = fournisseurs.sauvegarder(fournisseur);
    if (change) {
      evenements.publierFournisseur(
          EventTypes.SCORE_RECALCULE, persiste, motif(declencheur, score));
    }
    return persiste;
  }

  private String motif(String declencheur, BigDecimal score) {
    if (politique.estSousSeuilAlerte(score)) {
      return declencheur
          + " · score "
          + score.toPlainString()
          + " inférieur au seuil d'alerte "
          + politique.seuilAlerte().toPlainString();
    }
    return declencheur;
  }
}
