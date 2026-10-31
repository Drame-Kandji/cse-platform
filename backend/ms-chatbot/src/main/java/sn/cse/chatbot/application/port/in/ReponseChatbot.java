package sn.cse.chatbot.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import sn.cse.chatbot.domain.model.Intention;
import sn.cse.chatbot.domain.model.Source;

/**
 * Réponse de l'assistant (contrat §10).
 *
 * @param conversationId conversation
 * @param messageId message de réponse
 * @param reponse texte markdown
 * @param sources sources citées
 * @param intention intention détectée
 * @param modeDegrade vrai si produite sans LLM
 * @param horodatage date
 */
public record ReponseChatbot(
    UUID conversationId,
    UUID messageId,
    String reponse,
    List<Source> sources,
    Intention intention,
    boolean modeDegrade,
    Instant horodatage) {}
