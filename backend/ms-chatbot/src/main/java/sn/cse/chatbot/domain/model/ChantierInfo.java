package sn.cse.chatbot.domain.model;

/**
 * Chantier (lecture ms-suivi).
 *
 * @param id identifiant
 * @param code code
 * @param nom nom
 * @param localisation localisation
 * @param statut statut
 */
public record ChantierInfo(
    String id, String code, String nom, String localisation, String statut) {}
