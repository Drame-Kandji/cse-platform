package sn.cse.besoins.infrastructure.seed;

import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;

/**
 * Scénario de démonstration : un besoin et son parcours jusqu'à son statut final.
 *
 * @param numero numéro (identifiant fixe {@code b0000000-…-0000000000NN})
 * @param chantier numéro du chantier du référentiel partagé (1 à 6)
 * @param designation désignation
 * @param categorie catégorie
 * @param quantite quantité
 * @param priorite priorité
 * @param joursAvant ancienneté de la soumission (jours avant l'horloge)
 * @param delaiBesoinJours écart entre soumission et date de besoin
 * @param dureeJours durée d'utilisation
 * @param issue statut final
 * @param heuresDecision délai de décision du Responsable Matériel (heures)
 * @param specifications spécifications
 * @param justification justification (R08)
 * @param motifRejet motif si rejeté
 */
record ScenarioBesoin(
    int numero,
    int chantier,
    String designation,
    CategorieMateriel categorie,
    int quantite,
    Priorite priorite,
    int joursAvant,
    int delaiBesoinJours,
    int dureeJours,
    Issue issue,
    int heuresDecision,
    String specifications,
    String justification,
    String motifRejet) {

  /** Statut final du scénario. */
  enum Issue {
    EN_ATTENTE,
    VALIDE_ORIENTATION_EN_ATTENTE,
    REJETE,
    AFFECTE_DIRECT,
    EN_ACHAT
  }
}
