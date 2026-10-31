package sn.cse.achats.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.achats.domain.model.StatutFacture;
import sn.cse.common.crypto.EncryptedBigDecimalConverter;

/** Table {@code facture}. */
@Getter
@Setter
@Entity
@Table(name = "facture")
public class FactureEntity extends VersionedEntity {

  @Id private UUID id;

  @Column(nullable = false, updatable = false)
  private String numero;

  @Column(nullable = false, updatable = false)
  private UUID contratId;

  private String contratNumero;

  @Column(nullable = false, updatable = false)
  private UUID fournisseurId;

  private String fournisseurNom;

  @Convert(converter = EncryptedBigDecimalConverter.class)
  @Column(nullable = false, columnDefinition = "text")
  private BigDecimal montantHt;

  @Convert(converter = EncryptedBigDecimalConverter.class)
  @Column(nullable = false, columnDefinition = "text")
  private BigDecimal tva;

  @Column(nullable = false)
  private LocalDate dateEmission;

  @Column(nullable = false)
  private LocalDate dateEcheance;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "statut_facture")
  private StatutFacture statut;

  private String referenceMandat;
  private LocalDate dateMandatement;
  private LocalDate datePaiement;

  @Column(columnDefinition = "text")
  private String motifRejet;
}
