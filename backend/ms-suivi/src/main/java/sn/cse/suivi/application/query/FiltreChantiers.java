package sn.cse.suivi.application.query;

import sn.cse.suivi.domain.model.StatutChantier;

/**
 * Critères de recherche des chantiers.
 *
 * @param statut statut (optionnel)
 * @param q texte libre sur code, nom, localisation, région (optionnel)
 * @param perimetre chantiers visibles
 */
public record FiltreChantiers(StatutChantier statut, String q, Perimetre perimetre) {}
