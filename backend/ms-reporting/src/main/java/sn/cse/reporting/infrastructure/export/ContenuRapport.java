package sn.cse.reporting.infrastructure.export;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import sn.cse.reporting.domain.model.Priorite;
import sn.cse.reporting.domain.model.Section;
import sn.cse.reporting.domain.model.Tableau;

/**
 * Contenu d'un rapport indépendant du format : sections, indicateurs clé/valeur et tableaux, avec
 * libellés français. Partagé par les exports PDF et Excel pour garantir les mêmes chiffres.
 *
 * @param blocs une entrée par section affichée, dans l'ordre du contrat
 */
record ContenuRapport(List<Bloc> blocs) {

  /** Nature d'une valeur (choix du format d'affichage). */
  enum Nature {
    MONTANT,
    TAUX,
    NOMBRE,
    SCORE,
    JOURS,
    HEURES,
    TEXTE
  }

  /**
   * @param nature nature
   * @param nombre valeur numérique (null = non définie)
   * @param texte valeur textuelle (nature TEXTE)
   */
  record Valeur(Nature nature, BigDecimal nombre, String texte) {

    static Valeur de(Nature nature, BigDecimal nombre) {
      return new Valeur(nature, nombre, null);
    }

    static Valeur nombre(long n) {
      return new Valeur(Nature.NOMBRE, BigDecimal.valueOf(n), null);
    }

    static Valeur texte(String t) {
      return new Valeur(Nature.TEXTE, null, t == null ? FormatFr.NON_DEFINI : t);
    }

    /**
     * @return la valeur mise en forme à la française
     */
    String formatee() {
      return switch (nature) {
        case MONTANT -> FormatFr.montant(nombre);
        case TAUX -> FormatFr.taux(nombre);
        case NOMBRE -> FormatFr.nombre(nombre);
        case SCORE -> FormatFr.score(nombre);
        case JOURS -> FormatFr.decimal(nombre, "j");
        case HEURES -> FormatFr.decimal(nombre, "h");
        case TEXTE -> texte;
      };
    }
  }

  /**
   * @param libelle libellé de l'indicateur
   * @param valeur valeur
   */
  record Ligne(String libelle, Valeur valeur) {}

  /**
   * @param titre titre du tableau
   * @param colonnes en-têtes
   * @param lignes lignes de valeurs
   */
  record Table(String titre, List<String> colonnes, List<List<Valeur>> lignes) {}

  /**
   * @param section section
   * @param indicateurs indicateurs clé/valeur
   * @param tables tableaux détaillés
   */
  record Bloc(Section section, List<Ligne> indicateurs, List<Table> tables) {}

  /**
   * @param t tableau de bord
   * @return le contenu du rapport
   */
  static ContenuRapport depuis(Tableau t) {
    List<Bloc> blocs = new ArrayList<>();
    if (t.parc() != null) {
      blocs.add(parc(t.parc()));
    }
    if (t.besoins() != null) {
      blocs.add(besoins(t.besoins()));
    }
    if (t.achats() != null) {
      blocs.add(achats(t.achats()));
    }
    if (t.fournisseurs() != null) {
      blocs.add(fournisseurs(t.fournisseurs()));
    }
    if (t.finances() != null) {
      blocs.add(finances(t.finances()));
    }
    if (t.chantiers() != null) {
      blocs.add(chantiers(t.chantiers()));
    }
    return new ContenuRapport(List.copyOf(blocs));
  }

  private static Ligne l(String libelle, Valeur v) {
    return new Ligne(libelle, v);
  }

  private static Bloc parc(Tableau.Parc p) {
    List<Ligne> lignes =
        List.of(
            l("Matériels du parc", Valeur.nombre(p.total())),
            l("Disponibles", Valeur.nombre(p.disponibles())),
            l("Affectés", Valeur.nombre(p.affectes())),
            l("En maintenance", Valeur.nombre(p.enMaintenance())),
            l("Réformés", Valeur.nombre(p.reformes())),
            l("Taux de disponibilité", Valeur.de(Nature.TAUX, p.tauxDisponibilite())),
            l("Taux d'utilisation", Valeur.de(Nature.TAUX, p.tauxUtilisation())),
            l("Maintenances en cours", Valeur.nombre(p.maintenancesEnCours())),
            l("Pannes ouvertes", Valeur.nombre(p.pannesOuvertes())),
            l("Alertes de maintenance", Valeur.nombre(p.alertesMaintenance())));
    Table categories =
        new Table(
            "Par catégorie",
            List.of(
                "Catégorie", "Total", "Disponibles", "Affectés", "En maintenance", "Utilisation"),
            p.parCategorie().stream()
                .map(
                    c ->
                        List.of(
                            Valeur.texte(c.categorie().libelle()),
                            Valeur.nombre(c.total()),
                            Valeur.nombre(c.disponibles()),
                            Valeur.nombre(c.affectes()),
                            Valeur.nombre(c.enMaintenance()),
                            Valeur.de(Nature.TAUX, c.tauxUtilisation())))
                .toList());
    return new Bloc(Section.PARC, lignes, List.of(categories));
  }

  private static Bloc besoins(Tableau.Besoins b) {
    List<Ligne> lignes =
        List.of(
            l("Besoins soumis", Valeur.nombre(b.total())),
            l("En attente", Valeur.nombre(b.enAttente())),
            l("Validés", Valeur.nombre(b.valides())),
            l("Rejetés", Valeur.nombre(b.rejetes())),
            l("Affectés directement", Valeur.nombre(b.affectesDirect())),
            l("En achat", Valeur.nombre(b.enAchat())),
            l(
                "Délai moyen de traitement",
                Valeur.de(Nature.HEURES, b.delaiMoyenTraitementHeures())));
    List<List<Valeur>> priorites = new ArrayList<>();
    for (Map.Entry<Priorite, Long> e : b.parPriorite().entrySet()) {
      priorites.add(List.of(Valeur.texte(e.getKey().libelle()), Valeur.nombre(e.getValue())));
    }
    return new Bloc(
        Section.BESOINS,
        lignes,
        List.of(new Table("Par priorité", List.of("Priorité", "Besoins"), priorites)));
  }

  private static Bloc achats(Tableau.Achats a) {
    return new Bloc(
        Section.ACHATS,
        List.of(
            l("Appels d'offres publiés (ouverts)", Valeur.nombre(a.aoPublies())),
            l("Appels d'offres clôturés", Valeur.nombre(a.aoClotures())),
            l("Appels d'offres attribués", Valeur.nombre(a.aoAttribues())),
            l("Contrats en cours", Valeur.nombre(a.contratsEnCours())),
            l("Livraisons en retard", Valeur.nombre(a.livraisonsEnRetard())),
            l(
                "Délai moyen d'approvisionnement",
                Valeur.de(Nature.JOURS, a.delaiMoyenApprovisionnementJours())),
            l("Taux de livraison à l'heure", Valeur.de(Nature.TAUX, a.tauxLivraisonALHeure()))),
        List.of());
  }

  private static Bloc fournisseurs(Tableau.Fournisseurs f) {
    Table top =
        new Table(
            "Meilleurs fournisseurs",
            List.of("Fournisseur", "Score", "Litiges", "Livraison à l'heure"),
            f.top().stream()
                .map(
                    c ->
                        List.of(
                            Valeur.texte(c.raisonSociale()),
                            Valeur.de(Nature.SCORE, c.scorePerformance()),
                            Valeur.nombre(c.nombreLitiges()),
                            Valeur.de(Nature.TAUX, c.tauxLivraisonALHeure())))
                .toList());
    return new Bloc(
        Section.FOURNISSEURS,
        List.of(
            l("Fournisseurs actifs", Valeur.nombre(f.actifs())),
            l("Score moyen (sur 100)", Valeur.de(Nature.SCORE, f.scoreMoyen())),
            l("Litiges ouverts", Valeur.nombre(f.litigesOuverts()))),
        List.of(top));
  }

  private static Bloc finances(Tableau.Finances f) {
    return new Bloc(
        Section.FINANCES,
        List.of(
            l("Budget des chantiers actifs", Valeur.de(Nature.MONTANT, f.budgetTotal())),
            l("Montant engagé", Valeur.de(Nature.MONTANT, f.montantEngage())),
            l("Montant mandaté", Valeur.de(Nature.MONTANT, f.montantMandate())),
            l("Montant payé", Valeur.de(Nature.MONTANT, f.montantPaye())),
            l("Taux d'exécution budgétaire", Valeur.de(Nature.TAUX, f.tauxExecutionBudget())),
            l("Coût de maintenance", Valeur.de(Nature.MONTANT, f.coutMaintenance())),
            l("Coût d'exploitation", Valeur.de(Nature.MONTANT, f.coutExploitation()))),
        List.of());
  }

  private static Bloc chantiers(List<Tableau.Chantier> chantiers) {
    Table table =
        new Table(
            "Indicateurs par chantier",
            List.of(
                "Chantier",
                "Région",
                "Budget",
                "Marché",
                "Engagé",
                "Exploitation",
                "Maintenance",
                "Marge",
                "Matériels affectés",
                "Utilisation",
                "Besoins en attente"),
            chantiers.stream()
                .map(
                    c ->
                        List.of(
                            Valeur.texte(c.chantierNom()),
                            Valeur.texte(c.region()),
                            Valeur.de(Nature.MONTANT, c.budget()),
                            Valeur.de(Nature.MONTANT, c.montantMarche()),
                            Valeur.de(Nature.MONTANT, c.montantEngage()),
                            Valeur.de(Nature.MONTANT, c.coutExploitation()),
                            Valeur.de(Nature.MONTANT, c.coutMaintenance()),
                            Valeur.de(Nature.MONTANT, c.marge()),
                            Valeur.nombre(c.materielsAffectes()),
                            Valeur.de(Nature.TAUX, c.tauxUtilisation()),
                            Valeur.nombre(c.besoinsEnAttente())))
                .toList());
    return new Bloc(
        Section.CHANTIERS,
        List.of(l("Chantiers actifs sur la période", Valeur.nombre(chantiers.size()))),
        List.of(table));
  }
}
