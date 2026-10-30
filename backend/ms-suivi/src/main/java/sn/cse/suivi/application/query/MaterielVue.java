package sn.cse.suivi.application.query;

import sn.cse.suivi.domain.model.Materiel;

/**
 * Matériel et libellés résolus.
 *
 * @param materiel matériel
 * @param typeEnginLibelle libellé du type d'engin
 * @param chantierActuelNom nom du chantier courant
 * @param maintenanceDue seuil de maintenance préventive atteint à la date du jour
 */
public record MaterielVue(
    Materiel materiel, String typeEnginLibelle, String chantierActuelNom, boolean maintenanceDue) {}
