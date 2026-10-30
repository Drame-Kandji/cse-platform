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
import sn.cse.suivi.domain.model.StatutChantier;

/** Ligne de {@code suivi.chantiers}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chantiers")
public class ChantierEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 40)
  private String code;

  @Column(nullable = false, length = 200)
  private String nom;

  @Column(nullable = false, length = 200)
  private String localisation;

  @Column(nullable = false, length = 100)
  private String region;

  @Column(nullable = false)
  private double latitude;

  @Column(nullable = false)
  private double longitude;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "statut_chantier")
  private StatutChantier statut;

  @Column(nullable = false)
  private BigDecimal budget;

  @Column(name = "montant_marche", nullable = false)
  private BigDecimal montantMarche;

  @Column(name = "date_debut", nullable = false)
  private LocalDate dateDebut;

  @Column(name = "date_fin_prevue")
  private LocalDate dateFinPrevue;

  @Column(name = "chef_chantier_id")
  private UUID chefChantierId;

  @Column(name = "chef_chantier_nom", length = 150)
  private String chefChantierNom;

  @Version private Long version;
}
