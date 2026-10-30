package sn.cse.fournisseurs.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.fournisseurs.domain.model.GraviteLitige;
import sn.cse.fournisseurs.domain.model.StatutLitige;

/** Litige persistant (table {@code litiges}). */
@Entity
@Table(name = "litiges")
public class LitigeEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(nullable = false, updatable = false)
  private String reference;

  @Column(name = "fournisseur_id", nullable = false, updatable = false)
  private UUID fournisseurId;

  @Column(name = "raison_sociale", nullable = false)
  private String raisonSociale;

  @Column(name = "contrat_id")
  private UUID contratId;

  @Column(nullable = false)
  private String objet;

  @Column(nullable = false)
  private String description;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "gravite_litige")
  private GraviteLitige gravite;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "statut_litige")
  private StatutLitige statut;

  @Column(name = "date_ouverture", nullable = false)
  private Instant dateOuverture;

  @Column(name = "date_resolution")
  private Instant dateResolution;

  @ElementCollection
  @CollectionTable(name = "actions_litige", joinColumns = @JoinColumn(name = "litige_id"))
  @OrderColumn(name = "ordre")
  @BatchSize(size = 50)
  private List<ActionLitigeEmbeddable> actions = new ArrayList<>();

  @Version private Long version;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public void setFournisseurId(UUID fournisseurId) {
    this.fournisseurId = fournisseurId;
  }

  public String getRaisonSociale() {
    return raisonSociale;
  }

  public void setRaisonSociale(String raisonSociale) {
    this.raisonSociale = raisonSociale;
  }

  public UUID getContratId() {
    return contratId;
  }

  public void setContratId(UUID contratId) {
    this.contratId = contratId;
  }

  public String getObjet() {
    return objet;
  }

  public void setObjet(String objet) {
    this.objet = objet;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public GraviteLitige getGravite() {
    return gravite;
  }

  public void setGravite(GraviteLitige gravite) {
    this.gravite = gravite;
  }

  public StatutLitige getStatut() {
    return statut;
  }

  public void setStatut(StatutLitige statut) {
    this.statut = statut;
  }

  public Instant getDateOuverture() {
    return dateOuverture;
  }

  public void setDateOuverture(Instant dateOuverture) {
    this.dateOuverture = dateOuverture;
  }

  public Instant getDateResolution() {
    return dateResolution;
  }

  public void setDateResolution(Instant dateResolution) {
    this.dateResolution = dateResolution;
  }

  public List<ActionLitigeEmbeddable> getActions() {
    return actions;
  }

  public Long getVersion() {
    return version;
  }
}
