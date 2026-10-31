package sn.cse.chatbot.domain.model;

import java.time.LocalDate;

/**
 * Affectation (lecture ms-suivi).
 *
 * @param id identifiant
 * @param materielId matériel
 * @param materielCode code
 * @param materielDesignation désignation
 * @param chantierId chantier
 * @param chantierNom nom du chantier
 * @param dateDebut début
 * @param dateFinPrevue fin prévue
 * @param statut statut
 */
public record AffectationInfo(
    String id,
    String materielId,
    String materielCode,
    String materielDesignation,
    String chantierId,
    String chantierNom,
    LocalDate dateDebut,
    LocalDate dateFinPrevue,
    String statut) {}
