package sn.cse.chatbot.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Table {@code conversations} (résumé dénormalisé pour l'historique paginé). */
@Entity
@Table(name = "conversations")
public class ConversationEntity {

  @Id private UUID id;

  @Column(name = "utilisateur_id", nullable = false)
  private UUID utilisateurId;

  @Column(nullable = false, length = 200)
  private String titre;

  @Column(name = "dernier_message", nullable = false, length = 500)
  private String dernierMessage;

  @Column(name = "nombre_messages", nullable = false)
  private int nombreMessages;

  @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("ordre ASC")
  private List<MessageEntity> messages = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected ConversationEntity() {}

  ConversationEntity(UUID id, UUID utilisateurId, String titre, Instant createdAt) {
    this.id = id;
    this.utilisateurId = utilisateurId;
    this.titre = titre;
    this.createdAt = createdAt;
  }

  void resumer(String dernierMessage, int nombreMessages, Instant updatedAt) {
    this.dernierMessage = dernierMessage;
    this.nombreMessages = nombreMessages;
    this.updatedAt = updatedAt;
  }

  UUID getId() {
    return id;
  }

  UUID getUtilisateurId() {
    return utilisateurId;
  }

  String getTitre() {
    return titre;
  }

  String getDernierMessage() {
    return dernierMessage;
  }

  int getNombreMessages() {
    return nombreMessages;
  }

  List<MessageEntity> getMessages() {
    return messages;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  long getVersion() {
    return version;
  }
}
