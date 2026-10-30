package sn.cse.events.payload;

import java.util.UUID;

/** Instantané d'un litige fournisseur (LITIGE_OUVERT, LITIGE_RESOLU). */
public record LitigePayload(
    UUID litigeId,
    String reference,
    UUID fournisseurId,
    String raisonSociale,
    UUID contratId,
    String objet,
    String gravite,
    String statut) {}
