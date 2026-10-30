package sn.cse.fournisseurs.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.fournisseurs.domain.exception.CodesErreur;

/**
 * Agrégat « Litige » : différend avec un fournisseur, suivi par une chronologie d'actions jusqu'à
 * sa résolution ou son classement.
 */
public final class Litige {

  private final UUID id;
  private final String reference;
  private final UUID fournisseurId;
  private final String raisonSociale;
  private final UUID contratId;
  private final String objet;
  private final String description;
  private final GraviteLitige gravite;
  private StatutLitige statut;
  private final Instant dateOuverture;
  private Instant dateResolution;
  private final List<ActionLitige> actions;
  private final long version;

  private Litige(EtatLitige etat) {
    this.id = Objects.requireNonNull(etat.id(), "id");
    this.reference = Objects.requireNonNull(etat.reference(), "reference");
    this.fournisseurId = Objects.requireNonNull(etat.fournisseurId(), "fournisseurId");
    this.raisonSociale = Objects.requireNonNull(etat.raisonSociale(), "raisonSociale");
    this.contratId = etat.contratId();
    this.objet = texteObligatoire(etat.objet(), "objet");
    this.description = texteObligatoire(etat.description(), "description");
    this.gravite = Objects.requireNonNull(etat.gravite(), "gravite");
    this.statut = Objects.requireNonNull(etat.statut(), "statut");
    this.dateOuverture = Objects.requireNonNull(etat.dateOuverture(), "dateOuverture");
    this.dateResolution = etat.dateResolution();
    this.actions = new ArrayList<>(etat.actions() == null ? List.of() : etat.actions());
    this.version = etat.version();
  }

  /**
   * Ouvre un litige : statut {@code OUVERT} et première entrée {@code OUVERTURE} dans la
   * chronologie.
   *
   * @param reference référence lisible (LIT-2026-001)
   * @param fournisseur fournisseur concerné
   * @param contratId contrat concerné (optionnel)
   * @param objet objet
   * @param description description détaillée
   * @param gravite gravité
   * @param auteur auteur de l'ouverture
   * @param date instant d'ouverture
   * @return litige ouvert
   */
  public static Litige ouvrir(
      String reference,
      Fournisseur fournisseur,
      UUID contratId,
      String objet,
      String description,
      GraviteLitige gravite,
      Auteur auteur,
      Instant date) {
    String objetValide = texteObligatoire(objet, "objet");
    ActionLitige ouverture =
        new ActionLitige(
            date,
            auteur.id(),
            auteur.nom(),
            TypeActionLitige.OUVERTURE,
            "Ouverture du litige : " + objetValide);
    return new Litige(
        new EtatLitige(
            UUID.randomUUID(),
            reference,
            fournisseur.getId(),
            fournisseur.getProfil().raisonSociale(),
            contratId,
            objetValide,
            description,
            gravite,
            StatutLitige.OUVERT,
            date,
            null,
            List.of(ouverture),
            0L));
  }

  /**
   * Reconstitue l'agrégat depuis la persistance.
   *
   * @param etat état persistant
   * @return litige
   */
  public static Litige reconstituer(EtatLitige etat) {
    return new Litige(etat);
  }

  /**
   * Ajoute une action à la chronologie et applique la transition correspondante : {@code ACTION →
   * EN_COURS}, {@code RESOLUTION → RESOLU}, {@code CLASSEMENT → CLASSE}.
   *
   * @param type nature de l'action (OUVERTURE interdite)
   * @param commentaire commentaire obligatoire
   * @param auteur auteur
   * @param date instant
   * @return {@code true} si le litige vient d'être clos (résolu ou classé)
   */
  public boolean agir(TypeActionLitige type, String commentaire, Auteur auteur, Instant date) {
    StatutLitige cible =
        switch (Objects.requireNonNull(type, "type")) {
          case ACTION -> StatutLitige.EN_COURS;
          case RESOLUTION -> StatutLitige.RESOLU;
          case CLASSEMENT -> StatutLitige.CLASSE;
          case OUVERTURE ->
              throw new BusinessRuleException(
                  CodesErreur.ACTION_LITIGE_INVALIDE,
                  "L'action OUVERTURE est réservée à la création du litige.");
        };
    String commentaireValide = texteObligatoire(commentaire, "commentaire");
    if (!statut.peutPasserA(cible)) {
      throw new ConflictException(
          CodesErreur.LITIGE_CLOS,
          "Le litige " + reference + " est clos (" + statut + ") et n'accepte plus d'action.");
    }
    actions.add(new ActionLitige(date, auteur.id(), auteur.nom(), type, commentaireValide));
    statut = cible;
    if (!cible.estOuvert()) {
      dateResolution = date;
      return true;
    }
    return false;
  }

  private static String texteObligatoire(String valeur, String champ) {
    if (valeur == null || valeur.isBlank()) {
      throw new BusinessRuleException(
          CodesErreur.DONNEE_INVALIDE, "Le champ « " + champ + " » est obligatoire.");
    }
    return valeur.trim();
  }

  public UUID getId() {
    return id;
  }

  public String getReference() {
    return reference;
  }

  public UUID getFournisseurId() {
    return fournisseurId;
  }

  public String getRaisonSociale() {
    return raisonSociale;
  }

  public Optional<UUID> getContratId() {
    return Optional.ofNullable(contratId);
  }

  public String getObjet() {
    return objet;
  }

  public String getDescription() {
    return description;
  }

  public GraviteLitige getGravite() {
    return gravite;
  }

  public StatutLitige getStatut() {
    return statut;
  }

  public Instant getDateOuverture() {
    return dateOuverture;
  }

  public Optional<Instant> getDateResolution() {
    return Optional.ofNullable(dateResolution);
  }

  public List<ActionLitige> getActions() {
    return List.copyOf(actions);
  }

  public long getVersion() {
    return version;
  }

  /**
   * État persistant complet d'un litige.
   *
   * @param id identifiant
   * @param reference référence lisible
   * @param fournisseurId fournisseur
   * @param raisonSociale raison sociale (instantané à l'ouverture)
   * @param contratId contrat (optionnel)
   * @param objet objet
   * @param description description
   * @param gravite gravité
   * @param statut statut
   * @param dateOuverture ouverture
   * @param dateResolution résolution ou classement
   * @param actions chronologie
   * @param version version
   */
  public record EtatLitige(
      UUID id,
      String reference,
      UUID fournisseurId,
      String raisonSociale,
      UUID contratId,
      String objet,
      String description,
      GraviteLitige gravite,
      StatutLitige statut,
      Instant dateOuverture,
      Instant dateResolution,
      List<ActionLitige> actions,
      long version) {}
}
