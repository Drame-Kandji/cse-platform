package sn.cse.suivi.application.query;

import java.util.UUID;
import sn.cse.suivi.domain.model.StatutPanne;

/**
 * Critères de recherche des pannes.
 *
 * @param statut statut (optionnel)
 * @param materielId matériel (optionnel)
 * @param perimetre chantiers visibles (porte aussi le filtre {@code chantierId})
 */
public record FiltrePannes(StatutPanne statut, UUID materielId, Perimetre perimetre) {}
