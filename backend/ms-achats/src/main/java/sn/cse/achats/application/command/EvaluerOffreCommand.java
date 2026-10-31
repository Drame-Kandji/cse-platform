package sn.cse.achats.application.command;

import java.util.Map;
import java.util.UUID;
import sn.cse.achats.domain.model.CritereEvaluation;

/**
 * Saisie des notes d'une offre.
 *
 * @param appelOffreId appel d'offres
 * @param offreId offre
 * @param notes notes 0..100 des critères non calculés
 */
public record EvaluerOffreCommand(
    UUID appelOffreId, UUID offreId, Map<CritereEvaluation, Integer> notes) {

  public EvaluerOffreCommand {
    notes = notes == null ? Map.of() : Map.copyOf(notes);
  }
}
