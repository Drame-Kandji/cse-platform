package sn.cse.besoins.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;

/** Projection JPA de l'agrégat besoin (aucune logique métier : voir {@code BesoinMateriel}). */
@Entity
@Table(name = "besoins_materiels")
class BesoinMaterielEntity {

  private static final int LOT = 50;

  @Id UUID id;

  @Column(nullable = false, unique = true, length = 30, updatable = false)
  String reference;

  @Column(name = "chantier_id", nullable = false)
  UUID chantierId;

  @Column(name = "chantier_nom", length = 200)
  String chantierNom;

  @Column(name = "demandeur_id", updatable = false)
  UUID demandeurId;

  @Column(name = "demandeur_nom", length = 150, updatable = false)
  String demandeurNom;

  @Column(nullable = false, length = 200)
  String designation;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "categorie_materiel", nullable = false)
  CategorieMateriel categorie;

  @Column(name = "type_engin_id")
  UUID typeEnginId;

  @Column(nullable = false)
  int quantite;

  @Column(length = 2000)
  String specifications;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "priorite_besoin", nullable = false)
  Priorite priorite;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "statut_besoin", nullable = false)
  StatutBesoin statut;

  @Column(name = "duree_utilisation_jours", nullable = false)
  int dureeUtilisationJours;

  @Column(name = "date_besoin", nullable = false)
  LocalDate dateBesoin;

  @Column(length = 2000)
  String justification;

  @Column(name = "motif_rejet", length = 1000)
  String motifRejet;

  @Column(name = "validateur_id")
  UUID validateurId;

  @Column(name = "validateur_nom", length = 150)
  String validateurNom;

  @Column(name = "date_validation")
  Instant dateValidation;

  @Column(name = "orientation_en_attente", nullable = false)
  boolean orientationEnAttente;

  @ElementCollection
  @CollectionTable(
      name = "besoins_materiels_affectes",
      joinColumns = @JoinColumn(name = "besoin_id"))
  @OrderColumn(name = "ordre")
  @Column(name = "materiel_id", nullable = false)
  @BatchSize(size = LOT)
  List<UUID> materielIdsAffectes = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "besoin_id", nullable = false, updatable = false)
  @OrderBy("date ASC")
  @BatchSize(size = LOT)
  List<HistoriqueBesoinEntity> historique = new ArrayList<>();

  @Version Long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "created_by", length = 100, updatable = false)
  String createdBy;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Column(name = "updated_by", length = 100)
  String updatedBy;
}
