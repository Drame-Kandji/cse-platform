package sn.cse.achats.application.command;

import java.util.List;
import java.util.UUID;
import sn.cse.achats.domain.model.ClausesContrat;
import sn.cse.achats.domain.model.NouveauJalon;

/**
 * Rédaction des clauses d'un contrat BROUILLON.
 *
 * @param contratId contrat
 * @param clauses clauses particulières
 * @param delaiLivraisonJours délai de livraison
 * @param jalons jalons (remplacent les existants)
 */
public record ModifierClausesCommand(
    UUID contratId, ClausesContrat clauses, int delaiLivraisonJours, List<NouveauJalon> jalons) {

  public ModifierClausesCommand {
    jalons = jalons == null ? List.of() : List.copyOf(jalons);
  }
}
