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
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.Priorite;

/** Table {@code besoin_a_acheter} (projection de BESOIN_EN_ACHAT). */
@Getter
@Setter
@Entity
@Table(name = "besoin_a_acheter")
public class BesoinAAcheterEntity extends VersionedEntity {

  @Id private UUID besoinId;

  private String reference;
  private String designation;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "categorie_materiel")
  private CategorieMateriel categorie;

  private int quantite;

  @Column(columnDefinition = "text")
  private String specifications;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "priorite_besoin")
  private Priorite priorite;

  private UUID chantierId;
  private String chantierNom;

  @Column(nullable = false)
  private Instant dateReception;

  private UUID appelOffreId;
}
