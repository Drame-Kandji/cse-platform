package sn.cse.chatbot.application.port.out;

/**
 * Filtre de recherche de matériels (paramètres de {@code GET /api/materiels}).
 *
 * @param q texte (code, désignation) ou {@code null}
 * @param categorie catégorie ou {@code null}
 * @param etat état ou {@code null}
 * @param chantierId chantier ou {@code null}
 */
public record CritereMateriel(String q, String categorie, String etat, String chantierId) {}
