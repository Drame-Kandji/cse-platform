package sn.cse.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.audit.domain.model.AuditLog;

/**
 * Ligne du journal d'audit. {@link Immutable} : Hibernate n'émet jamais d'UPDATE pour cette entité
 * ; toutes les colonnes sont en outre non modifiables. Pas de setter ni de {@code @Version} (une
 * entrée n'a qu'une version).
 */
@Entity
@Immutable
@Table(name = "audit_log")
public class AuditLogEntity {

  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "event_id", updatable = false, nullable = false, unique = true)
  private UUID eventId;

  @Column(name = "occurred_at", updatable = false, nullable = false)
  private Instant occurredAt;

  @Column(name = "service", updatable = false, nullable = false, length = 80)
  private String service;

  @Column(name = "action", updatable = false, nullable = false, length = 80)
  private String action;

  @Column(name = "entite", updatable = false, nullable = false, length = 120)
  private String entite;

  @Column(name = "entite_id", updatable = false, nullable = false, length = 120)
  private String entiteId;

  @Column(name = "utilisateur_id", updatable = false, length = 120)
  private String utilisateurId;

  @Column(name = "utilisateur_nom", updatable = false, length = 200)
  private String utilisateurNom;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "roles", updatable = false, nullable = false, columnDefinition = "text[]")
  private List<String> roles = new ArrayList<>();

  @Column(name = "resume", updatable = false, columnDefinition = "text")
  private String resume;

  @Column(name = "correlation_id", updatable = false, length = 120)
  private String correlationId;

  @Column(name = "enregistre_le", updatable = false, nullable = false)
  private Instant enregistreLe;

  /** Constructeur requis par JPA. */
  protected AuditLogEntity() {}

  /**
   * @param log entrée du domaine
   * @return l'entité correspondante
   */
  static AuditLogEntity depuis(AuditLog log) {
    AuditLogEntity e = new AuditLogEntity();
    e.id = log.id();
    e.eventId = log.eventId();
    e.occurredAt = log.occurredAt();
    e.service = log.service();
    e.action = log.action();
    e.entite = log.entite();
    e.entiteId = log.entiteId();
    e.utilisateurId = log.utilisateurId();
    e.utilisateurNom = log.utilisateurNom();
    e.roles = new ArrayList<>(log.roles());
    e.resume = log.resume();
    e.correlationId = log.correlationId();
    e.enregistreLe = log.enregistreLe();
    return e;
  }

  /**
   * @return l'entrée du domaine
   */
  AuditLog versDomaine() {
    return new AuditLog(
        id,
        eventId,
        occurredAt,
        service,
        action,
        entite,
        entiteId,
        utilisateurId,
        utilisateurNom,
        roles == null ? List.of() : List.copyOf(roles),
        resume,
        correlationId,
        enregistreLe);
  }
}
