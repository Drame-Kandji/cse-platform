package sn.cse.fournisseurs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;

/** Ligne de catalogue persistante (table {@code lignes_catalogue}). */
@Entity
@Table(name = "lignes_catalogue")
public class LigneCatalogueEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "fournisseur_id", nullable = false)
  private UUID fournisseurId;

  @Column(nullable = false)
  private String reference;

  @Column(nullable = false)
  private String designation;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "categorie_materiel")
  private CategorieMateriel categorie;

  @Column(name = "prix_unitaire", nullable = false)
  private BigDecimal prixUnitaire;

  @Column(nullable = false)
  private String unite;

  @Column(name = "delai_livraison_jours", nullable = false)
  private int delaiLivraisonJours;

  @Column(name = "date_maj", nullable = false)
  private Instant dateMaj;

  @Version private Long version;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public void setFournisseurId(UUID fournisseurId) {
    this.fournisseurId = fournisseurId;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public String getDesignation() {
    return designation;
  }

  public void setDesignation(String designation) {
    this.designation = designation;
  }

  public CategorieMateriel getCategorie() {
    return categorie;
  }

  public void setCategorie(CategorieMateriel categorie) {
    this.categorie = categorie;
  }

  public BigDecimal getPrixUnitaire() {
    return prixUnitaire;
  }

  public void setPrixUnitaire(BigDecimal prixUnitaire) {
    this.prixUnitaire = prixUnitaire;
  }

  public String getUnite() {
    return unite;
  }

  public void setUnite(String unite) {
    this.unite = unite;
  }

  public int getDelaiLivraisonJours() {
    return delaiLivraisonJours;
  }

  public void setDelaiLivraisonJours(int delaiLivraisonJours) {
    this.delaiLivraisonJours = delaiLivraisonJours;
  }

  public Instant getDateMaj() {
    return dateMaj;
  }

  public void setDateMaj(Instant dateMaj) {
    this.dateMaj = dateMaj;
  }

  public Long getVersion() {
    return version;
  }
}
