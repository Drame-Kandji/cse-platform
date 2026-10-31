package sn.cse.chatbot.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Résumé d'une conversation pour l'historique (contrat §10).
 *
 * @param id identifiant
 * @param titre titre (début de la première question)
 * @param dernierMessage aperçu du dernier message
 * @param nombreMessages nombre de messages
 * @param updatedAt dernière activité
 */
public record ConversationResume(
    UUID id, String titre, String dernierMessage, int nombreMessages, Instant updatedAt) {}
