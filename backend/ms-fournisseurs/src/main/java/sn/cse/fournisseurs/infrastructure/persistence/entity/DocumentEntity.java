package sn.cse.fournisseurs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.fournisseurs.domain.model.TypeDocument;

/** Document administratif persistant (table {@code documents_administratifs}). */
@Entity
@Table(name = "documents_administratifs")
public class DocumentEntity extends AuditableEntity {

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "fournisseur_id", nullable = false)
  private FournisseurEntity fournisseur;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "type_document")
  private TypeDocument type;

  @Column(nullable = false)
  private String libelle;

  @Column(name = "date_expiration")
  private LocalDate dateExpiration;

  @Column(name = "expiration_notifiee", nullable = false)
  private boolean expirationNotifiee;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public FournisseurEntity getFournisseur() {
    return fournisseur;
  }

  public void setFournisseur(FournisseurEntity fournisseur) {
    this.fournisseur = fournisseur;
  }

  public TypeDocument getType() {
    return type;
  }

  public void setType(TypeDocument type) {
    this.type = type;
  }

  public String getLibelle() {
    return libelle;
  }

  public void setLibelle(String libelle) {
    this.libelle = libelle;
  }

  public LocalDate getDateExpiration() {
    return dateExpiration;
  }

  public void setDateExpiration(LocalDate dateExpiration) {
    this.dateExpiration = dateExpiration;
  }

  public boolean isExpirationNotifiee() {
    return expirationNotifiee;
  }

  public void setExpirationNotifiee(boolean expirationNotifiee) {
    this.expirationNotifiee = expirationNotifiee;
  }
}
