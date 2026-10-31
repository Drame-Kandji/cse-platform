package sn.cse.chatbot.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.chatbot.domain.model.Intention;
import sn.cse.chatbot.domain.model.RoleMessage;

/** Table {@code conversation_messages} (sources sérialisées en JSON). */
@Entity
@Table(name = "conversation_messages")
public class MessageEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversation_id")
  private ConversationEntity conversation;

  @Column(nullable = false)
  private int ordre;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "role_message")
  private RoleMessage role;

  @Column(nullable = false, columnDefinition = "text")
  private String contenu;

  @Column(nullable = false, columnDefinition = "text")
  private String sources;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "intention_chatbot")
  private Intention intention;

  @Column(name = "mode_degrade", nullable = false)
  private boolean modeDegrade;

  @Column(nullable = false)
  private Instant horodatage;

  protected MessageEntity() {}

  MessageEntity(
      UUID id,
      ConversationEntity conversation,
      int ordre,
      RoleMessage role,
      String contenu,
      String sources,
      Intention intention,
      boolean modeDegrade,
      Instant horodatage) {
    this.id = id;
    this.conversation = conversation;
    this.ordre = ordre;
    this.role = role;
    this.contenu = contenu;
    this.sources = sources;
    this.intention = intention;
    this.modeDegrade = modeDegrade;
    this.horodatage = horodatage;
  }

  UUID getId() {
    return id;
  }

  RoleMessage getRole() {
    return role;
  }

  String getContenu() {
    return contenu;
  }

  String getSources() {
    return sources;
  }

  Intention getIntention() {
    return intention;
  }

  boolean isModeDegrade() {
    return modeDegrade;
  }

  Instant getHorodatage() {
    return horodatage;
  }
}
