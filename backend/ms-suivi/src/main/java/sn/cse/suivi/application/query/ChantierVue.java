package sn.cse.suivi.application.query;

import sn.cse.suivi.domain.model.Chantier;

/**
 * Chantier et indicateurs calculés.
 *
 * @param chantier chantier
 * @param nombreMateriels matériels présents sur le chantier
 */
public record ChantierVue(Chantier chantier, long nombreMateriels) {}
