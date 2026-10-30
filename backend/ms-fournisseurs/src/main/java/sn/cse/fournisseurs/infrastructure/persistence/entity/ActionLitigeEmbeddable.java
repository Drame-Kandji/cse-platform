package sn.cse.fournisseurs.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.fournisseurs.domain.model.TypeActionLitige;

/** Entrée de chronologie d'un litige (table {@code actions_litige}). */
@Embeddable
public class ActionLitigeEmbeddable {

  @Column(name = "date_action", nullable = false)
  private Instant date;

  @Column(name = "auteur_id")
  private UUID auteurId;

  @Column(name = "auteur_nom", nullable = false)
  private String auteurNom;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false, columnDefinition = "type_action_litige")
  private TypeActionLitige type;

  @Column(nullable = false)
  private String commentaire;

  /** Constructeur requis par JPA. */
  protected ActionLitigeEmbeddable() {}

  /**
   * @param date date
   * @param auteurId auteur
   * @param auteurNom nom de l'auteur
   * @param type type
   * @param commentaire commentaire
   */
  public ActionLitigeEmbeddable(
      Instant date, UUID auteurId, String auteurNom, TypeActionLitige type, String commentaire) {
    this.date = date;
    this.auteurId = auteurId;
    this.auteurNom = auteurNom;
    this.type = type;
    this.commentaire = commentaire;
  }

  public Instant getDate() {
    return date;
  }

  public UUID getAuteurId() {
    return auteurId;
  }

  public String getAuteurNom() {
    return auteurNom;
  }

  public TypeActionLitige getType() {
    return type;
  }

  public String getCommentaire() {
    return commentaire;
  }
}
