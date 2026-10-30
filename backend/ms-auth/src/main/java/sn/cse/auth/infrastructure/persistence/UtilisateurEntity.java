package sn.cse.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Racine JPA de la hiérarchie des profils (stratégie SINGLE_TABLE, discriminateur {@code role}).
 * Entité de persistance uniquement : les règles vivent dans {@code domain.model}.
 */
@Entity
@Table(name = "utilisateurs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING, length = 30)
@EntityListeners(AuditingEntityListener.class)
public abstract class UtilisateurEntity {

  @Id private UUID id;

  // Lecture seule : le discriminateur est écrit par Hibernate ; exposé pour filtrer et trier.
  @Column(name = "role", insertable = false, updatable = false)
  private String role;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String prenom;

  @Column(nullable = false)
  private String nom;

  private String telephone;

  @Column(nullable = false)
  private boolean actif;

  @Column(name = "otp_configure", nullable = false)
  private boolean otpConfigure;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @CreatedBy
  @Column(name = "created_by", updatable = false)
  private String createdBy;

  @LastModifiedDate
  @Column(name = "updated_at")
  private Instant updatedAt;

  @LastModifiedBy
  @Column(name = "updated_by")
  private String updatedBy;

  @Version private Long version;

  protected UtilisateurEntity() {}

  protected UtilisateurEntity(UUID id, String role) {
    this.id = id;
    this.role = role;
  }

  public UUID getId() {
    return id;
  }

  public String getRole() {
    return role;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPrenom() {
    return prenom;
  }

  public void setPrenom(String prenom) {
    this.prenom = prenom;
  }

  public String getNom() {
    return nom;
  }

  public void setNom(String nom) {
    this.nom = nom;
  }

  public String getTelephone() {
    return telephone;
  }

  public void setTelephone(String telephone) {
    this.telephone = telephone;
  }

  public boolean isActif() {
    return actif;
  }

  public void setActif(boolean actif) {
    this.actif = actif;
  }

  public boolean isOtpConfigure() {
    return otpConfigure;
  }

  public void setOtpConfigure(boolean otpConfigure) {
    this.otpConfigure = otpConfigure;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Long getVersion() {
    return version;
  }
}
