package sn.cse.events.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Instantané d'une intervention de maintenance (MAINTENANCE_*). */
public record MaintenancePayload(
    UUID maintenanceId,
    String numeroOt,
    UUID materielId,
    String materielDesignation,
    UUID chantierId,
    String type,
    String statut,
    BigDecimal cout,
    LocalDate datePlanifiee,
    LocalDate dateDebut,
    LocalDate dateFin) {}
