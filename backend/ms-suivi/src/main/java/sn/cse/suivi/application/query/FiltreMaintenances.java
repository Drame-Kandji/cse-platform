package sn.cse.suivi.application.query;

import java.util.UUID;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.domain.model.TypeMaintenance;

/**
 * Critères de recherche des ordres de travail.
 *
 * @param statut statut (optionnel)
 * @param type type (optionnel)
 * @param materielId matériel (optionnel)
 * @param perimetre chantiers visibles
 */
public record FiltreMaintenances(
    StatutMaintenance statut, TypeMaintenance type, UUID materielId, Perimetre perimetre) {}
