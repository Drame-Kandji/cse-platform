package sn.cse.suivi.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Colonnes d'audit communes ({@code created_at/created_by/updated_at/updated_by}), renseignées par
 * Spring Data à partir de l'horloge et de l'utilisateur courant. Les colonnes de création ne sont
 * jamais réécrites (fusion d'entités détachées reconstruites depuis le domaine).
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

  @CreatedDate
  @Column(
      name = "created_at",
      nullable = false,
      updatable = false,
      columnDefinition = "timestamptz")
  private Instant createdAt;

  @CreatedBy
  @Column(name = "created_by", updatable = false, length = 100)
  private String createdBy;

  @LastModifiedDate
  @Column(name = "updated_at", columnDefinition = "timestamptz")
  private Instant updatedAt;

  @LastModifiedBy
  @Column(name = "updated_by", length = 100)
  private String updatedBy;
}
