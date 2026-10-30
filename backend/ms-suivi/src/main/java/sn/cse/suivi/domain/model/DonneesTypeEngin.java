package sn.cse.suivi.domain.model;

import sn.cse.suivi.domain.exception.SuiviErreurs;

/**
 * Données saisissables d'un type d'engin.
 *
 * @param code code unique (ex. {@code PELLE_HYD})
 * @param libelle libellé
 * @param categorie catégorie des matériels de ce type
 * @param seuilMaintenanceHeures heures de fonctionnement entre deux maintenances préventives
 * @param seuilMaintenanceJours jours entre deux maintenances préventives
 * @param actif vrai si le type est proposé à la saisie
 */
public record DonneesTypeEngin(
    String code,
    String libelle,
    CategorieMateriel categorie,
    Integer seuilMaintenanceHeures,
    Integer seuilMaintenanceJours,
    boolean actif) {

  public DonneesTypeEngin {
    if ((seuilMaintenanceHeures != null && seuilMaintenanceHeures <= 0)
        || (seuilMaintenanceJours != null && seuilMaintenanceJours <= 0)) {
      throw SuiviErreurs.regle(
          "SEUIL_INVALIDE", "Les seuils de maintenance doivent être strictement positifs.");
    }
  }
}
