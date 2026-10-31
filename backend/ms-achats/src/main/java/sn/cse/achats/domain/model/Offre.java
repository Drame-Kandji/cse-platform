package sn.cse.achats.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.exception.Regles;

/**
 * Offre d'un fournisseur sur un appel d'offres (entité de l'agrégat {@link AppelOffre}). Seules les
 * notes des critères saisis sont conservées ; PRIX et DELAI sont recalculés à chaque évaluation.
 */
public final class Offre {

  private final UUID id;
  private final String codeAnonyme;
  private final UUID fournisseurId;
  private final String fournisseurNom;
  private final BigDecimal montantPropose;
  private final int delaiLivraisonJours;
  private final Integer garantieMois;
  private final String description;
  private final Instant dateDepot;
  private final Map<CritereEvaluation, Integer> notesSaisies;
  private boolean retenue;

  /**
   * Reconstitution (persistance) ou création interne.
   *
   * @param id identifiant
   * @param codeAnonyme code opaque affiché tant que l'appel d'offres est publié (R10)
   * @param fournisseurId fournisseur
   * @param fournisseurNom raison sociale
   * @param montantPropose montant en FCFA
   * @param delaiLivraisonJours délai en jours
   * @param garantieMois garantie en mois (facultatif)
   * @param description description (facultatif)
   * @param dateDepot instant du dépôt
   * @param notesSaisies notes saisies (critères non calculés)
   * @param retenue vrai si l'offre a remporté le marché
   */
  public Offre(
      UUID id,
      String codeAnonyme,
      UUID fournisseurId,
      String fournisseurNom,
      BigDecimal montantPropose,
      int delaiLivraisonJours,
      Integer garantieMois,
      String description,
      Instant dateDepot,
      Map<CritereEvaluation, Integer> notesSaisies,
      boolean retenue) {
    this.id = Objects.requireNonNull(id, "id");
    this.codeAnonyme = Objects.requireNonNull(codeAnonyme, "codeAnonyme");
    this.fournisseurId = Objects.requireNonNull(fournisseurId, "fournisseurId");
    this.fournisseurNom = Objects.requireNonNull(fournisseurNom, "fournisseurNom");
    this.montantPropose = Regles.montantPositif(montantPropose, "montantPropose");
    Regles.exiger(
        delaiLivraisonJours >= 1,
        CodesErreur.DONNEE_INVALIDE,
        "Le délai de livraison doit être d'au moins un jour.");
    Regles.exiger(
        garantieMois == null || garantieMois >= 0,
        CodesErreur.DONNEE_INVALIDE,
        "La garantie ne peut pas être négative.");
    this.delaiLivraisonJours = delaiLivraisonJours;
    this.garantieMois = garantieMois;
    this.description = Regles.facultatif(description);
    this.dateDepot = Objects.requireNonNull(dateDepot, "dateDepot");
    this.notesSaisies = new EnumMap<>(CritereEvaluation.class);
    if (notesSaisies != null) {
      this.notesSaisies.putAll(notesSaisies);
    }
    this.retenue = retenue;
  }

  /**
   * Enregistre (ou remplace) des notes saisies, après validation par l'agrégat.
   *
   * @param notes notes 0..100 par critère non calculé
   */
  void noter(Map<CritereEvaluation, Integer> notes) {
    notesSaisies.putAll(notes);
  }

  void retenir() {
    this.retenue = true;
  }

  /**
   * @param critere critère saisi
   * @return la note saisie, si elle existe
   */
  public Optional<Integer> noteSaisie(CritereEvaluation critere) {
    return Optional.ofNullable(notesSaisies.get(critere));
  }

  public UUID getId() {
    return id;
  }

  public String getCodeAnonyme() {
    return codeAnonyme;
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public String getFournisseurNom() {
    return fournisseurNom;
  }

  public BigDecimal getMontantPropose() {
    return montantPropose;
  }

  public int getDelaiLivraisonJours() {
    return delaiLivraisonJours;
  }

  public Integer getGarantieMois() {
    return garantieMois;
  }

  public String getDescription() {
    return description;
  }

  public Instant getDateDepot() {
    return dateDepot;
  }

  public Map<CritereEvaluation, Integer> getNotesSaisies() {
    return Collections.unmodifiableMap(notesSaisies);
  }

  public boolean isRetenue() {
    return retenue;
  }
}
