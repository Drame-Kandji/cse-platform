package sn.cse.reporting.domain.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.DonneesReporting;
import sn.cse.reporting.domain.model.EtatMateriel;
import sn.cse.reporting.domain.model.ParametresReporting;
import sn.cse.reporting.domain.model.Perimetre;
import sn.cse.reporting.domain.model.Periode;
import sn.cse.reporting.domain.model.Priorite;
import sn.cse.reporting.domain.model.ProfilTableau;
import sn.cse.reporting.domain.model.Section;
import sn.cse.reporting.domain.model.StatutAppelOffre;
import sn.cse.reporting.domain.model.StatutBesoin;
import sn.cse.reporting.domain.model.Tableau;
import sn.cse.reporting.domain.model.vue.AppelOffreVue;
import sn.cse.reporting.domain.model.vue.BesoinVue;
import sn.cse.reporting.domain.model.vue.ChantierVue;
import sn.cse.reporting.domain.model.vue.ContratVue;
import sn.cse.reporting.domain.model.vue.FournisseurVue;
import sn.cse.reporting.domain.model.vue.LitigeVue;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.MaterielVue;

/**
 * Calcul pur des indicateurs d'un tableau de bord à partir des vues. Les formules sont documentées
 * dans le README du service.
 */
public class CalculateurTableau {

  private final ParametresReporting parametres;

  /**
   * @param parametres paramètres métier
   */
  public CalculateurTableau(ParametresReporting parametres) {
    this.parametres = parametres;
  }

  /**
   * @param profil profil (sections remplies)
   * @param periode période
   * @param perimetre périmètre de chantiers
   * @param donnees vues chargées
   * @param aujourdhui date du jour
   * @param genereLe instant de génération
   * @return le tableau de bord
   */
  public Tableau calculer(
      ProfilTableau profil,
      Periode periode,
      Perimetre perimetre,
      DonneesReporting donnees,
      LocalDate aujourdhui,
      Instant genereLe) {
    DonneesReporting d = donnees.restreindre(perimetre);
    return new Tableau(
        profil,
        periode.du(),
        periode.au(),
        genereLe,
        profil.affiche(Section.PARC)
            ? parc(periode, aujourdhui, d, perimetre, donnees.maintenances())
            : null,
        profil.affiche(Section.BESOINS) ? besoins(periode, d) : null,
        profil.affiche(Section.ACHATS) ? achats(periode, aujourdhui, d) : null,
        profil.affiche(Section.FOURNISSEURS) ? fournisseurs(d) : null,
        profil.affiche(Section.FINANCES) ? finances(periode, aujourdhui, d) : null,
        profil.affiche(Section.CHANTIERS)
            ? chantiers(periode, aujourdhui, d, donnees.maintenances())
            : null);
  }

  /**
   * @param periode période
   * @param aujourdhui date du jour
   * @param d données restreintes
   * @param perimetre périmètre
   * @param immobilisations maintenances de tous les chantiers (jours d'immobilisation)
   * @return indicateurs du parc (état courant + utilisation sur la période)
   */
  public Tableau.Parc parc(
      Periode periode,
      LocalDate aujourdhui,
      DonneesReporting d,
      Perimetre perimetre,
      List<MaintenanceVue> immobilisations) {
    Map<EtatMateriel, Long> parEtat = compterParEtat(d.materiels());
    long total = d.materiels().size();
    long reformes = parEtat.get(EtatMateriel.REFORME);
    List<Tableau.ParcCategorie> parCategorie =
        Arrays.stream(CategorieMateriel.values())
            .map(
                c -> {
                  List<MaterielVue> mc =
                      d.materiels().stream().filter(m -> m.categorie() == c).toList();
                  Map<EtatMateriel, Long> e = compterParEtat(mc);
                  return new Tableau.ParcCategorie(
                      c,
                      mc.size(),
                      e.get(EtatMateriel.DISPONIBLE),
                      e.get(EtatMateriel.AFFECTE),
                      e.get(EtatMateriel.EN_MAINTENANCE),
                      utilisation(periode, aujourdhui, d, perimetre, Set.of(c), immobilisations));
                })
            .toList();
    return new Tableau.Parc(
        total,
        parEtat.get(EtatMateriel.DISPONIBLE),
        parEtat.get(EtatMateriel.AFFECTE),
        parEtat.get(EtatMateriel.EN_MAINTENANCE),
        reformes,
        Arrondis.taux(parEtat.get(EtatMateriel.DISPONIBLE), total - reformes),
        utilisation(
            periode, aujourdhui, d, perimetre, Set.of(CategorieMateriel.values()), immobilisations),
        d.maintenances().stream().filter(m -> m.enCours()).count(),
        d.pannes().stream().filter(p -> p.ouverte()).count(),
        d.materiels().stream().filter(MaterielVue::alerteEnAttente).count(),
        parCategorie);
  }

  /**
   * @param periode période
   * @param d données restreintes
   * @return indicateurs des besoins soumis sur la période (statut courant)
   */
  public Tableau.Besoins besoins(Periode periode, DonneesReporting d) {
    List<BesoinVue> soumis = besoinsSoumis(periode, d);
    Map<StatutBesoin, Long> parStatut = compter(soumis, BesoinVue::statut, StatutBesoin.class);
    Map<Priorite, Long> parPriorite = compter(soumis, BesoinVue::priorite, Priorite.class);
    return new Tableau.Besoins(
        soumis.size(),
        parStatut.get(StatutBesoin.EN_ATTENTE),
        parStatut.get(StatutBesoin.VALIDE),
        parStatut.get(StatutBesoin.REJETE),
        parStatut.get(StatutBesoin.AFFECTE_DIRECT),
        parStatut.get(StatutBesoin.EN_ACHAT),
        Arrondis.moyenne(
                soumis.stream()
                    .map(BesoinVue::delaiTraitementHeures)
                    .flatMap(Optional::stream)
                    .toList())
            .orElse(null),
        parPriorite);
  }

  /**
   * @param periode période
   * @param aujourdhui date du jour
   * @param d données restreintes
   * @return indicateurs achats
   */
  public Tableau.Achats achats(Periode periode, LocalDate aujourdhui, DonneesReporting d) {
    List<AppelOffreVue> publies =
        d.appelsOffres().stream().filter(a -> periode.contient(a.datePublication())).toList();
    Map<StatutAppelOffre, Long> parStatut =
        compter(publies, AppelOffreVue::statut, StatutAppelOffre.class);
    List<ContratVue> livres = contratsLivres(periode, d);
    return new Tableau.Achats(
        parStatut.get(StatutAppelOffre.PUBLIE),
        parStatut.get(StatutAppelOffre.CLOTURE),
        parStatut.get(StatutAppelOffre.ATTRIBUE),
        d.contrats().stream().filter(ContratVue::enCours).count(),
        d.contrats().stream().filter(c -> c.enRetard(aujourdhui)).count(),
        Arrondis.moyenne(delaisApprovisionnement(periode, d)).orElse(null),
        Arrondis.tauxDefini(
                livres.stream().filter(ContratVue::livreALHeure).count(),
                livres.stream().filter(ContratVue::ponctualiteMesurable).count())
            .orElse(null));
  }

  /**
   * @param d données
   * @return indicateurs fournisseurs (classement des actifs par score décroissant)
   */
  public Tableau.Fournisseurs fournisseurs(DonneesReporting d) {
    List<FournisseurVue> actifs = d.fournisseurs().stream().filter(FournisseurVue::actif).toList();
    Map<UUID, Long> litigesParFournisseur =
        d.litiges().stream()
            .filter(l -> l.fournisseurId() != null)
            .collect(Collectors.groupingBy(LitigeVue::fournisseurId, Collectors.counting()));
    Map<UUID, List<ContratVue>> contratsParFournisseur =
        d.contrats().stream()
            .filter(c -> c.fournisseurId() != null && c.ponctualiteMesurable())
            .collect(Collectors.groupingBy(ContratVue::fournisseurId));
    List<Tableau.FournisseurClasse> top =
        actifs.stream()
            .sorted(
                Comparator.comparing(FournisseurVue::scorePerformance)
                    .reversed()
                    .thenComparing(f -> f.raisonSociale() == null ? "" : f.raisonSociale()))
            .limit(parametres.topFournisseurs())
            .map(
                f -> {
                  List<ContratVue> livres = contratsParFournisseur.getOrDefault(f.id(), List.of());
                  return new Tableau.FournisseurClasse(
                      f.id(),
                      f.raisonSociale(),
                      f.scorePerformance(),
                      litigesParFournisseur.getOrDefault(f.id(), 0L),
                      Arrondis.tauxDefini(
                              livres.stream().filter(ContratVue::livreALHeure).count(),
                              livres.size())
                          .orElse(null));
                })
            .toList();
    return new Tableau.Fournisseurs(
        actifs.size(),
        Arrondis.moyenne(actifs.stream().map(FournisseurVue::scorePerformance).toList())
            .orElse(BigDecimal.ZERO.setScale(1)),
        d.litiges().stream().filter(LitigeVue::ouvert).count(),
        top);
  }

  /**
   * @param periode période
   * @param aujourdhui date du jour
   * @param d données restreintes
   * @return indicateurs financiers
   */
  public Tableau.Finances finances(Periode periode, LocalDate aujourdhui, DonneesReporting d) {
    BigDecimal budget =
        d.chantiers().stream()
            .filter(c -> c.actifSur(periode))
            .map(ChantierVue::budget)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal engage = montantEngage(periode, d);
    return new Tableau.Finances(
        Arrondis.montant(budget),
        Arrondis.montant(engage),
        Arrondis.montant(
            d.factures().stream()
                .filter(f -> f.mandateeDans(periode))
                .map(f -> f.montantTtc())
                .reduce(BigDecimal.ZERO, BigDecimal::add)),
        Arrondis.montant(
            d.factures().stream()
                .filter(f -> f.payeeDans(periode))
                .map(f -> f.montantTtc())
                .reduce(BigDecimal.ZERO, BigDecimal::add)),
        Arrondis.taux(engage, budget),
        Arrondis.montant(coutMaintenance(periode, d)),
        Arrondis.montant(coutExploitation(periode, aujourdhui, d)));
  }

  /**
   * Marge d'un chantier = montant du marché × (jours du marché écoulés dans la période / durée
   * totale du marché) − coût d'exploitation − coût de maintenance.
   *
   * @param periode période
   * @param aujourdhui date du jour
   * @param d données restreintes
   * @param immobilisations maintenances de tous les chantiers (jours d'immobilisation)
   * @return une ligne par chantier actif sur la période, triée par nom
   */
  public List<Tableau.Chantier> chantiers(
      Periode periode,
      LocalDate aujourdhui,
      DonneesReporting d,
      List<MaintenanceVue> immobilisations) {
    return d.chantiers().stream()
        .filter(c -> c.actifSur(periode))
        .sorted(Comparator.comparing(ChantierVue::nomAffichable))
        .map(c -> chantier(c, periode, aujourdhui, d, immobilisations))
        .toList();
  }

  private Tableau.Chantier chantier(
      ChantierVue c,
      Periode periode,
      LocalDate aujourdhui,
      DonneesReporting d,
      List<MaintenanceVue> immobilisations) {
    Perimetre seul = Perimetre.chantiers(Set.of(c.id()));
    DonneesReporting dc = d.restreindre(seul);
    BigDecimal exploitation = coutExploitation(periode, aujourdhui, dc);
    BigDecimal maintenance = coutMaintenance(periode, dc);
    BigDecimal marge =
        recetteProratisee(c, periode, aujourdhui).subtract(exploitation).subtract(maintenance);
    return new Tableau.Chantier(
        c.id(),
        c.nomAffichable(),
        c.region(),
        Arrondis.montant(c.budget()),
        Arrondis.montant(c.montantMarche()),
        Arrondis.montant(montantEngage(periode, dc)),
        Arrondis.montant(exploitation),
        Arrondis.montant(maintenance),
        Arrondis.montant(marge),
        d.materiels().stream()
            .filter(m -> c.id().equals(m.chantierId()) && m.etat() == EtatMateriel.AFFECTE)
            .count(),
        CalculateurUtilisation.tauxPerimetre(
                periode, aujourdhui, dc, seul, x -> true, immobilisations)
            .orElse(BigDecimal.ZERO.setScale(1)),
        d.besoins().stream()
            .filter(b -> c.id().equals(b.chantierId()) && b.statut() == StatutBesoin.EN_ATTENTE)
            .count());
  }

  /**
   * @param c chantier
   * @param periode période
   * @param aujourdhui date du jour
   * @return part du montant du marché correspondant aux jours écoulés de la période
   */
  BigDecimal recetteProratisee(ChantierVue c, Periode periode, LocalDate aujourdhui) {
    Optional<Periode> marche = c.intervalleMarche(parametres.dureeChantierDefautJours());
    Optional<Periode> ecoulee = periode.ecouleeAu(aujourdhui);
    if (marche.isEmpty() || ecoulee.isEmpty()) {
      return BigDecimal.ZERO;
    }
    long jours =
        ecoulee
            .get()
            .intersection(marche.get().du(), marche.get().au())
            .map(Periode::jours)
            .orElse(0L);
    return c.montantMarche()
        .multiply(BigDecimal.valueOf(jours))
        .divide(BigDecimal.valueOf(marche.get().jours()), 0, java.math.RoundingMode.HALF_UP);
  }

  // ---- briques partagées avec les séries ----

  /**
   * @param periode période
   * @param d données
   * @return besoins dont le jour de soumission appartient à la période
   */
  public static List<BesoinVue> besoinsSoumis(Periode periode, DonneesReporting d) {
    return d.besoins().stream()
        .filter(b -> b.dateSoumission().filter(periode::contient).isPresent())
        .toList();
  }

  /**
   * @param periode période
   * @param d données
   * @return somme des contrats dont l'engagement a été validé dans la période
   */
  public static BigDecimal montantEngage(Periode periode, DonneesReporting d) {
    return d.contrats().stream()
        .filter(c -> c.engage() && periode.contient(c.dateEngagement()))
        .map(ContratVue::montant)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * @param periode période
   * @param d données
   * @return coût des maintenances clôturées imputées à la période
   */
  public static BigDecimal coutMaintenance(Periode periode, DonneesReporting d) {
    return d.maintenances().stream()
        .map(m -> m.coutDans(periode))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * @param periode période
   * @param aujourdhui date du jour
   * @param d données
   * @return Σ jours affectés dans la période × tarif journalier
   */
  public static BigDecimal coutExploitation(
      Periode periode, LocalDate aujourdhui, DonneesReporting d) {
    return d.affectations().stream()
        .map(a -> a.coutDans(periode, aujourdhui))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * @param periode période
   * @param d données
   * @return contrats livrés dans la période
   */
  public static List<ContratVue> contratsLivres(Periode periode, DonneesReporting d) {
    return d.contrats().stream()
        .filter(c -> c.livre() && periode.contient(c.dateLivraisonReelle()))
        .toList();
  }

  /**
   * Délai d'approvisionnement d'un contrat livré = livraison réelle − publication de l'AO, en
   * jours. Les contrats dont l'AO n'est pas connu (ou de délai négatif) sont ignorés.
   *
   * @param periode période (jour de livraison)
   * @param d données
   * @return les délais en jours
   */
  public static List<BigDecimal> delaisApprovisionnement(Periode periode, DonneesReporting d) {
    Map<UUID, AppelOffreVue> aos =
        d.appelsOffres().stream()
            .collect(Collectors.toMap(AppelOffreVue::id, Function.identity(), (a, b) -> a));
    return contratsLivres(periode, d).stream()
        .map(
            c -> {
              AppelOffreVue ao = c.appelOffreId() == null ? null : aos.get(c.appelOffreId());
              LocalDate publication = ao == null ? null : ao.datePublication();
              return publication == null
                  ? -1L
                  : ChronoUnit.DAYS.between(publication, c.dateLivraisonReelle());
            })
        .filter(j -> j >= 0)
        .map(BigDecimal::valueOf)
        .toList();
  }

  private static BigDecimal utilisation(
      Periode periode,
      LocalDate aujourdhui,
      DonneesReporting d,
      Perimetre perimetre,
      Set<CategorieMateriel> categories,
      List<MaintenanceVue> immobilisations) {
    return CalculateurUtilisation.tauxPerimetre(
            periode, aujourdhui, d, perimetre, categories::contains, immobilisations)
        .orElse(BigDecimal.ZERO.setScale(1));
  }

  private static Map<EtatMateriel, Long> compterParEtat(List<MaterielVue> materiels) {
    return compter(materiels, MaterielVue::etat, EtatMateriel.class);
  }

  private static <T, E extends Enum<E>> Map<E, Long> compter(
      List<T> elements, Function<T, E> cle, Class<E> type) {
    EnumMap<E, Long> compte = new EnumMap<>(type);
    for (E e : type.getEnumConstants()) {
      compte.put(e, 0L);
    }
    for (T t : elements) {
      E e = cle.apply(t);
      if (e != null) {
        compte.merge(e, 1L, Long::sum);
      }
    }
    return compte;
  }
}
