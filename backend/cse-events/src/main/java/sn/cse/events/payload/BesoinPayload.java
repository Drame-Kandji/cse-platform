package sn.cse.events.payload;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Instantané d'un besoin matériel (événements BESOIN_*). */
public record BesoinPayload(
    UUID besoinId,
    String reference,
    UUID chantierId,
    String chantierNom,
    UUID demandeurId,
    String demandeurNom,
    String designation,
    String categorie,
    int quantite,
    String specifications,
    String priorite,
    String statut,
    LocalDate dateBesoin,
    int dureeUtilisationJours,
    String motif,
    List<UUID> materielIds) {

  public BesoinPayload {
    materielIds = materielIds == null ? List.of() : List.copyOf(materielIds);
  }
}
