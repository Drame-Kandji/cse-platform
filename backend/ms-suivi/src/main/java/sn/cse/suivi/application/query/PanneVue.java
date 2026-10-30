package sn.cse.suivi.application.query;

import sn.cse.suivi.domain.model.Panne;

/**
 * Panne et libellés résolus.
 *
 * @param panne panne
 * @param materielCode code du matériel
 * @param materielDesignation désignation du matériel
 * @param chantierNom nom du chantier
 */
public record PanneVue(
    Panne panne, String materielCode, String materielDesignation, String chantierNom) {}
