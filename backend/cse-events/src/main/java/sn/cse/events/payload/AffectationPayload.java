package sn.cse.events.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Instantané d'une affectation matériel → chantier (MATERIEL_AFFECTE, RECEPTION_CONFIRMEE…). */
public record AffectationPayload(
    UUID affectationId,
    UUID materielId,
    String materielCode,
    String materielDesignation,
    String categorie,
    BigDecimal tarifJournalier,
    UUID chantierId,
    String chantierNom,
    UUID chefChantierId,
    UUID besoinId,
    LocalDate dateDebut,
    LocalDate dateFinPrevue,
    LocalDate dateFinReelle,
    String statut,
    UUID fournisseurId,
    UUID contratId) {}
