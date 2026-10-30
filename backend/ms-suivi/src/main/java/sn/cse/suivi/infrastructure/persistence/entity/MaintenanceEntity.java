package sn.cse.suivi.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.suivi.domain.model.StatutMaintenance;
import sn.cse.suivi.domain.model.TypeMaintenance;

/** Ligne de {@code suivi.maintenances}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "maintenances")
public class MaintenanceEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "numero_ot", nullable = false, length = 30)
  private String numeroOt;

  @Column(name = "materiel_id", nullable = false)
  private UUID materielId;

  @Column(name = "chantier_id")
  private UUID chantierId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "type_maintenance")
  private TypeMaintenance type;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "statut_maintenance")
  private StatutMaintenance statut;

  @Column(nullable = false, length = 1000)
  private String description;

  @Column(name = "date_planifiee", nullable = false)
  private LocalDate datePlanifiee;

  @Column(name = "date_debut")
  private LocalDate dateDebut;

  @Column(name = "date_fin")
  private LocalDate dateFin;

  @Column(length = 150)
  private String technicien;

  private BigDecimal cout;

  @Column(name = "heures_compteur")
  private Long heuresCompteur;

  @Column(length = 1000)
  private String observations;

  @Column(name = "panne_id")
  private UUID panneId;

  @Version private Long version;
}
