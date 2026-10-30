package sn.cse.suivi.application.query;

import java.util.UUID;
import sn.cse.suivi.domain.model.StatutAffectation;

/**
 * Critères de recherche des affectations.
 *
 * @param materielId matériel (optionnel)
 * @param statut statut (optionnel)
 * @param perimetre chantiers visibles (porte aussi le filtre {@code chantierId})
 */
public record FiltreAffectations(UUID materielId, StatutAffectation statut, Perimetre perimetre) {}
