package sn.cse.suivi.application.query;

import sn.cse.suivi.domain.model.Maintenance;

/**
 * Ordre de travail et libellés résolus.
 *
 * @param maintenance ordre de travail
 * @param materielCode code du matériel
 * @param materielDesignation désignation du matériel
 * @param chantierNom nom du chantier
 */
public record MaintenanceVue(
    Maintenance maintenance, String materielCode, String materielDesignation, String chantierNom) {}
