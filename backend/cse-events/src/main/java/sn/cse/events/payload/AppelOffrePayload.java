package sn.cse.events.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Instantané d'un appel d'offres (AO_*, OFFRE_DEPOSEE, ATTRIBUTION_*, MARCHE_ATTRIBUE). */
public record AppelOffrePayload(
    UUID appelOffreId,
    String reference,
    String objet,
    String type,
    UUID besoinId,
    UUID chantierId,
    String categorie,
    int quantite,
    Instant dateLimite,
    String statut,
    int nombreOffres,
    List<UUID> fournisseurIdsInvites,
    UUID offreRetenueId,
    UUID fournisseurRetenuId,
    BigDecimal montantAttribue,
    boolean approbationDgRequise) {

  public AppelOffrePayload {
    fournisseurIdsInvites =
        fournisseurIdsInvites == null ? List.of() : List.copyOf(fournisseurIdsInvites);
  }
}
