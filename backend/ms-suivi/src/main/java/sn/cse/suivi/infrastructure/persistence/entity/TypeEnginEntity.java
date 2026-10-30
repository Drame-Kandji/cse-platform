package sn.cse.suivi.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.suivi.domain.model.CategorieMateriel;

/** Ligne de {@code suivi.types_engins}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "types_engins")
public class TypeEnginEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 40)
  private String code;

  @Column(nullable = false, length = 150)
  private String libelle;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "categorie_materiel")
  private CategorieMateriel categorie;

  @Column(name = "seuil_maintenance_heures")
  private Integer seuilMaintenanceHeures;

  @Column(name = "seuil_maintenance_jours")
  private Integer seuilMaintenanceJours;

  @Column(nullable = false)
  private boolean actif;

  @Version private Long version;
}
