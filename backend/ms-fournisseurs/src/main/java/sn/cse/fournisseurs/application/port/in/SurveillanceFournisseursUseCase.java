package sn.cse.fournisseurs.application.port.in;

/** Contrôles périodiques du référentiel (job planifié). */
public interface SurveillanceFournisseursUseCase {

  /**
   * Émet {@code DOCUMENT_EXPIRE} pour chaque document expiré non encore signalé.
   *
   * @return nombre de documents signalés
   */
  int signalerDocumentsExpires();

  /**
   * Émet une alerte ({@code SCORE_RECALCULE} avec motif) pour chaque fournisseur actif dont le
   * score est inférieur au seuil.
   *
   * @return nombre d'alertes émises
   */
  int alerterScoresFaibles();
}
