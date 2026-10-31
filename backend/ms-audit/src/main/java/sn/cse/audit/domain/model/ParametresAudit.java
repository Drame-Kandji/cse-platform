package sn.cse.audit.domain.model;

/**
 * Paramètres métier de la consultation du journal (valeurs issues de {@code cse.audit.*}).
 *
 * @param fenetreStatistiquesJours nombre de jours de la fenêtre glissante des statistiques (jour
 *     courant inclus) ; 7 pour l'indicateur {@code septDerniersJours} du contrat
 * @param longueurMaxRecherche longueur maximale du texte libre {@code q}
 */
public record ParametresAudit(int fenetreStatistiquesJours, int longueurMaxRecherche) {

  public ParametresAudit {
    if (fenetreStatistiquesJours < 1) {
      throw new IllegalArgumentException(
          "La fenêtre des statistiques doit compter au moins 1 jour");
    }
    if (longueurMaxRecherche < 1) {
      throw new IllegalArgumentException("La longueur de recherche doit être positive");
    }
  }
}
