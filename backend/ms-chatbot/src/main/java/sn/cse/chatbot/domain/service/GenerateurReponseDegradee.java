package sn.cse.chatbot.domain.service;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import sn.cse.chatbot.domain.model.AffectationInfo;
import sn.cse.chatbot.domain.model.AnalyseQuestion;
import sn.cse.chatbot.domain.model.AppelOffreInfo;
import sn.cse.chatbot.domain.model.BesoinInfo;
import sn.cse.chatbot.domain.model.ContexteDonnees;
import sn.cse.chatbot.domain.model.ContratInfo;
import sn.cse.chatbot.domain.model.Entites;
import sn.cse.chatbot.domain.model.FournisseurInfo;
import sn.cse.chatbot.domain.model.MaintenanceInfo;
import sn.cse.chatbot.domain.model.MaterielInfo;
import sn.cse.chatbot.domain.model.Procedure;
import sn.cse.chatbot.domain.model.Section;

/**
 * Réponses sans LLM (mode dégradé) : un modèle par intention, construit directement à partir des
 * données réelles (listes à puces markdown). Utilisé quand aucun LLM n'est configuré ou qu'il
 * échoue ; doit rester réellement utile.
 */
public class GenerateurReponseDegradee {

  private static final String PUCE = "- ";

  private final int maxElements;

  /**
   * @param maxElements nombre maximal d'éléments listés par section
   */
  public GenerateurReponseDegradee(int maxElements) {
    this.maxElements = maxElements;
  }

  /**
   * @param analyse intention et entités
   * @param c données filtrées
   * @param exemples questions types (réponse AUTRE)
   * @return la réponse markdown
   */
  public String generer(AnalyseQuestion analyse, ContexteDonnees c, List<String> exemples) {
    Entites e = analyse.entites();
    String corps =
        switch (analyse.intention()) {
          case LOCALISER_ENGIN -> localiser(e, c);
          case DISPONIBILITE -> disponibilite(e, c);
          case STATUT_BESOIN -> besoins(e, c);
          case STATUT_COMMANDE -> commandes(e, c);
          case HISTORIQUE_MAINTENANCE -> maintenances(e, c);
          case PLANNING_AFFECTATION -> planning(e, c);
          case FOURNISSEURS_PAR_CATEGORIE -> fournisseurs(e, c);
          case PROCEDURE -> procedures(c);
          case AUTRE -> autre(exemples);
        };
    return (corps + avertissements(c)).trim();
  }

  private String localiser(Entites e, ContexteDonnees c) {
    if (c.materiels().isEmpty()) {
      return aucun(
          "aucun matériel", e.rechercheMateriel().orElse(categorie(e)), c, Section.MATERIELS);
    }
    return titre("Localisation", c.materiels().size(), "matériel trouvé", "matériels trouvés")
        + liste(
            c.materiels(),
            m -> {
              String ou =
                  m.chantierNom() != null
                      ? "sur le chantier **" + m.chantierNom() + "**"
                      : "AFFECTE".equals(m.etat())
                          ? "affecté (chantier non précisé)"
                          : "au dépôt (non affecté)";
              String gps =
                  m.latitude() == null ? "" : " — GPS " + m.latitude() + ", " + m.longitude();
              return "**"
                  + m.code()
                  + "** — "
                  + m.designation()
                  + " : "
                  + ou
                  + " ("
                  + Libelles.de(m.etat())
                  + ")"
                  + gps;
            });
  }

  private String disponibilite(Entites e, ContexteDonnees c) {
    List<MaterielInfo> dispo =
        c.materiels().stream().filter(m -> "DISPONIBLE".equals(m.etat())).toList();
    String objet = e.rechercheMateriel().orElse(categorie(e));
    StringBuilder sb = new StringBuilder();
    if (dispo.isEmpty()) {
      sb.append(aucun("aucun matériel disponible", objet, c, Section.MATERIELS));
    } else {
      sb.append(
              titre(
                  "Disponibilité" + (objet == null ? "" : " — " + objet),
                  dispo.size(),
                  "matériel disponible",
                  "matériels disponibles"))
          .append(
              liste(
                  dispo,
                  m ->
                      "**"
                          + m.code()
                          + "** — "
                          + m.designation()
                          + " ("
                          + Libelles.de(m.categorie())
                          + ")"
                          + (m.maintenanceDue() ? " — maintenance préventive due" : "")));
    }
    if (e.quantite() != null && !c.indisponibles().contains(Section.MATERIELS)) {
      sb.append("\n\n");
      if (dispo.size() >= e.quantite()) {
        sb.append("La quantité demandée (")
            .append(e.quantite())
            .append(") est couverte par le parc.");
      } else {
        sb.append("Seulement ")
            .append(dispo.size())
            .append(" disponible(s) pour ")
            .append(e.quantite())
            .append(
                " demandé(s) : exprimez un besoin, le Responsable Matériel l'orientera vers un achat si nécessaire.");
      }
    }
    return sb.toString();
  }

  private String besoins(Entites e, ContexteDonnees c) {
    if (c.besoins().isEmpty()) {
      String objet =
          e.referencesBesoin().isEmpty() ? null : String.join(", ", e.referencesBesoin());
      return aucun("aucun besoin", objet, c, Section.BESOINS);
    }
    return titre("Suivi des besoins", c.besoins().size(), "besoin", "besoins")
        + liste(c.besoins(), this::besoin);
  }

  private String besoin(BesoinInfo b) {
    StringBuilder sb =
        new StringBuilder("**")
            .append(b.reference())
            .append("** — ")
            .append(b.quantite())
            .append(" × ")
            .append(b.designation())
            .append(" (chantier ")
            .append(b.chantierNom() == null ? "—" : b.chantierNom())
            .append(") : ")
            .append(Libelles.de(b.statut()));
    if ("REJETE".equals(b.statut()) && b.motifRejet() != null) {
      sb.append(" — motif : ").append(b.motifRejet());
    }
    if ("EN_ATTENTE".equals(b.statut())) {
      sb.append(" par le Responsable Matériel");
    } else if (b.validateurNom() != null) {
      sb.append(" par ").append(b.validateurNom());
    }
    return sb.append(" — priorité ")
        .append(Libelles.de(b.priorite()))
        .append(", besoin au ")
        .append(Libelles.date(b.dateBesoin()))
        .toString();
  }

  private String commandes(Entites e, ContexteDonnees c) {
    if (c.contrats().isEmpty() && c.appelsOffres().isEmpty()) {
      List<String> refs = new java.util.ArrayList<>(e.referencesContrat());
      refs.addAll(e.referencesAppelOffre());
      return aucun(
          "aucun contrat ni appel d'offres",
          refs.isEmpty() ? null : String.join(", ", refs),
          c,
          Section.CONTRATS);
    }
    StringBuilder sb = new StringBuilder();
    if (!c.contrats().isEmpty()) {
      sb.append(titre("Contrats", c.contrats().size(), "contrat", "contrats"))
          .append(liste(c.contrats(), this::contrat));
    }
    if (!c.appelsOffres().isEmpty()) {
      if (sb.length() > 0) {
        sb.append("\n\n");
      }
      sb.append(
              titre(
                  "Appels d'offres", c.appelsOffres().size(), "appel d'offres", "appels d'offres"))
          .append(liste(c.appelsOffres(), this::appelOffre));
    }
    return sb.toString();
  }

  private String contrat(ContratInfo k) {
    StringBuilder sb =
        new StringBuilder("**")
            .append(k.numero())
            .append("** — ")
            .append(k.objet())
            .append(" (")
            .append(k.fournisseurNom() == null ? "fournisseur —" : k.fournisseurNom())
            .append(") : ")
            .append(Libelles.de(k.statut()));
    if (k.dateLivraisonReelle() != null) {
      sb.append(", livré le ").append(Libelles.date(k.dateLivraisonReelle()));
    } else if (k.dateLivraisonPrevue() != null) {
      sb.append(", livraison prévue le ").append(Libelles.date(k.dateLivraisonPrevue()));
    }
    if (k.montant() != null) {
      sb.append(" — ").append(Libelles.montant(k.montant()));
    }
    return sb.toString();
  }

  private String appelOffre(AppelOffreInfo a) {
    StringBuilder sb =
        new StringBuilder("**")
            .append(a.reference())
            .append("** — ")
            .append(a.objet())
            .append(" : ")
            .append(Libelles.de(a.statut()))
            .append(", ")
            .append(a.nombreOffres())
            .append(" offre(s), date limite le ")
            .append(Libelles.date(a.dateLimite()));
    if (a.montantAttribue() != null) {
      sb.append(" — attribué pour ").append(Libelles.montant(a.montantAttribue()));
    }
    return sb.toString();
  }

  private String maintenances(Entites e, ContexteDonnees c) {
    if (c.maintenances().isEmpty()) {
      return aucun(
          "aucune maintenance", e.rechercheMateriel().orElse(null), c, Section.MAINTENANCES);
    }
    return titre(
            "Historique de maintenance", c.maintenances().size(), "intervention", "interventions")
        + liste(c.maintenances(), this::maintenance);
  }

  private String maintenance(MaintenanceInfo m) {
    StringBuilder sb =
        new StringBuilder("**")
            .append(m.numeroOt())
            .append("** — ")
            .append(m.materielCode())
            .append(" ")
            .append(m.materielDesignation())
            .append(" : ")
            .append("maintenance ")
            .append(Libelles.de(m.type()))
            .append(", ")
            .append(Libelles.de(m.statut()))
            .append(", planifiée le ")
            .append(Libelles.date(m.datePlanifiee()));
    if (m.dateFin() != null) {
      sb.append(", terminée le ").append(Libelles.date(m.dateFin()));
    }
    if (m.description() != null && !m.description().isBlank()) {
      sb.append(" — ").append(m.description());
    }
    if (m.cout() != null) {
      sb.append(" — coût ").append(Libelles.montant(m.cout()));
    }
    return sb.toString();
  }

  private String planning(Entites e, ContexteDonnees c) {
    if (c.affectations().isEmpty()) {
      String objet = e.chantier() != null ? e.chantier() : e.rechercheMateriel().orElse(null);
      return aucun("aucune affectation", objet, c, Section.AFFECTATIONS);
    }
    return titre(
            "Planning des affectations", c.affectations().size(), "affectation", "affectations")
        + liste(c.affectations(), this::affectation);
  }

  private String affectation(AffectationInfo a) {
    return "**"
        + a.materielCode()
        + "** — "
        + a.materielDesignation()
        + " → "
        + a.chantierNom()
        + " du "
        + Libelles.date(a.dateDebut())
        + " au "
        + Libelles.date(a.dateFinPrevue())
        + " ("
        + Libelles.de(a.statut())
        + ")";
  }

  private String fournisseurs(Entites e, ContexteDonnees c) {
    if (c.fournisseurs().isEmpty()) {
      return aucun("aucun fournisseur actif", categorie(e), c, Section.FOURNISSEURS);
    }
    String pour = e.categorie() == null ? "" : " — " + Libelles.de(e.categorie());
    return titre(
            "Fournisseurs recommandés" + pour,
            c.fournisseurs().size(),
            "fournisseur",
            "fournisseurs")
        + liste(c.fournisseurs(), this::fournisseur);
  }

  private String fournisseur(FournisseurInfo f) {
    StringBuilder sb =
        new StringBuilder().append(f.rang()).append(". **").append(f.raisonSociale()).append("**");
    if (f.scorePerformance() != null) {
      sb.append(" — score ").append(Libelles.nombre(f.scorePerformance())).append("/100");
    }
    if (f.tauxLivraisonALHeure() != null) {
      sb.append(", livraisons à l'heure ")
          .append(Libelles.nombre(f.tauxLivraisonALHeure()))
          .append(" %");
    }
    if (f.nombreLitiges() > 0) {
      sb.append(", ").append(f.nombreLitiges()).append(" litige(s)");
    }
    if (!f.specialites().isEmpty()) {
      sb.append(" (")
          .append(f.specialites().stream().map(Libelles::de).collect(Collectors.joining(", ")))
          .append(")");
    }
    return sb.toString();
  }

  private String procedures(ContexteDonnees c) {
    if (c.procedures().isEmpty()) {
      return "Je n'ai pas trouvé de procédure correspondant à votre question. Reformulez-la "
          + "(par exemple : « Comment exprimer un besoin ? », « Comment signaler une panne ? »).";
    }
    StringBuilder sb = new StringBuilder();
    for (Procedure p : c.procedures()) {
      if (sb.length() > 0) {
        sb.append("\n\n");
      }
      sb.append("**").append(p.sujet()).append("**\n");
      for (int i = 0; i < p.etapes().size(); i++) {
        sb.append(i + 1).append(". ").append(p.etapes().get(i)).append('\n');
      }
    }
    return sb.toString().trim();
  }

  private static String autre(List<String> exemples) {
    StringBuilder sb =
        new StringBuilder(
            "Je suis l'assistant matériel et logistique de la CSE. Je peux vous aider à localiser un engin, "
                + "vérifier une disponibilité, suivre un besoin, un contrat ou une livraison, consulter "
                + "l'historique de maintenance, le planning des affectations, les fournisseurs par catégorie "
                + "et les procédures internes. Je ne réponds pas aux questions hors de ce périmètre.");
    if (!exemples.isEmpty()) {
      sb.append("\n\nExemples de questions :\n");
      exemples.forEach(q -> sb.append(PUCE).append(q).append('\n'));
    }
    return sb.toString().trim();
  }

  private <T> String liste(List<T> elements, Function<T, String> rendu) {
    StringBuilder sb = new StringBuilder();
    elements.stream()
        .limit(maxElements)
        .forEach(x -> sb.append(PUCE).append(rendu.apply(x)).append('\n'));
    if (elements.size() > maxElements) {
      sb.append("_… et ").append(elements.size() - maxElements).append(" autre(s)._\n");
    }
    return sb.toString().trim();
  }

  private static String titre(String sujet, int n, String singulier, String pluriel) {
    return "**" + sujet + "** — " + n + " " + (n > 1 ? pluriel : singulier) + " :\n";
  }

  private static String aucun(String quoi, String objet, ContexteDonnees c, Section section) {
    if (c.masquees().contains(section)) {
      return "Ces informations ne sont pas accessibles à votre profil.";
    }
    if (c.indisponibles().contains(section)) {
      return "Je ne peux pas consulter ces données pour le moment (service momentanément indisponible). Réessayez dans quelques instants.";
    }
    return "Je n'ai trouvé "
        + quoi
        + (objet == null ? "" : " correspondant à « " + objet + " »")
        + " parmi les données auxquelles vous avez accès.";
  }

  private static String categorie(Entites e) {
    return e.categorie() == null ? null : Libelles.de(e.categorie());
  }

  private static String avertissements(ContexteDonnees c) {
    StringBuilder sb = new StringBuilder();
    Set<Section> partiel = c.indisponibles();
    if (!partiel.isEmpty() && !c.estVide()) {
      sb.append(
          "\n\n_Certaines données sont momentanément indisponibles : la réponse peut être incomplète._");
    }
    if (!c.masquees().isEmpty() && !c.estVide()) {
      sb.append("\n\n_Certaines informations ne sont pas accessibles à votre profil._");
    }
    return sb.toString();
  }
}
