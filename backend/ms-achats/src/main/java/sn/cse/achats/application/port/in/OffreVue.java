package sn.cse.achats.application.port.in;

import java.util.UUID;
import sn.cse.achats.domain.service.OffreEvaluee;

/**
 * Offre présentée aux utilisateurs internes.
 *
 * @param appelOffreId appel d'offres
 * @param anonyme vrai tant que l'appel d'offres est publié : identité masquée (R10)
 * @param evaluation offre et notes
 */
public record OffreVue(UUID appelOffreId, boolean anonyme, OffreEvaluee evaluation) {}
