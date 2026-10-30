package sn.cse.suivi.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import sn.cse.suivi.domain.exception.SuiviErreurs;

/** Chantier de la CSE : lieu d'affectation des matériels. */
public final class Chantier {

  private final UUID id;
  private String code;
  private String nom;
  private String localisation;
  private String region;
  private Coordonnees coordonnees;
  private StatutChantier statut;
  private BigDecimal budget;
  private BigDecimal montantMarche;
  private LocalDate dateDebut;
  private LocalDate dateFinPrevue;
  private UUID chefChantierId;
  private String chefChantierNom;
  private final Long version;

  /**
   * État persistant complet d'un chantier (reconstitution depuis le stockage).
   *
   * @param id identifiant
   * @param donnees données saisissables
   * @param statut statut
   * @param version version de verrou optimiste ({@code null} si jamais enregistré)
   */
  public record Snapshot(UUID id, DonneesChantier donnees, StatutChantier statut, Long version) {}

  private Chantier(Snapshot s) {
    this.id = Objects.requireNonNull(s.id(), "id");
    this.statut = Objects.requireNonNull(s.statut(), "statut");
    this.version = s.version();
    appliquer(s.donnees());
  }

  /**
   * Ouvre un nouveau chantier ({@code EN_COURS}).
   *
   * @param donnees données saisies
   * @return le chantier
   */
  public static Chantier ouvrir(DonneesChantier donnees) {
    return ouvrir(UUID.randomUUID(), donnees);
  }

  /**
   * Ouvre un chantier avec un identifiant imposé (référentiel partagé entre services).
   *
   * @param id identifiant
   * @param donnees données saisies
   * @return le chantier
   */
  public static Chantier ouvrir(UUID id, DonneesChantier donnees) {
    return new Chantier(new Snapshot(id, donnees, StatutChantier.EN_COURS, null));
  }

  /**
   * @param snapshot état persistant
   * @return le chantier reconstitué
   */
  public static Chantier reconstituer(Snapshot snapshot) {
    return new Chantier(snapshot);
  }

  /**
   * Met à jour les données d'un chantier encore en cours.
   *
   * @param donnees nouvelles données
   * @param versionAttendue version lue par le client
   */
  public void modifier(DonneesChantier donnees, long versionAttendue) {
    verifierVersion(versionAttendue);
    if (statut == StatutChantier.TERMINE) {
      throw SuiviErreurs.conflit(
          "CHANTIER_TERMINE", "Le chantier " + code + " est terminé : il n'est plus modifiable.");
    }
    appliquer(donnees);
  }

  /**
   * Clôture le chantier.
   *
   * @param materielsEncoreAffectes nombre d'affectations actives sur le chantier
   */
  public void terminer(long materielsEncoreAffectes) {
    if (statut == StatutChantier.TERMINE) {
      throw SuiviErreurs.transitionInvalide("le chantier", statut, StatutChantier.TERMINE);
    }
    if (materielsEncoreAffectes > 0) {
      throw SuiviErreurs.regle(
          "CHANTIER_MATERIELS_AFFECTES",
          "Impossible de terminer le chantier "
              + code
              + " : "
              + materielsEncoreAffectes
              + " matériel(s) y sont encore affectés. Libérez-les d'abord.");
    }
    statut = StatutChantier.TERMINE;
  }

  /** Refuse toute nouvelle affectation sur un chantier terminé. */
  public void verifierOuvertAuxAffectations() {
    if (statut != StatutChantier.EN_COURS) {
      throw SuiviErreurs.regle(
          "CHANTIER_TERMINE",
          "Le chantier " + code + " est terminé : aucune affectation n'est possible.");
    }
  }

  private void verifierVersion(long versionAttendue) {
    if (version == null || version != versionAttendue) {
      throw SuiviErreurs.versionObsolete("Le chantier");
    }
  }

  private void appliquer(DonneesChantier d) {
    Objects.requireNonNull(d, "donnees");
    if (d.dateFinPrevue() != null) {
      new Periode(d.dateDebut(), d.dateFinPrevue());
    }
    if (d.budget().signum() < 0 || d.montantMarche().signum() < 0) {
      throw SuiviErreurs.regle(
          "MONTANT_INVALIDE", "Le budget et le montant du marché doivent être positifs.");
    }
    this.code = d.code();
    this.nom = d.nom();
    this.localisation = d.localisation();
    this.region = d.region();
    this.coordonnees = Objects.requireNonNull(d.coordonnees(), "coordonnees");
    this.budget = d.budget();
    this.montantMarche = d.montantMarche();
    this.dateDebut = d.dateDebut();
    this.dateFinPrevue = d.dateFinPrevue();
    this.chefChantierId = d.chefChantierId();
    this.chefChantierNom = d.chefChantierNom();
  }

  /**
   * @return l'état persistant
   */
  public Snapshot snapshot() {
    return new Snapshot(id, donnees(), statut, version);
  }

  /**
   * @return les données saisissables courantes
   */
  public DonneesChantier donnees() {
    return new DonneesChantier(
        code,
        nom,
        localisation,
        region,
        coordonnees,
        budget,
        montantMarche,
        dateDebut,
        dateFinPrevue,
        chefChantierId,
        chefChantierNom);
  }

  /**
   * @return vrai si le chantier est en cours
   */
  public boolean estEnCours() {
    return statut == StatutChantier.EN_COURS;
  }

  public UUID getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getNom() {
    return nom;
  }

  public String getLocalisation() {
    return localisation;
  }

  public String getRegion() {
    return region;
  }

  public Coordonnees getCoordonnees() {
    return coordonnees;
  }

  public StatutChantier getStatut() {
    return statut;
  }

  public BigDecimal getBudget() {
    return budget;
  }

  public BigDecimal getMontantMarche() {
    return montantMarche;
  }

  public LocalDate getDateDebut() {
    return dateDebut;
  }

  public LocalDate getDateFinPrevue() {
    return dateFinPrevue;
  }

  public UUID getChefChantierId() {
    return chefChantierId;
  }

  public String getChefChantierNom() {
    return chefChantierNom;
  }

  public Long getVersion() {
    return version;
  }
}
