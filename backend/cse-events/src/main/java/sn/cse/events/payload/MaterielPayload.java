package sn.cse.events.payload;

import java.math.BigDecimal;
import java.util.UUID;

/** Instantané d'un matériel (événements MATERIEL_* et ALERTE_MAINTENANCE). */
public record MaterielPayload(
    UUID materielId,
    String code,
    String designation,
    String categorie,
    String etat,
    UUID chantierId,
    String chantierNom,
    BigDecimal tarifJournalier,
    BigDecimal valeurAcquisition,
    long heuresFonctionnement,
    UUID fournisseurId,
    UUID contratId,
    String motif) {}
