package sn.cse.besoins.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.besoins.domain.model.ActionHistorique;
import sn.cse.besoins.domain.model.StatutBesoin;

/** Entrée d'historique persistée (immuable : jamais mise à jour). */
@Entity
@Table(name = "historiques_besoins")
class HistoriqueBesoinEntity {

  @Id UUID id;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "statut_besoin", nullable = false, updatable = false)
  StatutBesoin statut;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "action_historique_besoin", nullable = false, updatable = false)
  ActionHistorique action;

  @Column(name = "etape_validation", length = 60, updatable = false)
  String etapeValidation;

  @Column(name = "auteur_id", updatable = false)
  UUID auteurId;

  @Column(name = "auteur_nom", length = 150, updatable = false)
  String auteurNom;

  @Column(length = 2000, updatable = false)
  String commentaire;

  @Column(name = "date_action", nullable = false, updatable = false)
  Instant date;
}
