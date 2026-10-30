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
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.EtatMateriel;

/**
 * Ligne de {@code suivi.materiels}. {@code prochaineMaintenance} est calculée par le domaine et
 * stockée pour permettre le filtre « maintenance due » en base.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "materiels")
public class MaterielEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 40)
  private String code;

  @Column(length = 40)
  private String immatriculation;

  @Column(nullable = false, length = 200)
  private String designation;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "categorie_materiel")
  private CategorieMateriel categorie;

  @Column(name = "type_engin_id")
  private UUID typeEnginId;

  @Column(length = 100)
  private String marque;

  @Column(length = 100)
  private String modele;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "etat_materiel")
  private EtatMateriel etat;

  @Column(name = "heures_fonctionnement", nullable = false)
  private long heuresFonctionnement;

  @Column(name = "heures_derniere_maintenance", nullable = false)
  private long heuresDerniereMaintenance;

  @Column(name = "date_reference_maintenance", nullable = false)
  private LocalDate dateReferenceMaintenance;

  @Column(name = "seuil_maintenance_heures")
  private Integer seuilMaintenanceHeures;

  @Column(name = "seuil_maintenance_jours")
  private Integer seuilMaintenanceJours;

  @Column(name = "prochaine_maintenance")
  private LocalDate prochaineMaintenance;

  @Column(name = "date_derniere_alerte_maintenance")
  private LocalDate dateDerniereAlerteMaintenance;

  @Column(name = "tarif_journalier", nullable = false)
  private BigDecimal tarifJournalier;

  @Column(name = "valeur_acquisition")
  private BigDecimal valeurAcquisition;

  @Column(name = "date_acquisition")
  private LocalDate dateAcquisition;

  @Column(name = "chantier_actuel_id")
  private UUID chantierActuelId;

  private Double latitude;

  private Double longitude;

  @Column(name = "fournisseur_id")
  private UUID fournisseurId;

  @Column(name = "contrat_id")
  private UUID contratId;

  @Column(name = "reforme_demandee", nullable = false)
  private boolean reformeDemandee;

  @Column(name = "motif_reforme", length = 500)
  private String motifReforme;

  @Version private Long version;
}
