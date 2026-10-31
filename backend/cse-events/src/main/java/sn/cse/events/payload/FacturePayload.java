package sn.cse.events.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Instantané d'une facture fournisseur (FACTURE_*).
 *
 * <p>Les dates métier ({@code dateEmission}, {@code dateMandatement}, {@code datePaiement})
 * permettent aux consommateurs (reporting) de dater mandats et paiements indépendamment de
 * l'horodatage de l'enveloppe ; elles sont absentes des événements antérieurs à leur introduction
 * (consommateurs : repli sur {@code occurredAt}).
 */
public record FacturePayload(
    UUID factureId,
    String numero,
    UUID contratId,
    UUID fournisseurId,
    BigDecimal montantTtc,
    String statut,
    LocalDate dateEmission,
    LocalDate dateEcheance,
    LocalDate dateMandatement,
    LocalDate datePaiement) {}
