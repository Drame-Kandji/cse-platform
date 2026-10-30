package sn.cse.suivi.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.suivi.domain.model.GravitePanne;
import sn.cse.suivi.domain.model.StatutPanne;

/** Ligne de {@code suivi.pannes}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pannes")
public class PanneEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "materiel_id", nullable = false)
  private UUID materielId;

  @Column(name = "chantier_id")
  private UUID chantierId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "gravite_panne")
  private GravitePanne gravite;

  @Column(nullable = false, length = 1000)
  private String description;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "statut_panne")
  private StatutPanne statut;

  @Column(name = "signale_par_id")
  private UUID signaleParId;

  @Column(name = "signale_par_nom", length = 150)
  private String signaleParNom;

  @Column(name = "date_signalement", nullable = false, columnDefinition = "timestamptz")
  private Instant dateSignalement;

  @Column(name = "maintenance_id")
  private UUID maintenanceId;

  @Version private Long version;
}
