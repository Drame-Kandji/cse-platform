package sn.cse.achats.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.achats.domain.model.StatutFournisseur;

/** Table {@code fournisseur_projection} (statut des fournisseurs pour la règle R14). */
@Getter
@Setter
@Entity
@Table(name = "fournisseur_projection")
public class FournisseurProjectionEntity extends VersionedEntity {

  @Id private UUID fournisseurId;

  @Column(nullable = false)
  private String raisonSociale;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "statut_fournisseur")
  private StatutFournisseur statut;

  @Column(nullable = false)
  private Instant derniereMaj;
}
