package sn.cse.chatbot.domain.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sn.cse.chatbot.domain.model.AnalyseQuestion;
import sn.cse.chatbot.domain.model.Entites;
import sn.cse.chatbot.domain.model.Intention;

/**
 * Classification déterministe des questions en français : mots-clés pondérés appliqués au texte
 * normalisé (sans accents), références métier détectées par expressions régulières. Le score le
 * plus élevé l'emporte ; en cas d'égalité, l'ordre de {@link #PRIORITE} départage.
 */
public class ClassificateurIntention {

  private static final Pattern REF_BESOIN = Pattern.compile("\\bBES-\\d{4}-\\d{3,6}\\b");
  private static final Pattern REF_CONTRAT = Pattern.compile("\\bCTR-\\d{4}-\\d{3,6}\\b");
  private static final Pattern REF_AO = Pattern.compile("\\b(?:AO|DP)-\\d{4}-\\d{3,6}\\b");
  private static final Pattern REF_OT = Pattern.compile("\\bOT-\\d{4}-\\d{3,6}\\b");
  private static final Pattern CODE_CHANTIER = Pattern.compile("\\bCH-[A-Z]{3}-\\d{2}\\b");
  // Codes de matériel (PEL-001, CSE-NIV-02) et immatriculations sénégalaises (DK-1234-AB).
  private static final Pattern CODE_ENGIN =
      Pattern.compile("\\b(?:[A-Z]{2,5}-){1,2}\\d{2,5}(?:-[A-Z]{1,3})?\\b");
  private static final Set<String> PREFIXES_NON_ENGIN =
      Set.of("BES", "CTR", "AO", "DP", "OT", "CH");
  private static final Pattern QUANTITE =
      Pattern.compile("(?<![\\w-])(\\d{1,3})(?![\\w-])\\s*(?:x\\s*)?([a-z]+)?");
  private static final Pattern CHANTIER =
      Pattern.compile(
          "chantiers?\\s+(?:(?:de la|de l'|du|des|de|d'|a|au|en)\\s*)?([\\p{L}0-9'’ -]{2,60})",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  private static final Set<String> MOTS_ARRET =
      Set.of(
          "pour",
          "cette",
          "ce",
          "cet",
          "ces",
          "en",
          "et",
          "avec",
          "depuis",
          "jusqu'a",
          "jusqu",
          "semaine",
          "mois",
          "aujourd'hui",
          "demain",
          "hier",
          "actuellement",
          "maintenant",
          "est",
          "sont",
          "a",
          "au",
          "aux",
          "qui",
          "que",
          "quel",
          "quels",
          "quelle",
          "quelles",
          "dans",
          "sur",
          "il",
          "elle",
          "ont",
          "pendant",
          "prochain",
          "prochaine",
          "y",
          "ou",
          "svp",
          "s'il",
          "merci",
          "?",
          "actuel",
          "actuelle");
  private static final Set<String> ARTICLES =
      Set.of("le", "la", "les", "l'", "de", "du", "des", "d'");
  private static final Set<String> MOTS_MES_CHANTIERS =
      Set.of("mon chantier", "mes chantiers", "notre chantier", "nos chantiers");

  /** Ordre de départage en cas d'égalité de score. */
  static final List<Intention> PRIORITE =
      List.of(
          Intention.PROCEDURE,
          Intention.STATUT_BESOIN,
          Intention.STATUT_COMMANDE,
          Intention.HISTORIQUE_MAINTENANCE,
          Intention.FOURNISSEURS_PAR_CATEGORIE,
          Intention.DISPONIBILITE,
          Intention.PLANNING_AFFECTATION,
          Intention.LOCALISER_ENGIN);

  private final Map<Intention, List<MotCle>> motsCles = new EnumMap<>(Intention.class);
  private final Map<String, Materiel> vocabulaireMateriel = new LinkedHashMap<>();
  private final Map<String, String> categoriesGeneriques = new LinkedHashMap<>();
  private final Map<String, String> statutsBesoin = new LinkedHashMap<>();

  /** Mot-clé pondéré ; {@code *} final = préfixe (racine). */
  private record MotCle(Pattern motif, int poids) {
    static MotCle de(String expression, int poids) {
      boolean racine = expression.endsWith("*");
      String e = racine ? expression.substring(0, expression.length() - 1) : expression;
      return new MotCle(
          Pattern.compile("(?<![a-z0-9])" + Pattern.quote(e) + (racine ? "" : "(?![a-z0-9])")),
          poids);
    }
  }

  /** Désignation reconnue et sa catégorie. */
  private record Materiel(String terme, String categorie) {}

  /** Construit le classificateur et son vocabulaire métier. */
  public ClassificateurIntention() {
    mots(
        Intention.LOCALISER_ENGIN,
        3,
        "ou est",
        "ou se trouve",
        "ou se trouvent",
        "ou sont",
        "localis*",
        "ou est situe*",
        "position",
        "gps",
        "coordonnees");
    mots(
        Intention.LOCALISER_ENGIN,
        2,
        "trouver",
        "situe*",
        "sur quel chantier",
        "quel chantier",
        "carte",
        "emplacement");
    mots(
        Intention.DISPONIBILITE,
        3,
        "disponib*",
        "dispo",
        "libre",
        "libres",
        "en stock",
        "inoccupe*",
        "non affecte*");
    mots(
        Intention.DISPONIBILITE,
        2,
        "avons-nous",
        "a-t-on",
        "reste-t-il",
        "y a-t-il des",
        "combien de",
        "puis-je avoir");
    mots(
        Intention.STATUT_BESOIN,
        2,
        "besoin",
        "besoins",
        "expression de besoin",
        "ma demande",
        "mes demandes",
        "ou en est",
        "statut",
        "etat de",
        "avancement",
        "suivi de");
    mots(Intention.STATUT_BESOIN, 1, "demande", "valide", "rejete*", "en attente", "traite*");
    mots(
        Intention.STATUT_COMMANDE,
        3,
        "commande",
        "commandes",
        "contrat",
        "contrats",
        "appel d'offres",
        "appel d offres",
        "appels d'offres",
        "livraison",
        "livraisons",
        "bon de livraison",
        "engagement",
        "marche",
        "attribution",
        "facture",
        "factures");
    mots(
        Intention.STATUT_COMMANDE,
        2,
        "livre",
        "achat",
        "achats",
        "statut",
        "ou en est",
        "avancement",
        "fournisseur retenu");
    mots(
        Intention.HISTORIQUE_MAINTENANCE,
        3,
        "maintenance*",
        "entretien*",
        "revision*",
        "vidange*",
        "reparation*",
        "panne",
        "pannes",
        "ordre de travail",
        "intervention*",
        "historique");
    mots(Intention.HISTORIQUE_MAINTENANCE, 1, "en panne", "repare*", "technicien");
    mots(
        Intention.PLANNING_AFFECTATION,
        3,
        "planning",
        "plannings",
        "affectation*",
        "calendrier",
        "programmation",
        "planifie*",
        "affecte*");
    mots(
        Intention.PLANNING_AFFECTATION,
        2,
        "jusqu'a quand",
        "jusqu a quand",
        "date de fin",
        "se termine",
        "fin d'affectation",
        "retour",
        "prevu*",
        "semaine prochaine",
        "mois prochain",
        "quand");
    mots(
        Intention.FOURNISSEURS_PAR_CATEGORIE,
        3,
        "fournisseur",
        "fournisseurs",
        "prestataire*",
        "loueur*",
        "vendeur*",
        "qui fournit",
        "qui vend",
        "classement");
    mots(Intention.FOURNISSEURS_PAR_CATEGORIE, 2, "meilleur*", "partenaire*", "soumissionnaire*");
    mots(
        Intention.PROCEDURE,
        3,
        "comment",
        "procedure*",
        "etapes",
        "demarche*",
        "que dois-je faire",
        "que faire",
        "processus",
        "marche a suivre",
        "qui valide",
        "qui doit",
        "qui peut",
        "est-il possible de",
        "comment faire");
    mots(Intention.PROCEDURE, 2, "aide", "regle*", "workflow", "circuit", "faut-il");

    materiel(
        "MATERIEL_ROULANT",
        "pelle hydraulique",
        "pelle hydraulique",
        "pelle",
        "pelle",
        "pelles",
        "pelle",
        "pelleteuse",
        "pelleteuse",
        "pelleteuses",
        "pelleteuse",
        "excavatrice",
        "excavatrice",
        "excavatrices",
        "excavatrice",
        "niveleuse",
        "niveleuse",
        "niveleuses",
        "niveleuse",
        "bulldozer",
        "bulldozer",
        "bulldozers",
        "bulldozer",
        "chargeuse",
        "chargeuse",
        "chargeuses",
        "chargeuse",
        "compacteur",
        "compacteur",
        "compacteurs",
        "compacteur",
        "rouleau",
        "rouleau",
        "camion",
        "camion",
        "camions",
        "camion",
        "benne",
        "benne",
        "bennes",
        "benne",
        "grue",
        "grue",
        "grues",
        "grue",
        "tractopelle",
        "tractopelle",
        "tractopelles",
        "tractopelle",
        "chariot",
        "chariot",
        "finisseur",
        "finisseur",
        "toupie",
        "toupie",
        "dumper",
        "dumper",
        "vehicule",
        "véhicule",
        "vehicules",
        "véhicule",
        "pick-up",
        "pick-up",
        "4x4",
        "4x4");
    materiel(
        "OUTILLAGE",
        "groupe electrogene",
        "groupe électrogène",
        "groupes electrogenes",
        "groupe électrogène",
        "betonniere",
        "bétonnière",
        "betonnieres",
        "bétonnière",
        "perceuse",
        "perceuse",
        "perceuses",
        "perceuse",
        "marteau piqueur",
        "marteau",
        "marteau-piqueur",
        "marteau",
        "disqueuse",
        "disqueuse",
        "meuleuse",
        "meuleuse",
        "vibreur",
        "vibreur",
        "echafaudage",
        "échafaudage",
        "echafaudages",
        "échafaudage",
        "compresseur",
        "compresseur",
        "pompe",
        "pompe",
        "poste a souder",
        "poste à souder",
        "theodolite",
        "théodolite");
    materiel(
        "EQUIPEMENT_INFORMATIQUE",
        "ordinateur",
        "ordinateur",
        "ordinateurs",
        "ordinateur",
        "pc",
        "pc",
        "imprimante",
        "imprimante",
        "imprimantes",
        "imprimante",
        "tablette",
        "tablette",
        "tablettes",
        "tablette",
        "serveur",
        "serveur",
        "routeur",
        "routeur",
        "laptop",
        "laptop");
    materiel(
        "CONSOMMABLE",
        "carburant",
        "carburant",
        "gasoil",
        "gasoil",
        "gazole",
        "gasoil",
        "ciment",
        "ciment",
        "huile",
        "huile",
        "pneu",
        "pneu",
        "pneus",
        "pneu",
        "fer a beton",
        "fer à béton",
        "gravier",
        "gravier",
        "bitume",
        "bitume");
    generique(
        "MATERIEL_ROULANT",
        "materiel roulant",
        "materiels roulants",
        "engin",
        "engins",
        "roulant",
        "roulants");
    generique("OUTILLAGE", "outillage", "outillages", "outil", "outils");
    generique(
        "EQUIPEMENT_INFORMATIQUE",
        "informatique",
        "equipement informatique",
        "equipements informatiques");
    generique("CONSOMMABLE", "consommable", "consommables");
    statutsBesoin.put("en attente", "EN_ATTENTE");
    statutsBesoin.put("rejete", "REJETE");
    statutsBesoin.put("rejetes", "REJETE");
    statutsBesoin.put("valides", "VALIDE");
    statutsBesoin.put("en achat", "EN_ACHAT");
  }

  private void mots(Intention intention, int poids, String... expressions) {
    List<MotCle> liste = motsCles.computeIfAbsent(intention, k -> new ArrayList<>());
    for (String e : expressions) {
      liste.add(MotCle.de(e, poids));
    }
  }

  private void materiel(String categorie, String... paires) {
    for (int i = 0; i < paires.length; i += 2) {
      vocabulaireMateriel.put(paires[i], new Materiel(paires[i + 1], categorie));
    }
  }

  private void generique(String categorie, String... expressions) {
    for (String e : expressions) {
      categoriesGeneriques.put(e, categorie);
    }
  }

  /**
   * @param question question (déjà assainie)
   * @return l'intention et les entités
   */
  public AnalyseQuestion analyser(String question) {
    String normalise = NormaliseurTexte.normaliser(question);
    String majuscules = question == null ? "" : question.toUpperCase(Locale.ROOT);
    Entites entites = extraire(question == null ? "" : question, normalise, majuscules);
    return new AnalyseQuestion(classer(normalise, entites), entites, normalise);
  }

  private Intention classer(String texte, Entites e) {
    Map<Intention, Integer> scores = new EnumMap<>(Intention.class);
    motsCles.forEach(
        (intention, liste) -> {
          int score = 0;
          for (MotCle m : liste) {
            if (m.motif().matcher(texte).find()) {
              score += m.poids();
            }
          }
          scores.put(intention, score);
        });
    // Références explicites : signaux forts.
    if (!e.referencesBesoin().isEmpty()) {
      scores.merge(Intention.STATUT_BESOIN, 5, Integer::sum);
    }
    if (!e.referencesContrat().isEmpty() || !e.referencesAppelOffre().isEmpty()) {
      scores.merge(Intention.STATUT_COMMANDE, 5, Integer::sum);
    }
    boolean materielCite = e.rechercheMateriel().isPresent() || e.categorie() != null;
    if (materielCite) {
      // Un matériel cité sans autre indice est le plus souvent une question de localisation.
      scores.merge(Intention.LOCALISER_ENGIN, 1, Integer::sum);
    } else {
      // Sans matériel cité, « disponible » ou « où est » sont moins probants.
      scores.computeIfPresent(Intention.DISPONIBILITE, (k, v) -> v > 0 ? v - 1 : 0);
    }
    if (scores.getOrDefault(Intention.FOURNISSEURS_PAR_CATEGORIE, 0) > 0 && e.categorie() != null) {
      scores.merge(Intention.FOURNISSEURS_PAR_CATEGORIE, 1, Integer::sum);
    }
    int max = scores.values().stream().max(Integer::compare).orElse(0);
    if (max < 2) {
      // Un engin cité seul (« la grue ? ») : on le localise ; sinon hors périmètre.
      return materielCite ? Intention.LOCALISER_ENGIN : Intention.AUTRE;
    }
    return PRIORITE.stream()
        .filter(i -> scores.getOrDefault(i, 0) == max)
        .findFirst()
        .orElse(Intention.AUTRE);
  }

  private Entites extraire(String original, String normalise, String majuscules) {
    List<String> besoins = toutes(REF_BESOIN, majuscules);
    List<String> contrats = toutes(REF_CONTRAT, majuscules);
    List<String> aos = toutes(REF_AO, majuscules);
    List<String> engins = new ArrayList<>();
    Matcher m = CODE_ENGIN.matcher(majuscules);
    while (m.find()) {
      String code = m.group();
      if (!PREFIXES_NON_ENGIN.contains(code.substring(0, code.indexOf('-')))
          && !REF_OT.matcher(code).matches()) {
        engins.add(code);
      }
    }
    Optional<Materiel> materiel = trouverMateriel(normalise);
    String categorie =
        materiel.map(Materiel::categorie).orElseGet(() -> trouverCategorieGenerique(normalise));
    String chantier =
        toutes(CODE_CHANTIER, majuscules).stream().findFirst().orElseGet(() -> chantier(original));
    boolean mesChantiers = MOTS_MES_CHANTIERS.stream().anyMatch(normalise::contains);
    return new Entites(
        besoins,
        contrats,
        aos,
        List.copyOf(new LinkedHashSet<>(engins)),
        categorie,
        materiel.map(Materiel::terme).orElse(null),
        mesChantiers ? null : chantier,
        mesChantiers,
        quantite(normalise),
        besoins.isEmpty() ? statutBesoin(normalise) : null);
  }

  private Optional<Materiel> trouverMateriel(String texte) {
    // Les expressions les plus longues d'abord (« pelle hydraulique » avant « pelle »).
    return vocabulaireMateriel.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
        .filter(e -> MotCle.de(e.getKey(), 0).motif().matcher(texte).find())
        .map(Map.Entry::getValue)
        .findFirst();
  }

  private String trouverCategorieGenerique(String texte) {
    return categoriesGeneriques.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
        .filter(e -> MotCle.de(e.getKey(), 0).motif().matcher(texte).find())
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  private String statutBesoin(String texte) {
    return statutsBesoin.entrySet().stream()
        .filter(e -> MotCle.de(e.getKey(), 0).motif().matcher(texte).find())
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  private static Integer quantite(String texte) {
    Matcher m = QUANTITE.matcher(texte);
    while (m.find()) {
      int n = Integer.parseInt(m.group(1));
      String suivant = m.group(2);
      boolean unite =
          suivant != null
              && Set.of("h", "heures", "jours", "j", "km", "t", "tonnes", "mois", "ans")
                  .contains(suivant);
      if (n > 0 && !unite) {
        return n;
      }
    }
    return null;
  }

  private String chantier(String original) {
    Matcher m = CHANTIER.matcher(original);
    if (!m.find()) {
      return null;
    }
    List<String> mots = new ArrayList<>();
    for (String mot : m.group(1).trim().split("\\s+")) {
      String n = NormaliseurTexte.normaliser(mot);
      if (n.isEmpty() || MOTS_ARRET.contains(n) || mots.size() == 4) {
        break;
      }
      mots.add(mot.replaceAll("[?!.,;:]+$", ""));
    }
    // Les articles et prépositions ne terminent jamais un nom (« Thiès la [semaine] » → « Thiès »).
    while (!mots.isEmpty()
        && ARTICLES.contains(NormaliseurTexte.normaliser(mots.get(mots.size() - 1)))) {
      mots.remove(mots.size() - 1);
    }
    String resultat = String.join(" ", mots).trim();
    return resultat.length() < 2 ? null : resultat;
  }

  private static List<String> toutes(Pattern p, String texte) {
    Set<String> resultat = new LinkedHashSet<>();
    Matcher m = p.matcher(texte);
    while (m.find()) {
      resultat.add(m.group());
    }
    return List.copyOf(resultat);
  }
}
