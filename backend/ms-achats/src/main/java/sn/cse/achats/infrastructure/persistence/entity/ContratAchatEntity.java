package sn.cse.achats.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.achats.domain.model.StatutContrat;
import sn.cse.common.crypto.EncryptedBigDecimalConverter;

/** Table {@code contrat_achat} (agrégat ContratAchat, engagement DAF inclus). */
@Getter
@Setter
@Entity
@Table(name = "contrat_achat")
public class ContratAchatEntity extends VersionedEntity {

  @Id private UUID id;

  @Column(nullable = false, updatable = false)
  private String numero;

  @Column(nullable = false, updatable = false)
  private UUID appelOffreId;

  private String appelOffreReference;

  @Column(nullable = false)
  private UUID offreId;

  @Column(nullable = false)
  private UUID fournisseurId;

  private String fournisseurNom;
  private UUID besoinId;
  private UUID chantierId;
  private String chantierNom;
  private String objet;
  private String designation;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "categorie_materiel")
  private CategorieMateriel categorie;

  private int quantite;

  @Convert(converter = EncryptedBigDecimalConverter.class)
  @Column(nullable = false, columnDefinition = "text")
  private BigDecimal montant;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "statut_contrat")
  private StatutContrat statut;

  private LocalDate dateSignature;
  private int delaiLivraisonJours;
  private LocalDate dateLivraisonPrevue;
  private LocalDate dateLivraisonReelle;
  private String numeroBonLivraison;

  @Column(columnDefinition = "text")
  private String observationsLivraison;

  private Integer garantieMois;

  @Column(columnDefinition = "text")
  private String conditionsMaintenance;

  @Column(columnDefinition = "text")
  private String conditionsLivraison;

  @Column(precision = 7, scale = 4)
  private BigDecimal penaliteRetardPctParJour;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "statut_approbation")
  private StatutApprobation engagementStatut;

  @Convert(converter = EncryptedBigDecimalConverter.class)
  @Column(columnDefinition = "text")
  private BigDecimal engagementMontant;

  private UUID engagementParId;
  private String engagementParNom;
  private Instant engagementDateDemande;
  private Instant engagementDateDecision;

  @Column(columnDefinition = "text")
  private String engagementCommentaire;

  @Convert(converter = EncryptedBigDecimalConverter.class)
  @Column(columnDefinition = "text")
  private BigDecimal penalitesRetard;

  private LocalDate dateDerniereAlerteRetard;

  private UUID signatureCseParId;
  private String signatureCseParNom;
  private Instant signatureCseDate;
  private UUID signatureFournisseurParId;
  private String signatureFournisseurParNom;
  private Instant signatureFournisseurDate;

  @Column(columnDefinition = "text")
  private String motifRefusSignature;

  private LocalDate dateExpedition;

  @Column(columnDefinition = "text")
  private String commentaireExpedition;

  @OneToMany(mappedBy = "contrat", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("ordre ASC")
  @BatchSize(size = 50)
  private List<JalonEntity> jalons = new ArrayList<>();
}
