package sn.cse.events.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Instantané d'une facture fournisseur (FACTURE_*). */
public record FacturePayload(
    UUID factureId,
    String numero,
    UUID contratId,
    UUID fournisseurId,
    BigDecimal montantTtc,
    String statut,
    LocalDate dateEcheance) {}
