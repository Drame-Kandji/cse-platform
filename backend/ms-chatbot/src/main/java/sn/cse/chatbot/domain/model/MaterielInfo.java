package sn.cse.chatbot.domain.model;

import java.time.LocalDate;

/**
 * Matériel (lecture ms-suivi).
 *
 * @param id identifiant
 * @param code code interne
 * @param designation désignation
 * @param categorie catégorie
 * @param etat état
 * @param chantierNom chantier actuel ou {@code null}
 * @param chantierId chantier actuel ou {@code null}
 * @param latitude latitude ou {@code null}
 * @param longitude longitude ou {@code null}
 * @param prochaineMaintenance date de prochaine maintenance ou {@code null}
 * @param maintenanceDue maintenance préventive due
 */
public record MaterielInfo(
    String id,
    String code,
    String designation,
    String categorie,
    String etat,
    String chantierNom,
    String chantierId,
    Double latitude,
    Double longitude,
    LocalDate prochaineMaintenance,
    boolean maintenanceDue) {}
