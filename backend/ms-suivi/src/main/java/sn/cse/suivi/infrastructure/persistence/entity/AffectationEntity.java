package sn.cse.suivi.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.suivi.domain.model.StatutAffectation;

/** Ligne de {@code suivi.affectations}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "affectations")
public class AffectationEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "materiel_id", nullable = false)
  private UUID materielId;

  @Column(name = "chantier_id", nullable = false)
  private UUID chantierId;

  @Column(name = "besoin_id")
  private UUID besoinId;

  @Column(name = "date_debut", nullable = false)
  private LocalDate dateDebut;

  @Column(name = "date_fin_prevue", nullable = false)
  private LocalDate dateFinPrevue;

  @Column(name = "date_fin_reelle")
  private LocalDate dateFinReelle;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "statut_affectation")
  private StatutAffectation statut;

  @Column(name = "date_reception", columnDefinition = "timestamptz")
  private Instant dateReception;

  @Column(name = "observations_reception", length = 1000)
  private String observationsReception;

  @Column(name = "commentaire_liberation", length = 1000)
  private String commentaireLiberation;

  @Column(name = "fournisseur_id")
  private UUID fournisseurId;

  @Column(name = "contrat_id")
  private UUID contratId;

  @Column(name = "alerte_fin_envoyee", nullable = false)
  private boolean alerteFinEnvoyee;

  @Version private Long version;
}
