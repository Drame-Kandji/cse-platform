package sn.cse.suivi.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import sn.cse.suivi.domain.model.TypeMouvement;

/** Ligne de {@code suivi.mouvements} (journal en ajout seul, donc ni version ni mise à jour). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Immutable
@Table(name = "mouvements")
@EntityListeners(AuditingEntityListener.class)
public class MouvementEntity {

  @Id private UUID id;

  @Column(name = "materiel_id", nullable = false)
  private UUID materielId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "type_mouvement")
  private TypeMouvement type;

  @Column(name = "chantier_origine_id")
  private UUID chantierOrigineId;

  @Column(name = "chantier_origine_nom", length = 200)
  private String chantierOrigineNom;

  @Column(name = "chantier_destination_id")
  private UUID chantierDestinationId;

  @Column(name = "chantier_destination_nom", length = 200)
  private String chantierDestinationNom;

  @Column(name = "date_mouvement", nullable = false, columnDefinition = "timestamptz")
  private Instant date;

  @Column(name = "auteur_id")
  private UUID auteurId;

  @Column(name = "auteur_nom", length = 150)
  private String auteurNom;

  @Column(length = 1000)
  private String commentaire;

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
}
