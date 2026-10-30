package sn.cse.events.payload;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Instantané d'un fournisseur (FOURNISSEUR_*, SCORE_RECALCULE, DOCUMENT_EXPIRE). */
public record FournisseurPayload(
    UUID fournisseurId,
    String raisonSociale,
    String email,
    List<String> specialites,
    String statut,
    BigDecimal scorePerformance,
    String motif) {

  public FournisseurPayload {
    specialites = specialites == null ? List.of() : List.copyOf(specialites);
  }
}
