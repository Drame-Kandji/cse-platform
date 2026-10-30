package sn.cse.suivi.application.query;

import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.CategorieMateriel;

/**
 * Affectation et libellés résolus.
 *
 * @param affectation affectation
 * @param materielCode code du matériel
 * @param materielDesignation désignation du matériel
 * @param categorie catégorie du matériel
 * @param chantierNom nom du chantier
 */
public record AffectationVue(
    Affectation affectation,
    String materielCode,
    String materielDesignation,
    CategorieMateriel categorie,
    String chantierNom) {}
