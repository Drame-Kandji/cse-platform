package sn.cse.fournisseurs.infrastructure.persistence.entity;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.common.crypto.EncryptedStringConverter;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;

/** Représentation persistante de l'agrégat Fournisseur (table {@code fournisseurs}). */
@Entity
@Table(name = "fournisseurs")
public class FournisseurEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "raison_sociale", nullable = false)
  private String raisonSociale;

  @Column(nullable = false)
  private String ninea;

  private String rccm;
  private String adresse;
  private String ville;

  @Column(nullable = false)
  private String pays;

  private String telephone;

  @Column(nullable = false)
  private String email;

  @Column(name = "contact_nom")
  private String contactNom;

  @Column(name = "contact_fonction")
  private String contactFonction;

  @ElementCollection
  @CollectionTable(
      name = "fournisseur_specialites",
      joinColumns = @JoinColumn(name = "fournisseur_id"))
  @Column(name = "specialite", nullable = false, columnDefinition = "categorie_materiel")
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @BatchSize(size = 50)
  private Set<CategorieMateriel> specialites = new HashSet<>();

  @ElementCollection
  @CollectionTable(
      name = "fournisseur_certifications",
      joinColumns = @JoinColumn(name = "fournisseur_id"))
  @OrderColumn(name = "ordre")
  @Column(name = "certification", nullable = false)
  @BatchSize(size = 50)
  private List<String> certifications = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "statut_fournisseur")
  private StatutFournisseur statut;

  @Column(name = "motif_statut")
  private String motifStatut;

  // Donnée sensible chiffrée au repos (AES-256-GCM, clé CSE_CRYPTO_KEY).
  @Convert(converter = EncryptedStringConverter.class)
  @Column(columnDefinition = "TEXT")
  private String banque;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(columnDefinition = "TEXT")
  private String iban;

  @Column(name = "blacklistage_demandeur_id")
  private UUID blacklistageDemandeurId;

  @Column(name = "blacklistage_demandeur_nom")
  private String blacklistageDemandeurNom;

  @Column(name = "blacklistage_motif")
  private String blacklistageMotif;

  @Column(name = "blacklistage_date")
  private Instant blacklistageDate;

  @Column(name = "score_performance", nullable = false)
  private BigDecimal scorePerformance;

  @Column(name = "nombre_evaluations", nullable = false)
  private int nombreEvaluations;

  @Column(name = "livraisons_total", nullable = false)
  private int livraisonsTotal;

  @Column(name = "livraisons_a_l_heure", nullable = false)
  private int livraisonsALHeure;

  @Column(name = "date_inscription", nullable = false)
  private LocalDate dateInscription;

  @OneToMany(mappedBy = "fournisseur", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("type ASC, dateExpiration ASC")
  @BatchSize(size = 50)
  private List<DocumentEntity> documents = new ArrayList<>();

  @Version private Long version;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getRaisonSociale() {
    return raisonSociale;
  }

  public void setRaisonSociale(String raisonSociale) {
    this.raisonSociale = raisonSociale;
  }

  public String getNinea() {
    return ninea;
  }

  public void setNinea(String ninea) {
    this.ninea = ninea;
  }

  public String getRccm() {
    return rccm;
  }

  public void setRccm(String rccm) {
    this.rccm = rccm;
  }

  public String getAdresse() {
    return adresse;
  }

  public void setAdresse(String adresse) {
    this.adresse = adresse;
  }

  public String getVille() {
    return ville;
  }

  public void setVille(String ville) {
    this.ville = ville;
  }

  public String getPays() {
    return pays;
  }

  public void setPays(String pays) {
    this.pays = pays;
  }

  public String getTelephone() {
    return telephone;
  }

  public void setTelephone(String telephone) {
    this.telephone = telephone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getContactNom() {
    return contactNom;
  }

  public void setContactNom(String contactNom) {
    this.contactNom = contactNom;
  }

  public String getContactFonction() {
    return contactFonction;
  }

  public void setContactFonction(String contactFonction) {
    this.contactFonction = contactFonction;
  }

  public Set<CategorieMateriel> getSpecialites() {
    return specialites;
  }

  public List<String> getCertifications() {
    return certifications;
  }

  public StatutFournisseur getStatut() {
    return statut;
  }

  public void setStatut(StatutFournisseur statut) {
    this.statut = statut;
  }

  public String getMotifStatut() {
    return motifStatut;
  }

  public void setMotifStatut(String motifStatut) {
    this.motifStatut = motifStatut;
  }

  public String getBanque() {
    return banque;
  }

  public void setBanque(String banque) {
    this.banque = banque;
  }

  public String getIban() {
    return iban;
  }

  public void setIban(String iban) {
    this.iban = iban;
  }

  public UUID getBlacklistageDemandeurId() {
    return blacklistageDemandeurId;
  }

  public void setBlacklistageDemandeurId(UUID blacklistageDemandeurId) {
    this.blacklistageDemandeurId = blacklistageDemandeurId;
  }

  public String getBlacklistageDemandeurNom() {
    return blacklistageDemandeurNom;
  }

  public void setBlacklistageDemandeurNom(String blacklistageDemandeurNom) {
    this.blacklistageDemandeurNom = blacklistageDemandeurNom;
  }

  public String getBlacklistageMotif() {
    return blacklistageMotif;
  }

  public void setBlacklistageMotif(String blacklistageMotif) {
    this.blacklistageMotif = blacklistageMotif;
  }

  public Instant getBlacklistageDate() {
    return blacklistageDate;
  }

  public void setBlacklistageDate(Instant blacklistageDate) {
    this.blacklistageDate = blacklistageDate;
  }

  public BigDecimal getScorePerformance() {
    return scorePerformance;
  }

  public void setScorePerformance(BigDecimal scorePerformance) {
    this.scorePerformance = scorePerformance;
  }

  public int getNombreEvaluations() {
    return nombreEvaluations;
  }

  public void setNombreEvaluations(int nombreEvaluations) {
    this.nombreEvaluations = nombreEvaluations;
  }

  public int getLivraisonsTotal() {
    return livraisonsTotal;
  }

  public void setLivraisonsTotal(int livraisonsTotal) {
    this.livraisonsTotal = livraisonsTotal;
  }

  public int getLivraisonsALHeure() {
    return livraisonsALHeure;
  }

  public void setLivraisonsALHeure(int livraisonsALHeure) {
    this.livraisonsALHeure = livraisonsALHeure;
  }

  public LocalDate getDateInscription() {
    return dateInscription;
  }

  public void setDateInscription(LocalDate dateInscription) {
    this.dateInscription = dateInscription;
  }

  public List<DocumentEntity> getDocuments() {
    return documents;
  }

  public Long getVersion() {
    return version;
  }
}
