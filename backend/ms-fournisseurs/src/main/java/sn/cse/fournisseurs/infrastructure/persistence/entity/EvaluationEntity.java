package sn.cse.fournisseurs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Évaluation persistante (table {@code evaluations}), immuable après création. */
@Entity
@Table(name = "evaluations")
public class EvaluationEntity extends AuditableEntity {

  @Id private UUID id;

  @Column(name = "fournisseur_id", nullable = false)
  private UUID fournisseurId;

  @Column(name = "contrat_id")
  private UUID contratId;

  @Column(nullable = false)
  private int qualite;

  @Column(nullable = false)
  private int delais;

  @Column(nullable = false)
  private int conformite;

  @Column(nullable = false)
  private int sav;

  @Column(nullable = false)
  private int prix;

  @Column(name = "note_globale", nullable = false)
  private BigDecimal noteGlobale;

  private String commentaire;

  @Column(name = "evaluateur_id")
  private UUID evaluateurId;

  @Column(name = "evaluateur_nom", nullable = false)
  private String evaluateurNom;

  @Column(name = "date_evaluation", nullable = false)
  private Instant dateEvaluation;

  /** Constructeur requis par JPA. */
  protected EvaluationEntity() {}

  /**
   * @param id identifiant
   * @param fournisseurId fournisseur
   * @param contratId contrat
   * @param notes notes [qualite, delais, conformite, sav, prix]
   * @param noteGlobale note globale
   * @param commentaire commentaire
   * @param evaluateurId évaluateur
   * @param evaluateurNom nom de l'évaluateur
   * @param dateEvaluation date
   */
  public EvaluationEntity(
      UUID id,
      UUID fournisseurId,
      UUID contratId,
      int[] notes,
      BigDecimal noteGlobale,
      String commentaire,
      UUID evaluateurId,
      String evaluateurNom,
      Instant dateEvaluation) {
    this.id = id;
    this.fournisseurId = fournisseurId;
    this.contratId = contratId;
    this.qualite = notes[0];
    this.delais = notes[1];
    this.conformite = notes[2];
    this.sav = notes[3];
    this.prix = notes[4];
    this.noteGlobale = noteGlobale;
    this.commentaire = commentaire;
    this.evaluateurId = evaluateurId;
    this.evaluateurNom = evaluateurNom;
    this.dateEvaluation = dateEvaluation;
  }

  public UUID getId() {
    return id;
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public UUID getContratId() {
    return contratId;
  }

  public int getQualite() {
    return qualite;
  }

  public int getDelais() {
    return delais;
  }

  public int getConformite() {
    return conformite;
  }

  public int getSav() {
    return sav;
  }

  public int getPrix() {
    return prix;
  }

  public BigDecimal getNoteGlobale() {
    return noteGlobale;
  }

  public String getCommentaire() {
    return commentaire;
  }

  public UUID getEvaluateurId() {
    return evaluateurId;
  }

  public String getEvaluateurNom() {
    return evaluateurNom;
  }

  public Instant getDateEvaluation() {
    return dateEvaluation;
  }
}
