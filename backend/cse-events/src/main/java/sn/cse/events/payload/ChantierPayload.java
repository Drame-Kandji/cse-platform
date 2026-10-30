package sn.cse.events.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Instantané d'un chantier (événements CHANTIER_*). */
public record ChantierPayload(
    UUID chantierId,
    String code,
    String nom,
    String region,
    String statut,
    BigDecimal budget,
    BigDecimal montantMarche,
    UUID chefChantierId,
    LocalDate dateDebut,
    LocalDate dateFinPrevue) {}
