package sn.cse.achats.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.StatutAO;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.achats.domain.model.TypeAppelOffre;
import sn.cse.common.crypto.EncryptedBigDecimalConverter;

/** Table {@code appel_offre} (agrégat AppelOffre, proposition d'attribution incluse). */
@Getter
@Setter
@Entity
@Table(name = "appel_offre")
public class AppelOffreEntity extends VersionedEntity {

  @Id private UUID id;

  @Column(nullable = false, updatable = false)
  private String reference;

  @Column(nullable = false)
  private String objet;

  @Column(columnDefinition = "text")
  private String description;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "type_appel_offre")
  private TypeAppelOffre type;

  private UUID besoinId;
  private String besoinReference;
  private String besoinDesignation;
  private UUID chantierId;
  private String chantierNom;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "categorie_materiel")
  private CategorieMateriel categorie;

  private int quantite;

  @Column(columnDefinition = "text")
  private String specifications;

  @Convert(converter = EncryptedBigDecimalConverter.class)
  @Column(columnDefinition = "text")
  private BigDecimal budgetEstime;

  @Column(nullable = false)
  private Instant datePublication;

  @Column(nullable = false)
  private Instant dateLimite;

  private Instant dateCloture;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "statut_ao")
  private StatutAO statut;

  private UUID offreRetenueId;

  @Convert(converter = EncryptedBigDecimalConverter.class)
  @Column(columnDefinition = "text")
  private BigDecimal montantAttribue;

  private UUID attributionOffreId;

  @Convert(converter = EncryptedBigDecimalConverter.class)
  @Column(columnDefinition = "text")
  private BigDecimal attributionMontant;

  @Column(columnDefinition = "text")
  private String attributionJustification;

  private UUID attributionParId;
  private String attributionParNom;
  private Instant attributionDate;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "statut_approbation")
  private StatutApprobation covalidationStatut;

  private UUID covalidationParId;
  private String covalidationParNom;
  private Instant covalidationDate;

  @Column(columnDefinition = "text")
  private String covalidationCommentaire;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "statut_approbation")
  private StatutApprobation approbationDgStatut;

  private UUID approbationDgParId;
  private String approbationDgParNom;
  private Instant approbationDgDate;

  @Column(columnDefinition = "text")
  private String approbationDgCommentaire;

  private UUID creeParId;

  @Column(nullable = false)
  private String creeParNom;

  @ElementCollection
  @CollectionTable(name = "appel_offre_critere", joinColumns = @JoinColumn(name = "appel_offre_id"))
  @OrderColumn(name = "ordre")
  @BatchSize(size = 50)
  private List<CritereEmbeddable> criteres = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "appel_offre_invite", joinColumns = @JoinColumn(name = "appel_offre_id"))
  @OrderColumn(name = "ordre")
  @BatchSize(size = 50)
  private List<InviteEmbeddable> invites = new ArrayList<>();

  @OneToMany(mappedBy = "appelOffre", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<OffreEntity> offres = new ArrayList<>();
}
