package sn.cse.chatbot.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Conversation d'un utilisateur avec l'assistant (agrégat). Seul son propriétaire peut la consulter
 * ou la supprimer.
 */
public final class Conversation {

  private static final String ELLIPSE = "…";

  private final UUID id;
  private final UUID utilisateurId;
  private final String titre;
  private final List<Message> messages;
  private final Instant createdAt;
  private Instant updatedAt;
  private final long version;

  /**
   * Reconstitution depuis la persistance.
   *
   * @param id identifiant
   * @param utilisateurId propriétaire
   * @param titre titre
   * @param messages messages dans l'ordre chronologique
   * @param createdAt création
   * @param updatedAt dernière activité
   * @param version version (verrou optimiste)
   */
  public Conversation(
      UUID id,
      UUID utilisateurId,
      String titre,
      List<Message> messages,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = Objects.requireNonNull(id, "id");
    this.utilisateurId = Objects.requireNonNull(utilisateurId, "utilisateurId");
    this.titre = Objects.requireNonNull(titre, "titre");
    this.messages = new ArrayList<>(messages);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    this.version = version;
  }

  /**
   * @param utilisateurId propriétaire
   * @param premiereQuestion première question (sert de titre)
   * @param maintenant date
   * @param longueurTitre longueur maximale du titre
   * @return une nouvelle conversation vide
   */
  public static Conversation demarrer(
      UUID utilisateurId, String premiereQuestion, Instant maintenant, int longueurTitre) {
    return new Conversation(
        UUID.randomUUID(),
        utilisateurId,
        tronquer(premiereQuestion.replaceAll("\\s+", " ").trim(), longueurTitre),
        List.of(),
        maintenant,
        maintenant,
        0);
  }

  /**
   * @param contenu question
   * @param maintenant date
   * @return le message ajouté
   */
  public Message ajouterQuestion(String contenu, Instant maintenant) {
    return ajouter(
        new Message(
            UUID.randomUUID(),
            RoleMessage.UTILISATEUR,
            contenu,
            List.of(),
            null,
            false,
            maintenant));
  }

  /**
   * @param contenu réponse
   * @param sources sources citées
   * @param intention intention détectée
   * @param modeDegrade réponse sans LLM
   * @param maintenant date
   * @return le message ajouté
   */
  public Message ajouterReponse(
      String contenu,
      List<Source> sources,
      Intention intention,
      boolean modeDegrade,
      Instant maintenant) {
    return ajouter(
        new Message(
            UUID.randomUUID(),
            RoleMessage.ASSISTANT,
            contenu,
            sources,
            intention,
            modeDegrade,
            maintenant));
  }

  private Message ajouter(Message m) {
    messages.add(m);
    updatedAt = m.horodatage();
    return m;
  }

  /**
   * @param candidat utilisateur
   * @return vrai s'il est propriétaire
   */
  public boolean appartientA(UUID candidat) {
    return utilisateurId.equals(candidat);
  }

  /**
   * @param n nombre maximal
   * @return les {@code n} derniers messages (contexte conversationnel du LLM)
   */
  public List<Message> derniersMessages(int n) {
    return List.copyOf(messages.subList(Math.max(0, messages.size() - n), messages.size()));
  }

  /**
   * @param longueurApercu longueur de l'aperçu du dernier message
   * @return le résumé pour l'historique
   */
  public ConversationResume resume(int longueurApercu) {
    String dernier =
        messages.isEmpty()
            ? ""
            : tronquer(messages.get(messages.size() - 1).contenu(), longueurApercu);
    return new ConversationResume(id, titre, dernier, messages.size(), updatedAt);
  }

  static String tronquer(String texte, int longueur) {
    String t = texte.replaceAll("\\s+", " ").trim();
    return t.length() <= longueur ? t : t.substring(0, Math.max(0, longueur - 1)).trim() + ELLIPSE;
  }

  /**
   * @return identifiant
   */
  public UUID id() {
    return id;
  }

  /**
   * @return propriétaire
   */
  public UUID utilisateurId() {
    return utilisateurId;
  }

  /**
   * @return titre
   */
  public String titre() {
    return titre;
  }

  /**
   * @return messages (lecture seule)
   */
  public List<Message> messages() {
    return Collections.unmodifiableList(messages);
  }

  /**
   * @return date de création
   */
  public Instant createdAt() {
    return createdAt;
  }

  /**
   * @return dernière activité
   */
  public Instant updatedAt() {
    return updatedAt;
  }

  /**
   * @return version
   */
  public long version() {
    return version;
  }
}
