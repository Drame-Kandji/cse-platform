package sn.cse.achats.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import sn.cse.common.crypto.EncryptedBigDecimalConverter;

/** Table {@code offre} (entité de l'agrégat AppelOffre). */
@Getter
@Setter
@Entity
@Table(name = "offre")
public class OffreEntity extends AuditEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "appel_offre_id", nullable = false, updatable = false)
  private AppelOffreEntity appelOffre;

  @Column(nullable = false, updatable = false)
  private String codeAnonyme;

  @Column(nullable = false, updatable = false)
  private UUID fournisseurId;

  @Column(nullable = false)
  private String fournisseurNom;

  @Convert(converter = EncryptedBigDecimalConverter.class)
  @Column(nullable = false, columnDefinition = "text")
  private BigDecimal montantPropose;

  private int delaiLivraisonJours;
  private Integer garantieMois;

  @Column(columnDefinition = "text")
  private String description;

  @Column(nullable = false)
  private Instant dateDepot;

  private boolean retenue;

  @ElementCollection
  @CollectionTable(name = "offre_note", joinColumns = @JoinColumn(name = "offre_id"))
  @BatchSize(size = 50)
  private Set<NoteEmbeddable> notes = new HashSet<>();
}
