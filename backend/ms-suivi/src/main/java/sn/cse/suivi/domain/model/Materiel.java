package sn.cse.suivi.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import sn.cse.suivi.domain.exception.SuiviErreurs;

/**
 * Matériel du parc CSE. Porte son cycle de vie ({@link EtatMateriel}), son compteur horaire et la
 * politique de maintenance préventive (seuils hérités de son type d'engin).
 */
public final class Materiel {

  private final UUID id;
  private DescriptionMateriel description;
  private EtatMateriel etat;
  private long heuresFonctionnement;
  private long heuresDerniereMaintenance;
  private LocalDate dateReferenceMaintenance;
  private Integer seuilMaintenanceHeures;
  private Integer seuilMaintenanceJours;
  private LocalDate dateDerniereAlerteMaintenance;
  private UUID chantierActuelId;
  private Coordonnees position;
  private final UUID fournisseurId;
  private final UUID contratId;
  private boolean reformeDemandee;
  private String motifReforme;
  private final Long version;

  /**
   * État persistant complet d'un matériel.
   *
   * @param id identifiant
   * @param description caractéristiques descriptives
   * @param etat état courant
   * @param heuresFonctionnement compteur horaire
   * @param heuresDerniereMaintenance compteur lors de la dernière maintenance
   * @param dateReferenceMaintenance date de la dernière maintenance (ou de mise en service)
   * @param seuilMaintenanceHeures seuil horaire de maintenance préventive
   * @param seuilMaintenanceJours seuil calendaire de maintenance préventive
   * @param dateDerniereAlerteMaintenance dernière alerte émise
   * @param chantierActuelId chantier d'affectation courant
   * @param position dernière position GPS connue
   * @param fournisseurId fournisseur d'origine
   * @param contratId contrat d'achat d'origine
   * @param reformeDemandee demande de réforme en attente de décision DG
   * @param motifReforme motif de la (demande de) réforme
   * @param version version de verrou optimiste
   */
  public record Snapshot(
      UUID id,
      DescriptionMateriel description,
      EtatMateriel etat,
      long heuresFonctionnement,
      long heuresDerniereMaintenance,
      LocalDate dateReferenceMaintenance,
      Integer seuilMaintenanceHeures,
      Integer seuilMaintenanceJours,
      LocalDate dateDerniereAlerteMaintenance,
      UUID chantierActuelId,
      Coordonnees position,
      UUID fournisseurId,
      UUID contratId,
      boolean reformeDemandee,
      String motifReforme,
      Long version) {}

  /**
   * Origine d'un matériel livré dans le cadre d'un marché.
   *
   * @param fournisseurId fournisseur
   * @param contratId contrat d'achat
   */
  public record Origine(UUID fournisseurId, UUID contratId) {

    /** Matériel sans marché associé (saisie directe). */
    public static final Origine AUCUNE = new Origine(null, null);
  }

  private Materiel(Snapshot s) {
    this.id = Objects.requireNonNull(s.id(), "id");
    this.description = Objects.requireNonNull(s.description(), "description");
    this.etat = Objects.requireNonNull(s.etat(), "etat");
    this.heuresFonctionnement = s.heuresFonctionnement();
    this.heuresDerniereMaintenance = s.heuresDerniereMaintenance();
    this.dateReferenceMaintenance =
        Objects.requireNonNull(s.dateReferenceMaintenance(), "dateReferenceMaintenance");
    this.seuilMaintenanceHeures = s.seuilMaintenanceHeures();
    this.seuilMaintenanceJours = s.seuilMaintenanceJours();
    this.dateDerniereAlerteMaintenance = s.dateDerniereAlerteMaintenance();
    this.chantierActuelId = s.chantierActuelId();
    this.position = s.position();
    this.fournisseurId = s.fournisseurId();
    this.contratId = s.contratId();
    this.reformeDemandee = s.reformeDemandee();
    this.motifReforme = s.motifReforme();
    this.version = s.version();
  }

  /**
   * Enregistre un nouveau matériel dans le parc ({@code DISPONIBLE}).
   *
   * @param description caractéristiques
   * @param type type d'engin (seuils de maintenance), peut être {@code null}
   * @param heuresInitiales compteur horaire à l'entrée dans le parc
   * @param origine fournisseur / contrat d'origine
   * @param aujourdhui date du jour
   * @return le matériel
   */
  public static Materiel enregistrer(
      DescriptionMateriel description,
      TypeEngin type,
      long heuresInitiales,
      Origine origine,
      LocalDate aujourdhui) {
    return enregistrer(UUID.randomUUID(), description, type, heuresInitiales, origine, aujourdhui);
  }

  /**
   * Variante à identifiant imposé (référentiel de démonstration partagé entre services).
   *
   * @param id identifiant
   * @param description caractéristiques
   * @param type type d'engin, peut être {@code null}
   * @param heuresInitiales compteur horaire initial
   * @param origine fournisseur / contrat d'origine
   * @param aujourdhui date du jour
   * @return le matériel
   */
  public static Materiel enregistrer(
      UUID id,
      DescriptionMateriel description,
      TypeEngin type,
      long heuresInitiales,
      Origine origine,
      LocalDate aujourdhui) {
    if (heuresInitiales < 0) {
      throw SuiviErreurs.regle(
          "RELEVE_HEURES_INVALIDE", "Le compteur horaire ne peut pas être négatif.");
    }
    verifierType(description, type);
    LocalDate reference =
        description.dateAcquisition() == null ? aujourdhui : description.dateAcquisition();
    return new Materiel(
        new Snapshot(
            id,
            description,
            EtatMateriel.DISPONIBLE,
            heuresInitiales,
            heuresInitiales,
            reference,
            type == null ? null : type.getDonnees().seuilMaintenanceHeures(),
            type == null ? null : type.getDonnees().seuilMaintenanceJours(),
            null,
            null,
            null,
            origine.fournisseurId(),
            origine.contratId(),
            false,
            null,
            null));
  }

  /**
   * @param snapshot état persistant
   * @return le matériel reconstitué
   */
  public static Materiel reconstituer(Snapshot snapshot) {
    return new Materiel(snapshot);
  }

  /**
   * Met à jour les caractéristiques descriptives.
   *
   * @param nouvelle nouvelles caractéristiques
   * @param type type d'engin (peut être {@code null})
   * @param versionAttendue version lue par le client
   */
  public void modifier(DescriptionMateriel nouvelle, TypeEngin type, long versionAttendue) {
    if (version == null || version != versionAttendue) {
      throw SuiviErreurs.versionObsolete("Le matériel");
    }
    verifierNonReforme();
    verifierType(nouvelle, type);
    boolean typeChange = !Objects.equals(description.typeEnginId(), nouvelle.typeEnginId());
    this.description = nouvelle;
    if (typeChange) {
      appliquerSeuils(
          type == null ? null : type.getDonnees().seuilMaintenanceHeures(),
          type == null ? null : type.getDonnees().seuilMaintenanceJours());
    }
  }

  /**
   * Applique les seuils de maintenance préventive (modification du type d'engin).
   *
   * @param heures seuil horaire
   * @param jours seuil calendaire
   */
  public void appliquerSeuils(Integer heures, Integer jours) {
    this.seuilMaintenanceHeures = heures;
    this.seuilMaintenanceJours = jours;
  }

  /**
   * Affecte le matériel au chantier. Règle R04 : seul un matériel {@code DISPONIBLE} peut être
   * affecté à un nouveau chantier.
   *
   * @param chantier chantier de destination
   */
  public void affecter(Chantier chantier) {
    verifierAffectable();
    if (etat != EtatMateriel.DISPONIBLE) {
      throw SuiviErreurs.materielNonDisponible(code(), "son état est " + etat);
    }
    etat.verifierTransition(EtatMateriel.AFFECTE);
    this.etat = EtatMateriel.AFFECTE;
    this.chantierActuelId = chantier.getId();
    this.position = chantier.getCoordonnees();
  }

  /**
   * Refuse l'affectation d'un matériel immobilisé, réformé ou en instance de réforme (R04, R13),
   * indépendamment de toute autre affectation.
   */
  public void verifierAffectable() {
    if (etat == EtatMateriel.REFORME || etat == EtatMateriel.EN_MAINTENANCE) {
      throw SuiviErreurs.materielNonDisponible(code(), "son état est " + etat);
    }
    if (reformeDemandee) {
      throw SuiviErreurs.materielNonDisponible(code(), "une demande de réforme est en cours");
    }
  }

  /**
   * Retour au parc à la fin d'une affectation. Un matériel en maintenance reste immobilisé : la
   * clôture de l'ordre de travail le rendra disponible.
   */
  public void liberer() {
    if (etat == EtatMateriel.AFFECTE) {
      etat.verifierTransition(EtatMateriel.DISPONIBLE);
      this.etat = EtatMateriel.DISPONIBLE;
    }
    this.chantierActuelId = null;
  }

  /** Immobilise le matériel pour une intervention (sans effet s'il l'est déjà). */
  public void mettreEnMaintenance() {
    verifierNonReforme();
    if (etat != EtatMateriel.EN_MAINTENANCE) {
      etat.verifierTransition(EtatMateriel.EN_MAINTENANCE);
      this.etat = EtatMateriel.EN_MAINTENANCE;
    }
  }

  /**
   * Remet le matériel en service après maintenance : retour sur chantier si une affectation est en
   * cours, sinon au parc.
   *
   * @param affectationActive vrai si une affectation non terminée existe
   */
  public void remettreEnService(boolean affectationActive) {
    if (etat != EtatMateriel.EN_MAINTENANCE) {
      return;
    }
    EtatMateriel cible = affectationActive ? EtatMateriel.AFFECTE : EtatMateriel.DISPONIBLE;
    etat.verifierTransition(cible);
    this.etat = cible;
  }

  /**
   * Changement d'état manuel par le Responsable Matériel : seule l'entrée en maintenance ou la
   * remise en service ({@code DISPONIBLE}) sont permises ; l'affectation et la réforme suivent leur
   * propre procédure.
   *
   * @param cible état demandé
   * @param contexte situation opérationnelle
   */
  public void changerEtat(EtatMateriel cible, ContexteMateriel contexte) {
    verifierNonReforme();
    if (cible != EtatMateriel.DISPONIBLE && cible != EtatMateriel.EN_MAINTENANCE) {
      throw SuiviErreurs.regle(
          "ETAT_CIBLE_INTERDIT",
          "Seuls les états DISPONIBLE et EN_MAINTENANCE peuvent être demandés ici ; utilisez"
              + " l'affectation ou la procédure de réforme.");
    }
    if (cible == etat) {
      throw SuiviErreurs.transitionInvalide("le matériel", etat, cible);
    }
    if (cible == EtatMateriel.EN_MAINTENANCE) {
      mettreEnMaintenance();
      return;
    }
    if (etat == EtatMateriel.AFFECTE) {
      throw SuiviErreurs.regle(
          "AFFECTATION_EN_COURS",
          "Le matériel "
              + code()
              + " est affecté : utilisez la libération pour le rendre au parc.");
    }
    if (contexte.maintenanceEnCours()) {
      throw SuiviErreurs.regle(
          "MAINTENANCE_EN_COURS",
          "Un ordre de travail est en cours sur " + code() + " : clôturez-le d'abord.");
    }
    remettreEnService(contexte.affectationActive());
  }

  /**
   * Relevé du compteur horaire.
   *
   * @param heures nouvelle valeur du compteur
   */
  public void releverHeures(long heures) {
    verifierNonReforme();
    if (heures < heuresFonctionnement) {
      throw SuiviErreurs.regle(
          "RELEVE_HEURES_INVALIDE",
          "Le relevé ("
              + heures
              + " h) est inférieur au compteur actuel ("
              + heuresFonctionnement
              + " h).");
    }
    this.heuresFonctionnement = heures;
  }

  /**
   * Réinitialise le cycle de maintenance préventive après une intervention clôturée.
   *
   * @param date date de clôture
   */
  public void enregistrerMaintenance(LocalDate date) {
    this.heuresDerniereMaintenance = heuresFonctionnement;
    this.dateReferenceMaintenance = date;
    this.dateDerniereAlerteMaintenance = null;
  }

  /**
   * Étape 1 de la réforme (R13) : demande motivée du Responsable Matériel.
   *
   * @param motif justification
   */
  public void demanderReforme(String motif) {
    verifierNonReforme();
    if (reformeDemandee) {
      throw SuiviErreurs.conflit(
          "REFORME_DEJA_DEMANDEE", "Une demande de réforme est déjà en attente pour " + code());
    }
    if (motif == null || motif.isBlank()) {
      throw SuiviErreurs.regle("MOTIF_OBLIGATOIRE", "Le motif de réforme est obligatoire.");
    }
    this.reformeDemandee = true;
    this.motifReforme = motif;
  }

  /**
   * Étape 2 de la réforme (R13) : décision de la Direction Générale. Un matériel réformé quitte
   * définitivement le parc ({@code REFORME} est terminal).
   *
   * @param approuve décision
   * @param contexte situation opérationnelle (le matériel doit être libre)
   */
  public void deciderReforme(boolean approuve, ContexteMateriel contexte) {
    verifierNonReforme();
    if (!reformeDemandee) {
      throw SuiviErreurs.conflit(
          "REFORME_NON_DEMANDEE", "Aucune demande de réforme n'est en attente pour " + code());
    }
    if (!approuve) {
      this.reformeDemandee = false;
      this.motifReforme = null;
      return;
    }
    if (contexte.affectationActive() || contexte.maintenanceEnCours()) {
      throw SuiviErreurs.regle(
          "REFORME_IMPOSSIBLE",
          "Le matériel "
              + code()
              + " doit être libéré et hors maintenance avant d'être réformé (R13).");
    }
    etat.verifierTransition(EtatMateriel.REFORME);
    this.etat = EtatMateriel.REFORME;
    this.reformeDemandee = false;
    this.chantierActuelId = null;
  }

  /**
   * @return heures de fonctionnement depuis la dernière maintenance
   */
  public long heuresDepuisMaintenance() {
    return heuresFonctionnement - heuresDerniereMaintenance;
  }

  /**
   * @return date de la prochaine maintenance préventive calendaire, si un seuil en jours existe
   */
  public LocalDate prochaineMaintenance() {
    return seuilMaintenanceJours == null
        ? null
        : dateReferenceMaintenance.plusDays(seuilMaintenanceJours);
  }

  /**
   * @param aujourdhui date du jour
   * @return vrai si un seuil de maintenance préventive (heures ou jours) est atteint
   */
  public boolean estMaintenanceDue(LocalDate aujourdhui) {
    if (etat == EtatMateriel.REFORME) {
      return false;
    }
    boolean heuresAtteintes =
        seuilMaintenanceHeures != null && heuresDepuisMaintenance() >= seuilMaintenanceHeures;
    LocalDate prochaine = prochaineMaintenance();
    boolean dateAtteinte = prochaine != null && !aujourdhui.isBefore(prochaine);
    return heuresAtteintes || dateAtteinte;
  }

  /**
   * Évite de répéter l'alerte de maintenance à chaque passage du job.
   *
   * @param aujourdhui date du jour
   * @param delaiRappelJours délai minimal entre deux alertes
   * @return vrai si une alerte doit être émise
   */
  public boolean doitEtreAlerte(LocalDate aujourdhui, int delaiRappelJours) {
    return estMaintenanceDue(aujourdhui)
        && (dateDerniereAlerteMaintenance == null
            || !aujourdhui.isBefore(dateDerniereAlerteMaintenance.plusDays(delaiRappelJours)));
  }

  /**
   * @param aujourdhui date de l'alerte émise
   */
  public void marquerAlerteMaintenance(LocalDate aujourdhui) {
    this.dateDerniereAlerteMaintenance = aujourdhui;
  }

  /**
   * @param nouvelle position GPS (balise ou saisie)
   */
  public void localiser(Coordonnees nouvelle) {
    this.position = nouvelle;
  }

  private void verifierNonReforme() {
    if (etat == EtatMateriel.REFORME) {
      throw SuiviErreurs.materielReforme(code());
    }
  }

  private static void verifierType(DescriptionMateriel description, TypeEngin type) {
    if (type == null) {
      if (description.typeEnginId() != null) {
        throw new IllegalArgumentException("Type d'engin attendu : " + description.typeEnginId());
      }
      return;
    }
    if (!type.getId().equals(description.typeEnginId())) {
      throw new IllegalArgumentException("Type d'engin incohérent avec la description");
    }
    type.verifierCompatible(description.categorie());
  }

  /**
   * @return l'état persistant
   */
  public Snapshot snapshot() {
    return new Snapshot(
        id,
        description,
        etat,
        heuresFonctionnement,
        heuresDerniereMaintenance,
        dateReferenceMaintenance,
        seuilMaintenanceHeures,
        seuilMaintenanceJours,
        dateDerniereAlerteMaintenance,
        chantierActuelId,
        position,
        fournisseurId,
        contratId,
        reformeDemandee,
        motifReforme,
        version);
  }

  private String code() {
    return description.code();
  }

  public UUID getId() {
    return id;
  }

  public DescriptionMateriel getDescription() {
    return description;
  }

  public String getCode() {
    return description.code();
  }

  public String getDesignation() {
    return description.designation();
  }

  public CategorieMateriel getCategorie() {
    return description.categorie();
  }

  public BigDecimal getTarifJournalier() {
    return description.tarifJournalier();
  }

  public EtatMateriel getEtat() {
    return etat;
  }

  public long getHeuresFonctionnement() {
    return heuresFonctionnement;
  }

  public Integer getSeuilMaintenanceHeures() {
    return seuilMaintenanceHeures;
  }

  public Integer getSeuilMaintenanceJours() {
    return seuilMaintenanceJours;
  }

  public UUID getChantierActuelId() {
    return chantierActuelId;
  }

  public Coordonnees getPosition() {
    return position;
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public UUID getContratId() {
    return contratId;
  }

  public boolean isReformeDemandee() {
    return reformeDemandee;
  }

  public String getMotifReforme() {
    return motifReforme;
  }

  public Long getVersion() {
    return version;
  }
}
