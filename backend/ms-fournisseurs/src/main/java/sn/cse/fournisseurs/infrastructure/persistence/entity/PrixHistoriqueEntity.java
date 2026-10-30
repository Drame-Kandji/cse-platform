package sn.cse.fournisseurs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Entrée d'historique de prix (table {@code prix_historiques}), en ajout seul. */
@Entity
@Table(name = "prix_historiques")
public class PrixHistoriqueEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "fournisseur_id", nullable = false)
  private UUID fournisseurId;

  @Column(nullable = false)
  private String reference;

  @Column(nullable = false)
  private String designation;

  @Column(name = "prix_unitaire", nullable = false)
  private BigDecimal prixUnitaire;

  @Column(name = "date_prix", nullable = false)
  private Instant datePrix;

  /** Constructeur requis par JPA. */
  protected PrixHistoriqueEntity() {}

  /**
   * @param id identifiant
   * @param fournisseurId fournisseur
   * @param reference référence
   * @param designation désignation
   * @param prixUnitaire prix
   * @param datePrix date du prix
   */
  public PrixHistoriqueEntity(
      UUID id,
      UUID fournisseurId,
      String reference,
      String designation,
      BigDecimal prixUnitaire,
      Instant datePrix) {
    this.id = id;
    this.fournisseurId = fournisseurId;
    this.reference = reference;
    this.designation = designation;
    this.prixUnitaire = prixUnitaire;
    this.datePrix = datePrix;
  }

  public UUID getId() {
    return id;
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public String getReference() {
    return reference;
  }

  public String getDesignation() {
    return designation;
  }

  public BigDecimal getPrixUnitaire() {
    return prixUnitaire;
  }

  public Instant getDatePrix() {
    return datePrix;
  }
}
