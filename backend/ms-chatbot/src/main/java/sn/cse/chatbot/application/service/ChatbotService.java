package sn.cse.chatbot.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sn.cse.chatbot.application.ParametresChatbot;
import sn.cse.chatbot.application.exception.LimiteDebitDepasseeException;
import sn.cse.chatbot.application.exception.LlmIndisponibleException;
import sn.cse.chatbot.application.exception.SourceIndisponibleException;
import sn.cse.chatbot.application.port.in.QuestionCommand;
import sn.cse.chatbot.application.port.in.ReponseChatbot;
import sn.cse.chatbot.application.port.in.SuggestionsUseCase;
import sn.cse.chatbot.application.port.in.TraiterQuestionUseCase;
import sn.cse.chatbot.application.port.out.AchatsPort;
import sn.cse.chatbot.application.port.out.BesoinsPort;
import sn.cse.chatbot.application.port.out.CritereMateriel;
import sn.cse.chatbot.application.port.out.FournisseursPort;
import sn.cse.chatbot.application.port.out.LimiteurDebit;
import sn.cse.chatbot.application.port.out.LlmPort;
import sn.cse.chatbot.application.port.out.RequeteLlm;
import sn.cse.chatbot.application.port.out.SuiviPort;
import sn.cse.chatbot.domain.model.AnalyseQuestion;
import sn.cse.chatbot.domain.model.ChantierInfo;
import sn.cse.chatbot.domain.model.ContexteDonnees;
import sn.cse.chatbot.domain.model.Conversation;
import sn.cse.chatbot.domain.model.Entites;
import sn.cse.chatbot.domain.model.MaterielInfo;
import sn.cse.chatbot.domain.model.Message;
import sn.cse.chatbot.domain.model.Section;
import sn.cse.chatbot.domain.model.Source;
import sn.cse.chatbot.domain.service.AssainisseurTexte;
import sn.cse.chatbot.domain.service.BaseProcedures;
import sn.cse.chatbot.domain.service.CatalogueSources;
import sn.cse.chatbot.domain.service.ClassificateurIntention;
import sn.cse.chatbot.domain.service.GenerateurReponseDegradee;
import sn.cse.chatbot.domain.service.PolitiqueAcces;
import sn.cse.chatbot.domain.service.SerialiseurContexte;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;

/**
 * ChatbotService du diagramme de séquence : verifierRole → analyseIntention → getContexteData →
 * appliquerFiltreRole → genererReponse (LLM, sinon mode dégradé) → HistoriqueService.
 *
 * <p>Volontairement non transactionnel : l'appel au LLM ne doit pas immobiliser une connexion à la
 * base. Seul l'enregistrement final est transactionnel.
 */
@Service
public class ChatbotService implements TraiterQuestionUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(ChatbotService.class);
  private static final String DISPONIBLE = "DISPONIBLE";
  private static final int MAX_REFERENCES = 3;

  private final SuiviPort suivi;
  private final BesoinsPort besoins;
  private final AchatsPort achats;
  private final FournisseursPort fournisseurs;
  private final LlmPort llm;
  private final LimiteurDebit limiteur;
  private final HistoriqueService historique;
  private final SuggestionsUseCase suggestions;
  private final ParametresChatbot parametres;
  private final Clock clock;

  private final ClassificateurIntention classificateur = new ClassificateurIntention();
  private final PolitiqueAcces politique;
  private final CatalogueSources catalogue;
  private final BaseProcedures procedures;
  private final SerialiseurContexte serialiseur;
  private final GenerateurReponseDegradee degrade;

  /**
   * @param suivi ms-suivi
   * @param besoins ms-besoins
   * @param achats ms-achats
   * @param fournisseurs ms-fournisseurs
   * @param llm modèle de langage
   * @param limiteur limitation de débit
   * @param historique historique
   * @param suggestions questions types
   * @param parametres paramètres
   * @param clock horloge
   */
  public ChatbotService(
      SuiviPort suivi,
      BesoinsPort besoins,
      AchatsPort achats,
      FournisseursPort fournisseurs,
      LlmPort llm,
      LimiteurDebit limiteur,
      HistoriqueService historique,
      SuggestionsUseCase suggestions,
      ParametresChatbot parametres,
      Clock clock) {
    this.suivi = suivi;
    this.besoins = besoins;
    this.achats = achats;
    this.fournisseurs = fournisseurs;
    this.llm = llm;
    this.limiteur = limiteur;
    this.historique = historique;
    this.suggestions = suggestions;
    this.parametres = parametres;
    this.clock = clock;
    this.politique = new PolitiqueAcces(parametres.acces(), parametres.rolesMontants());
    this.catalogue = new CatalogueSources(parametres.liens(), parametres.sourcesMax());
    this.procedures = new BaseProcedures(parametres.procedures(), 2);
    this.serialiseur = new SerialiseurContexte(parametres.contexteMaxCaracteres());
    this.degrade = new GenerateurReponseDegradee(parametres.resultatsMax());
  }

  @Override
  public ReponseChatbot traiter(QuestionCommand commande) {
    CurrentUser utilisateur = commande.utilisateur();
    Set<Section> autorisees = verifierRole(utilisateur);
    if (!limiteur.autoriser(utilisateur.id())) {
      throw new LimiteDebitDepasseeException();
    }
    String question = valider(commande.question());
    Optional<Conversation> existante = historique.charger(utilisateur, commande.conversationId());

    AnalyseQuestion analyse = analyseIntention(question);
    ContexteDonnees brut = getContexteData(analyse, utilisateur, autorisees);
    ContexteDonnees filtre = appliquerFiltreRole(brut, utilisateur);
    List<Source> sources = catalogue.sources(filtre);
    List<Message> precedents =
        existante.map(c -> c.derniersMessages(parametres.messagesHistorique())).orElse(List.of());
    ReponseGeneree reponse = genererReponse(question, analyse, filtre, utilisateur, precedents);

    HistoriqueService.Echange echange =
        historique.enregistrer(
            utilisateur,
            commande.conversationId(),
            question,
            reponse,
            sources,
            analyse.intention(),
            clock.instant());
    LOG.info(
        "Question traitée : intention={}, sources={}, modeDegrade={}",
        analyse.intention(),
        sources.size(),
        reponse.modeDegrade());
    return new ReponseChatbot(
        echange.conversationId(),
        echange.reponse().id(),
        echange.reponse().contenu(),
        echange.reponse().sources(),
        analyse.intention(),
        reponse.modeDegrade(),
        echange.reponse().horodatage());
  }

  /** Étape 1 : rôle interne exigé ; détermine les sections consultables. */
  Set<Section> verifierRole(CurrentUser utilisateur) {
    return politique.verifierRole(utilisateur);
  }

  /** Étape 2 : intention et entités (classification déterministe). */
  AnalyseQuestion analyseIntention(String question) {
    return classificateur.analyser(question);
  }

  /**
   * Étape 3 : données réelles, lues avec le JWT de l'utilisateur ; panne → section indisponible.
   */
  ContexteDonnees getContexteData(AnalyseQuestion analyse, CurrentUser u, Set<Section> autorisees) {
    Entites e = analyse.entites();
    int max = parametres.resultatsMax();
    ContexteDonnees.Builder b = ContexteDonnees.builder();
    switch (analyse.intention()) {
      case LOCALISER_ENGIN ->
          collecter(
              b,
              Section.MATERIELS,
              autorisees,
              () -> suivi.rechercherMateriels(critere(e, null, u, autorisees), max),
              ContexteDonnees::avecMateriels);
      case DISPONIBILITE ->
          collecter(
              b,
              Section.MATERIELS,
              autorisees,
              () -> suivi.rechercherMateriels(critere(e, DISPONIBLE, u, autorisees), max),
              ContexteDonnees::avecMateriels);
      case STATUT_BESOIN ->
          collecter(
              b, Section.BESOINS, autorisees, () -> besoins(e, max), ContexteDonnees::avecBesoins);
      case STATUT_COMMANDE -> {
        boolean seulementAo =
            e.referencesContrat().isEmpty() && !e.referencesAppelOffre().isEmpty();
        boolean seulementContrat =
            !e.referencesContrat().isEmpty() && e.referencesAppelOffre().isEmpty();
        if (!seulementAo) {
          collecter(
              b,
              Section.CONTRATS,
              autorisees,
              () -> parReferences(e.referencesContrat(), q -> achats.rechercherContrats(q, max)),
              ContexteDonnees::avecContrats);
        }
        if (!seulementContrat) {
          collecter(
              b,
              Section.APPELS_OFFRES,
              autorisees,
              () ->
                  parReferences(
                      e.referencesAppelOffre(), q -> achats.rechercherAppelsOffres(q, max)),
              ContexteDonnees::avecAppelsOffres);
        }
      }
      case HISTORIQUE_MAINTENANCE -> maintenances(b, e, autorisees, max);
      case PLANNING_AFFECTATION -> planning(b, e, u, autorisees, max);
      case FOURNISSEURS_PAR_CATEGORIE ->
          collecter(
              b,
              Section.FOURNISSEURS,
              autorisees,
              () -> fournisseurs.classement(e.categorie(), max),
              ContexteDonnees::avecFournisseurs);
      case PROCEDURE ->
          collecter(
              b,
              Section.PROCEDURES,
              autorisees,
              () -> procedures.rechercher(analyse.texteNormalise()),
              ContexteDonnees::avecProcedures);
      case AUTRE -> {
        // Hors périmètre : aucune donnée n'est consultée.
      }
    }
    return b.build();
  }

  /** Étape 4 : sections interdites retirées, montants masqués selon le rôle. */
  ContexteDonnees appliquerFiltreRole(ContexteDonnees contexte, CurrentUser utilisateur) {
    return politique.appliquerFiltreRole(contexte, utilisateur);
  }

  /** Étape 5 : réponse du LLM sur les seules données fournies ; à défaut, mode dégradé. */
  ReponseGeneree genererReponse(
      String question,
      AnalyseQuestion analyse,
      ContexteDonnees contexte,
      CurrentUser utilisateur,
      List<Message> precedents) {
    if (llm.estDisponible()) {
      try {
        String texte =
            AssainisseurTexte.nettoyer(
                llm.generer(
                    new RequeteLlm(
                        question,
                        serialiseur.serialiser(contexte),
                        analyse.intention(),
                        profil(utilisateur),
                        LocalDate.now(clock),
                        precedents)));
        if (!texte.isBlank()) {
          return new ReponseGeneree(
              AssainisseurTexte.tronquer(texte, parametres.reponseMaxCaracteres()), false);
        }
        LOG.warn("Réponse vide du LLM : bascule en mode dégradé");
      } catch (LlmIndisponibleException ex) {
        LOG.warn("LLM indisponible ({}) : bascule en mode dégradé", ex.getMessage());
      }
    }
    return new ReponseGeneree(
        degrade.generer(analyse, contexte, suggestions.suggestions(utilisateur)), true);
  }

  private String valider(String brute) {
    String question = AssainisseurTexte.nettoyer(brute);
    if (question.isBlank()) {
      throw new InvalidRequestException("QUESTION_VIDE", "La question est vide.");
    }
    if (question.length() > parametres.questionMaxCaracteres()) {
      throw new InvalidRequestException(
          "QUESTION_TROP_LONGUE",
          "La question dépasse " + parametres.questionMaxCaracteres() + " caractères.");
    }
    return question;
  }

  private CritereMateriel critere(Entites e, String etat, CurrentUser u, Set<Section> autorisees) {
    String chantierId = null;
    if (e.rechercheMateriel().isEmpty() && (e.chantier() != null || e.mesChantiers())) {
      chantierId = chantiersCibles(e, u, autorisees).stream().findFirst().orElse(null);
    }
    return new CritereMateriel(e.rechercheMateriel().orElse(null), e.categorie(), etat, chantierId);
  }

  private List<sn.cse.chatbot.domain.model.BesoinInfo> besoins(Entites e, int max) {
    if (e.referencesBesoin().isEmpty()) {
      return besoins.rechercher(null, e.statutBesoin(), max);
    }
    return parReferences(e.referencesBesoin(), ref -> besoins.rechercher(ref, null, max));
  }

  private void maintenances(
      ContexteDonnees.Builder b, Entites e, Set<Section> autorisees, int max) {
    Optional<String> recherche = e.rechercheMateriel();
    if (recherche.isEmpty()) {
      collecter(
          b,
          Section.MAINTENANCES,
          autorisees,
          () -> suivi.maintenances(null, max),
          ContexteDonnees::avecMaintenances);
      return;
    }
    List<MaterielInfo> trouves = new ArrayList<>();
    collecter(
        b,
        Section.MATERIELS,
        autorisees,
        () -> {
          trouves.addAll(
              suivi.rechercherMateriels(
                  new CritereMateriel(recherche.get(), e.categorie(), null, null), MAX_REFERENCES));
          return trouves;
        },
        ContexteDonnees::avecMateriels);
    if (!trouves.isEmpty()) {
      collecter(
          b,
          Section.MAINTENANCES,
          autorisees,
          () ->
              trouves.stream()
                  .flatMap(m -> suivi.maintenances(m.id(), max).stream())
                  .limit(max)
                  .toList(),
          ContexteDonnees::avecMaintenances);
    }
  }

  private void planning(
      ContexteDonnees.Builder b, Entites e, CurrentUser u, Set<Section> autorisees, int max) {
    LocalDate du = LocalDate.now(clock);
    LocalDate au = du.plusDays(parametres.horizonPlanningJours());
    Optional<String> code =
        e.codeEngin()
            .or(() -> Optional.ofNullable(e.chantier() == null ? e.termeMateriel() : null));
    if (code.isPresent()) {
      List<MaterielInfo> trouves = new ArrayList<>();
      collecter(
          b,
          Section.MATERIELS,
          autorisees,
          () -> {
            trouves.addAll(
                suivi.rechercherMateriels(
                    new CritereMateriel(code.get(), e.categorie(), null, null), MAX_REFERENCES));
            return trouves;
          },
          ContexteDonnees::avecMateriels);
      collecter(
          b,
          Section.AFFECTATIONS,
          autorisees,
          () ->
              trouves.stream()
                  .flatMap(m -> suivi.affectationsDuMateriel(m.id(), max).stream())
                  .limit(max)
                  .toList(),
          ContexteDonnees::avecAffectations);
      return;
    }
    List<String> chantiers = new ArrayList<>();
    if (e.chantier() != null || e.mesChantiers() || u.estRestreintAuxChantiers()) {
      try {
        chantiers.addAll(chantiersCibles(e, u, autorisees));
      } catch (SourceIndisponibleException ex) {
        b.indisponible(Section.AFFECTATIONS);
        return;
      }
      if (chantiers.isEmpty()) {
        return;
      }
    }
    collecter(
        b,
        Section.AFFECTATIONS,
        autorisees,
        () ->
            chantiers.isEmpty()
                ? suivi.planning(null, du, au, max)
                : chantiers.stream()
                    .limit(MAX_REFERENCES)
                    .flatMap(id -> suivi.planning(id, du, au, max).stream())
                    .limit(max)
                    .toList(),
        ContexteDonnees::avecAffectations);
  }

  private List<String> chantiersCibles(Entites e, CurrentUser u, Set<Section> autorisees) {
    if (e.chantier() != null && autorisees.contains(Section.CHANTIERS)) {
      return suivi.rechercherChantiers(e.chantier(), MAX_REFERENCES).stream()
          .map(ChantierInfo::id)
          .filter(id -> u.peutAccederAuChantier(java.util.UUID.fromString(id)))
          .toList();
    }
    return u.chantierIds().stream()
        .map(java.util.UUID::toString)
        .sorted()
        .collect(Collectors.toList());
  }

  private static <T> List<T> parReferences(
      List<String> references, java.util.function.Function<String, List<T>> recherche) {
    if (references.isEmpty()) {
      return recherche.apply(null);
    }
    return references.stream()
        .limit(MAX_REFERENCES)
        .flatMap(r -> recherche.apply(r).stream())
        .distinct()
        .toList();
  }

  private static <T> void collecter(
      ContexteDonnees.Builder b,
      Section section,
      Set<Section> autorisees,
      Supplier<List<T>> lecture,
      BiFunction<ContexteDonnees, List<T>, ContexteDonnees> ajout) {
    if (!autorisees.contains(section)) {
      b.masquee(section);
      return;
    }
    try {
      List<T> valeurs = lecture.get();
      b.avec(c -> ajout.apply(c, valeurs));
    } catch (SourceIndisponibleException ex) {
      LOG.warn("Section {} indisponible : {}", section, ex.getMessage());
      b.indisponible(section);
    }
  }

  private static String profil(CurrentUser u) {
    return Roles.INTERNES.stream().filter(u::hasRole).collect(Collectors.joining(", "));
  }
}
