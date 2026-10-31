package sn.cse.chatbot.domain.model;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Données récupérées pour répondre, section par section, avec la trace des sections indisponibles
 * (service en panne) et masquées (rôle insuffisant).
 *
 * @param materiels matériels
 * @param besoins besoins
 * @param contrats contrats
 * @param appelsOffres appels d'offres
 * @param fournisseurs fournisseurs
 * @param maintenances maintenances
 * @param affectations affectations
 * @param chantiers chantiers
 * @param procedures procédures
 * @param indisponibles sections non obtenues pour cause technique
 * @param masquees sections retirées par le contrôle d'accès
 */
public record ContexteDonnees(
    List<MaterielInfo> materiels,
    List<BesoinInfo> besoins,
    List<ContratInfo> contrats,
    List<AppelOffreInfo> appelsOffres,
    List<FournisseurInfo> fournisseurs,
    List<MaintenanceInfo> maintenances,
    List<AffectationInfo> affectations,
    List<ChantierInfo> chantiers,
    List<Procedure> procedures,
    Set<Section> indisponibles,
    Set<Section> masquees) {

  public ContexteDonnees {
    materiels = List.copyOf(materiels);
    besoins = List.copyOf(besoins);
    contrats = List.copyOf(contrats);
    appelsOffres = List.copyOf(appelsOffres);
    fournisseurs = List.copyOf(fournisseurs);
    maintenances = List.copyOf(maintenances);
    affectations = List.copyOf(affectations);
    chantiers = List.copyOf(chantiers);
    procedures = List.copyOf(procedures);
    indisponibles = indisponibles.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(indisponibles));
    masquees = masquees.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(masquees));
  }

  /**
   * @return un contexte vide
   */
  public static ContexteDonnees vide() {
    return new ContexteDonnees(
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
        List.of(), Set.of(), Set.of());
  }

  /**
   * @return vrai si aucune donnée métier n'a été trouvée
   */
  public boolean estVide() {
    return materiels.isEmpty()
        && besoins.isEmpty()
        && contrats.isEmpty()
        && appelsOffres.isEmpty()
        && fournisseurs.isEmpty()
        && maintenances.isEmpty()
        && affectations.isEmpty()
        && chantiers.isEmpty()
        && procedures.isEmpty();
  }

  /**
   * Retire une section (données vidées, section marquée masquée).
   *
   * @param section section à retirer
   * @return le contexte filtré
   */
  public ContexteDonnees masquer(Section section) {
    Set<Section> m = EnumSet.noneOf(Section.class);
    m.addAll(masquees);
    m.add(section);
    return new ContexteDonnees(
        section == Section.MATERIELS ? List.of() : materiels,
        section == Section.BESOINS ? List.of() : besoins,
        section == Section.CONTRATS ? List.of() : contrats,
        section == Section.APPELS_OFFRES ? List.of() : appelsOffres,
        section == Section.FOURNISSEURS ? List.of() : fournisseurs,
        section == Section.MAINTENANCES ? List.of() : maintenances,
        section == Section.AFFECTATIONS ? List.of() : affectations,
        section == Section.CHANTIERS ? List.of() : chantiers,
        section == Section.PROCEDURES ? List.of() : procedures,
        indisponibles,
        m);
  }

  /**
   * @return le contexte sans aucun montant (contrats, appels d'offres, coûts de maintenance)
   */
  public ContexteDonnees sansMontants() {
    return new ContexteDonnees(
        materiels,
        besoins,
        contrats.stream().map(ContratInfo::sansMontant).toList(),
        appelsOffres.stream().map(AppelOffreInfo::sansMontant).toList(),
        fournisseurs,
        maintenances.stream().map(MaintenanceInfo::sansCout).toList(),
        affectations,
        chantiers,
        procedures,
        indisponibles,
        masquees);
  }

  /**
   * Constructeur incrémental (Builder) utilisé pendant la collecte.
   *
   * @return un constructeur vide
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Constructeur incrémental. */
  public static final class Builder {
    private ContexteDonnees courant = vide();

    private Builder() {}

    /**
     * @param modification transformation à appliquer
     * @return ce constructeur
     */
    public Builder avec(UnaryOperator<ContexteDonnees> modification) {
      courant = modification.apply(courant);
      return this;
    }

    /**
     * @param section section indisponible
     * @return ce constructeur
     */
    public Builder indisponible(Section section) {
      Set<Section> s = EnumSet.noneOf(Section.class);
      s.addAll(courant.indisponibles());
      s.add(section);
      ContexteDonnees c = courant;
      courant =
          new ContexteDonnees(
              c.materiels(),
              c.besoins(),
              c.contrats(),
              c.appelsOffres(),
              c.fournisseurs(),
              c.maintenances(),
              c.affectations(),
              c.chantiers(),
              c.procedures(),
              s,
              c.masquees());
      return this;
    }

    /**
     * @param section section masquée (rôle)
     * @return ce constructeur
     */
    public Builder masquee(Section section) {
      courant = courant.masquer(section);
      return this;
    }

    /**
     * @return le contexte construit
     */
    public ContexteDonnees build() {
      return courant;
    }
  }

  /**
   * @param valeurs matériels
   * @return copie avec ces matériels
   */
  public ContexteDonnees avecMateriels(List<MaterielInfo> valeurs) {
    return new ContexteDonnees(
        valeurs,
        besoins,
        contrats,
        appelsOffres,
        fournisseurs,
        maintenances,
        affectations,
        chantiers,
        procedures,
        indisponibles,
        masquees);
  }

  /**
   * @param valeurs besoins
   * @return copie avec ces besoins
   */
  public ContexteDonnees avecBesoins(List<BesoinInfo> valeurs) {
    return new ContexteDonnees(
        materiels,
        valeurs,
        contrats,
        appelsOffres,
        fournisseurs,
        maintenances,
        affectations,
        chantiers,
        procedures,
        indisponibles,
        masquees);
  }

  /**
   * @param valeurs contrats
   * @return copie avec ces contrats
   */
  public ContexteDonnees avecContrats(List<ContratInfo> valeurs) {
    return new ContexteDonnees(
        materiels,
        besoins,
        valeurs,
        appelsOffres,
        fournisseurs,
        maintenances,
        affectations,
        chantiers,
        procedures,
        indisponibles,
        masquees);
  }

  /**
   * @param valeurs appels d'offres
   * @return copie avec ces appels d'offres
   */
  public ContexteDonnees avecAppelsOffres(List<AppelOffreInfo> valeurs) {
    return new ContexteDonnees(
        materiels,
        besoins,
        contrats,
        valeurs,
        fournisseurs,
        maintenances,
        affectations,
        chantiers,
        procedures,
        indisponibles,
        masquees);
  }

  /**
   * @param valeurs fournisseurs
   * @return copie avec ces fournisseurs
   */
  public ContexteDonnees avecFournisseurs(List<FournisseurInfo> valeurs) {
    return new ContexteDonnees(
        materiels,
        besoins,
        contrats,
        appelsOffres,
        valeurs,
        maintenances,
        affectations,
        chantiers,
        procedures,
        indisponibles,
        masquees);
  }

  /**
   * @param valeurs maintenances
   * @return copie avec ces maintenances
   */
  public ContexteDonnees avecMaintenances(List<MaintenanceInfo> valeurs) {
    return new ContexteDonnees(
        materiels,
        besoins,
        contrats,
        appelsOffres,
        fournisseurs,
        valeurs,
        affectations,
        chantiers,
        procedures,
        indisponibles,
        masquees);
  }

  /**
   * @param valeurs affectations
   * @return copie avec ces affectations
   */
  public ContexteDonnees avecAffectations(List<AffectationInfo> valeurs) {
    return new ContexteDonnees(
        materiels,
        besoins,
        contrats,
        appelsOffres,
        fournisseurs,
        maintenances,
        valeurs,
        chantiers,
        procedures,
        indisponibles,
        masquees);
  }

  /**
   * @param valeurs chantiers
   * @return copie avec ces chantiers
   */
  public ContexteDonnees avecChantiers(List<ChantierInfo> valeurs) {
    return new ContexteDonnees(
        materiels,
        besoins,
        contrats,
        appelsOffres,
        fournisseurs,
        maintenances,
        affectations,
        valeurs,
        procedures,
        indisponibles,
        masquees);
  }

  /**
   * @param valeurs procédures
   * @return copie avec ces procédures
   */
  public ContexteDonnees avecProcedures(List<Procedure> valeurs) {
    return new ContexteDonnees(
        materiels,
        besoins,
        contrats,
        appelsOffres,
        fournisseurs,
        maintenances,
        affectations,
        chantiers,
        valeurs,
        indisponibles,
        masquees);
  }
}
