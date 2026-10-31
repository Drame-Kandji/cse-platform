package sn.cse.achats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static sn.cse.achats.Donnees.F1;
import static sn.cse.achats.Donnees.F2;
import static sn.cse.achats.Donnees.SEUIL;
import static sn.cse.achats.Donnees.SUSPENDU;
import static sn.cse.achats.Donnees.T0;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sn.cse.achats.Donnees;
import sn.cse.achats.application.command.BesoinEnAchatCommand;
import sn.cse.achats.application.command.ConfirmerLivraisonCommand;
import sn.cse.achats.application.command.CreerAppelOffreCommand;
import sn.cse.achats.application.command.DecisionCommand;
import sn.cse.achats.application.command.DeposerOffreCommand;
import sn.cse.achats.application.command.EvaluerOffreCommand;
import sn.cse.achats.application.command.FournisseurObserveCommand;
import sn.cse.achats.application.command.ModifierClausesCommand;
import sn.cse.achats.application.command.ProposerAttributionCommand;
import sn.cse.achats.application.command.RecevoirFactureCommand;
import sn.cse.achats.application.port.in.DocumentPdf;
import sn.cse.achats.application.port.in.OffreFournisseur;
import sn.cse.achats.application.port.in.OffreVue;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.application.port.out.ProcesVerbalGenerator;
import sn.cse.achats.application.service.AttributionMarcheService;
import sn.cse.achats.application.service.ClotureAppelOffreService;
import sn.cse.achats.application.service.ConsultationAppelsOffresService;
import sn.cse.achats.application.service.ConsultationContratsService;
import sn.cse.achats.application.service.ConsultationFinanceService;
import sn.cse.achats.application.service.DepotOffreService;
import sn.cse.achats.application.service.EngagementService;
import sn.cse.achats.application.service.EvaluationOffresService;
import sn.cse.achats.application.service.ExecutionContratService;
import sn.cse.achats.application.service.FactureService;
import sn.cse.achats.application.service.GenerationContratService;
import sn.cse.achats.application.service.ParametresAchats;
import sn.cse.achats.application.service.PortailFournisseurService;
import sn.cse.achats.application.service.ProcesVerbalService;
import sn.cse.achats.application.service.ProjectionsService;
import sn.cse.achats.application.service.PublicationAppelOffreService;
import sn.cse.achats.application.service.SurveillanceLivraisonsService;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.BesoinAAcheter;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.ClausesContrat;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.CriterePondere;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.model.NouveauJalon;
import sn.cse.achats.domain.model.Priorite;
import sn.cse.achats.domain.model.StatutAO;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.achats.domain.model.StatutContrat;
import sn.cse.achats.domain.model.StatutFacture;
import sn.cse.achats.domain.model.StatutFournisseur;
import sn.cse.achats.domain.model.TypeAppelOffre;
import sn.cse.achats.domain.model.TypeProcesVerbal;
import sn.cse.achats.domain.service.MoteurEvaluation;
import sn.cse.achats.domain.service.SyntheseFinance;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.CseException;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;
import sn.cse.events.EventTypes;

/** Cas d'utilisation de ms-achats avec des ports en mémoire (aucun framework). */
class CasUtilisationAchatsTest {

  private static final PageQuery PAGE = new PageQuery(0, 20, null, true);
  private static final UUID BESOIN = UUID.fromString("b0000000-0000-0000-0000-000000000001");
  private static final CurrentUser RA =
      new CurrentUser(
          Donnees.RA.id(),
          "Fatou Sarr",
          "ra@cse.sn",
          Set.of(Roles.RESPONSABLE_ACHATS),
          Set.of(),
          null);
  private static final CurrentUser RM =
      new CurrentUser(
          Donnees.RM.id(),
          "Ibrahima Fall",
          null,
          Set.of(Roles.RESPONSABLE_MATERIEL),
          Set.of(),
          null);
  private static final CurrentUser DG =
      new CurrentUser(
          Donnees.DG.id(), "Aminata Sow", null, Set.of(Roles.DIRECTION_GENERALE), Set.of(), null);
  private static final CurrentUser DAF =
      new CurrentUser(Donnees.DAF.id(), "Ousmane Ba", null, Set.of(Roles.DAF), Set.of(), null);
  private static final CurrentUser PORTAIL_F1 =
      new CurrentUser(
          UUID.randomUUID(), "Moussa Kane", null, Set.of(Roles.FOURNISSEUR), Set.of(), F1.id());
  private static final CurrentUser PORTAIL_SANS_FOURNISSEUR =
      new CurrentUser(UUID.randomUUID(), " ", null, Set.of(Roles.FOURNISSEUR), Set.of(), null);

  private FauxPorts ports;
  private FauxPorts.Horloge horloge;
  private ProcesVerbalGenerator generateurPdf;

  private PublicationAppelOffreService publication;
  private DepotOffreService depot;
  private ClotureAppelOffreService cloture;
  private EvaluationOffresService evaluation;
  private AttributionMarcheService attribution;
  private GenerationContratService generation;
  private ConsultationAppelsOffresService consultation;
  private ProcesVerbalService procesVerbaux;
  private ExecutionContratService execution;
  private EngagementService engagements;
  private SurveillanceLivraisonsService surveillance;
  private ConsultationContratsService consultationContrats;
  private FactureService facturation;
  private ConsultationFinanceService finance;
  private ProjectionsService projections;
  private PortailFournisseurService portail;

  @BeforeEach
  void preparer() {
    ports = new FauxPorts();
    horloge = new FauxPorts.Horloge(T0);
    generateurPdf = mock(ProcesVerbalGenerator.class);
    ParametresAchats parametres = new ParametresAchats(SEUIL, Donnees.PARAMETRES);
    MoteurEvaluation moteur = MoteurEvaluation.standard();
    AppelOffreRepository aos = ports.appelOffreRepository();
    ContratRepository contrats = ports.contratRepository();
    FactureRepository factures = ports.factureRepository();
    publication =
        new PublicationAppelOffreService(
            aos,
            ports.besoinRepository(),
            ports.annuaire(),
            ports.references(),
            ports.publisher(),
            horloge);
    depot =
        new DepotOffreService(
            aos, ports.annuaire(), ports.projectionRepository(), ports.publisher(), horloge);
    cloture = new ClotureAppelOffreService(aos, ports.publisher(), horloge);
    evaluation = new EvaluationOffresService(aos, moteur);
    generation =
        new GenerationContratService(
            aos, contrats, ports.references(), ports.publisher(), parametres, horloge);
    attribution =
        new AttributionMarcheService(aos, generation, ports.publisher(), parametres, horloge);
    consultation = new ConsultationAppelsOffresService(aos, ports.besoinRepository(), moteur);
    procesVerbaux = new ProcesVerbalService(aos, generateurPdf, moteur, horloge);
    execution =
        new ExecutionContratService(contrats, factures, ports.publisher(), parametres, horloge);
    engagements = new EngagementService(contrats, ports.publisher(), horloge);
    surveillance =
        new SurveillanceLivraisonsService(contrats, ports.publisher(), parametres, horloge);
    consultationContrats = new ConsultationContratsService(contrats);
    facturation = new FactureService(factures, contrats, ports.publisher(), horloge);
    finance = new ConsultationFinanceService(factures, contrats, horloge);
    projections = new ProjectionsService(ports.besoinRepository(), ports.projectionRepository());
    portail = new PortailFournisseurService(aos, contrats, factures, depot, facturation, horloge);
    ports.distants.put(F1.id(), F1);
    ports.distants.put(F2.id(), F2);
    ports.distants.put(SUSPENDU.id(), SUSPENDU);
  }

  private static String code(Throwable t) {
    return ((CseException) t).getCode();
  }

  private void recevoirBesoin() {
    projections.besoinEnAchat(
        new BesoinEnAchatCommand(
            BESOIN,
            "BES-2026-0001",
            "Pelle hydraulique 20 t",
            CategorieMateriel.MATERIEL_ROULANT,
            2,
            "Godet 1,2 m3",
            Priorite.URGENT,
            Donnees.BESOIN.chantierId(),
            "Extension autoroute Dakar–AIBD",
            T0.minusSeconds(3600)));
  }

  private CreerAppelOffreCommand commande(UUID besoinId, TypeAppelOffre type, List<UUID> invites) {
    return new CreerAppelOffreCommand(
        besoinId,
        "Acquisition de deux pelles hydrauliques 20 t",
        null,
        type,
        CategorieMateriel.MATERIEL_ROULANT,
        2,
        null,
        new BigDecimal("320000000"),
        T0.plus(Duration.ofDays(10)),
        List.of(
            new CriterePondere(CritereEvaluation.PRIX, 60),
            new CriterePondere(CritereEvaluation.DELAI, 40)),
        invites,
        RA);
  }

  private AppelOffre publierAo() {
    return publication.publier(
        commande(null, TypeAppelOffre.APPEL_OFFRES, List.of(F1.id(), F2.id())));
  }

  private OffreVue saisir(AppelOffre ao, UUID fournisseur, String montant, int delai) {
    return depot.saisirOffre(
        new DeposerOffreCommand(
            ao.getId(), fournisseur, new BigDecimal(montant), delai, 12, null, RA));
  }

  /** AO clôturé avec F1 (montant1) et F2 (montant2), même délai. */
  private AppelOffre aoCloture(String montant1, String montant2) {
    AppelOffre ao = publierAo();
    saisir(ao, F1.id(), montant1, 30);
    saisir(ao, F2.id(), montant2, 30);
    horloge.regler(T0.plus(Duration.ofDays(11)));
    return cloture.cloturer(ao.getId());
  }

  private UUID offreDe(AppelOffre ao, UUID fournisseur) {
    return ports.appelsOffres.get(ao.getId()).offreDu(fournisseur).orElseThrow().getId();
  }

  /** Marché attribué à F1 sous le seuil, contrat BROUILLON généré. */
  private ContratAchat contratGenere() {
    AppelOffre ao = aoCloture("120000000", "130000000");
    attribution.proposer(
        new ProposerAttributionCommand(ao.getId(), offreDe(ao, F1.id()), "Moins cher", RA));
    attribution.coValider(new DecisionCommand(ao.getId(), true, null, RM));
    return ports.contrats.values().iterator().next();
  }

  private ContratAchat contratEngage() {
    ContratAchat c = contratGenere();
    execution.signer(c.getId());
    return engagements.decider(new DecisionCommand(c.getId(), true, "OK", DAF));
  }

  @Nested
  class Publication {

    @Test
    void should_publier_et_rattacher_le_besoin() {
      recevoirBesoin();

      AppelOffre ao =
          publication.publier(
              commande(BESOIN, TypeAppelOffre.DEMANDE_PRIX, List.of(F1.id(), F1.id())));

      assertThat(ao.getReference()).isEqualTo("AO-2026-001");
      assertThat(ao.getInvites()).hasSize(1);
      assertThat(ao.getOrigine().orElseThrow().chantierNom())
          .isEqualTo("Extension autoroute Dakar–AIBD");
      assertThat(ports.besoins.get(BESOIN).appelOffreId()).isEqualTo(ao.getId());
      assertThat(ports.evenements).containsExactly(EventTypes.AO_PUBLIE);
      assertThat(consultation.besoinsAAcheter(PAGE).content()).hasSize(1);
    }

    @Test
    void should_refuser_besoin_inconnu_ou_deja_en_achat() {
      assertThatThrownBy(
              () -> publication.publier(commande(BESOIN, TypeAppelOffre.APPEL_OFFRES, List.of())))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.BESOIN_NON_EN_ACHAT));
      recevoirBesoin();
      publication.publier(commande(BESOIN, TypeAppelOffre.APPEL_OFFRES, List.of()));

      assertThatThrownBy(
              () -> publication.publier(commande(BESOIN, TypeAppelOffre.APPEL_OFFRES, List.of())))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.BESOIN_DEJA_EN_ACHAT));
    }

    @Test
    void should_refuser_invite_inconnu_ou_suspendu_R14() {
      assertThatThrownBy(
              () ->
                  publication.publier(
                      commande(null, TypeAppelOffre.DEMANDE_PRIX, List.of(UUID.randomUUID()))))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.FOURNISSEUR_INCONNU));
      assertThatThrownBy(
              () ->
                  publication.publier(
                      commande(null, TypeAppelOffre.DEMANDE_PRIX, List.of(SUSPENDU.id()))))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.FOURNISSEUR_NON_ELIGIBLE));
      assertThat(ports.appelsOffres).isEmpty();
    }
  }

  @Nested
  class Depot {

    @Test
    void should_saisir_offre_anonymisee_tant_que_publie_R10() {
      AppelOffre ao = publierAo();

      OffreVue vue = saisir(ao, F1.id(), "298000000", 60);

      assertThat(vue.anonyme()).isTrue();
      assertThat(vue.evaluation().notes()).isEmpty();
      assertThat(consultation.offres(ao.getId()))
          .singleElement()
          .satisfies(o -> assertThat(o.anonyme()).isTrue());
      assertThat(ports.evenements).containsExactly(EventTypes.AO_PUBLIE, EventTypes.OFFRE_DEPOSEE);
    }

    @Test
    void should_refuser_offre_apres_date_limite_R02() {
      AppelOffre ao = publierAo();
      horloge.regler(ao.getDateLimite().plusSeconds(1));

      assertThatThrownBy(() -> saisir(ao, F1.id(), "100", 10))
          .isInstanceOf(BusinessRuleException.class)
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.DATE_LIMITE_DEPASSEE));
    }

    @Test
    void should_refuser_offre_d_un_fournisseur_suspendu_ou_inconnu_R14() {
      AppelOffre ao = publierAo();

      assertThatThrownBy(() -> saisir(ao, SUSPENDU.id(), "100", 10))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.FOURNISSEUR_NON_ELIGIBLE));
      assertThatThrownBy(() -> saisir(ao, UUID.randomUUID(), "100", 10))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.FOURNISSEUR_INCONNU));
      assertThatThrownBy(
              () ->
                  depot.saisirOffre(
                      new DeposerOffreCommand(
                          UUID.randomUUID(), F1.id(), BigDecimal.TEN, 1, null, null, RA)))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_lire_le_statut_local_pour_le_portail_R14() {
      AppelOffre ao = publierAo();
      projections.fournisseurObserve(
          new FournisseurObserveCommand(
              F1.id(), F1.raisonSociale(), StatutFournisseur.SUSPENDU, T0));

      assertThatThrownBy(
              () -> portail.deposerOffre(PORTAIL_F1, ao.getId(), BigDecimal.TEN, 10, null, null))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.FOURNISSEUR_NON_ELIGIBLE));

      projections.fournisseurObserve(
          new FournisseurObserveCommand(
              F1.id(), F1.raisonSociale(), StatutFournisseur.ACTIF, T0.plusSeconds(1)));
      OffreFournisseur offre =
          portail.deposerOffre(PORTAIL_F1, ao.getId(), BigDecimal.TEN, 10, 6, "Offre portail");

      assertThat(offre.offre().getFournisseurId()).isEqualTo(F1.id());
      assertThat(portail.mesOffres(PORTAIL_F1, PAGE).content())
          .singleElement()
          .satisfies(o -> assertThat(o.appelOffre().getId()).isEqualTo(ao.getId()));
      assertThat(portail.appelsOffresOuverts(PORTAIL_F1, PAGE).content()).hasSize(1);
    }

    @Test
    void should_ignorer_un_evenement_fournisseur_plus_ancien() {
      projections.fournisseurObserve(
          new FournisseurObserveCommand(
              F1.id(), F1.raisonSociale(), StatutFournisseur.SUSPENDU, T0));
      projections.fournisseurObserve(
          new FournisseurObserveCommand(
              F1.id(), F1.raisonSociale(), StatutFournisseur.ACTIF, T0.minusSeconds(60)));

      assertThat(ports.projection.get(F1.id()).statut()).isEqualTo(StatutFournisseur.SUSPENDU);
    }
  }

  @Nested
  class ClotureEtEvaluation {

    @Test
    void should_cloturer_automatiquement_les_ao_echus() {
      AppelOffre echu = publierAo();
      CreerAppelOffreCommand base = commande(null, TypeAppelOffre.APPEL_OFFRES, List.of());
      AppelOffre ouvert =
          publication.publier(
              new CreerAppelOffreCommand(
                  null,
                  base.objet(),
                  null,
                  base.type(),
                  base.categorie(),
                  1,
                  null,
                  null,
                  T0.plus(Duration.ofDays(20)),
                  base.criteres(),
                  List.of(),
                  RA));
      horloge.regler(echu.getDateLimite());

      assertThat(cloture.cloturerEchus()).isEqualTo(1);
      assertThat(ports.appelsOffres.get(echu.getId()).getStatut()).isEqualTo(StatutAO.CLOTURE);
      assertThat(ports.appelsOffres.get(ouvert.getId()).getStatut()).isEqualTo(StatutAO.PUBLIE);
      assertThat(ports.evenements).contains(EventTypes.AO_CLOTURE);
      assertThat(cloture.cloturerEchus()).isZero();
    }

    @Test
    void should_lever_anonymat_et_comparer_apres_cloture_R10() {
      AppelOffre ao = aoCloture("100000000", "80000000");

      var comparatif = evaluation.comparatif(ao.getId());

      assertThat(comparatif.recommandationOffreId()).isEqualTo(offreDe(ao, F2.id()));
      assertThat(consultation.offres(ao.getId()))
          .allSatisfy(o -> assertThat(o.anonyme()).isFalse());
      assertThat(consultation.detail(ao.getId()).getStatut()).isEqualTo(StatutAO.CLOTURE);
      assertThat(
              consultation
                  .rechercher(new AppelOffreRepository.Filtre(StatutAO.CLOTURE, null, null), PAGE)
                  .content())
          .hasSize(1);
    }

    @Test
    void should_noter_une_offre_et_renvoyer_son_evaluation() {
      AppelOffre ao =
          publication.publier(
              new CreerAppelOffreCommand(
                  null,
                  "Groupes électrogènes",
                  null,
                  TypeAppelOffre.APPEL_OFFRES,
                  CategorieMateriel.OUTILLAGE,
                  4,
                  null,
                  null,
                  T0.plus(Duration.ofDays(3)),
                  List.of(
                      new CriterePondere(CritereEvaluation.PRIX, 50),
                      new CriterePondere(CritereEvaluation.SAV, 50)),
                  List.of(),
                  RA));
      saisir(ao, F1.id(), "40000000", 20);
      horloge.regler(T0.plus(Duration.ofDays(4)));
      cloture.cloturerEchus();

      OffreVue vue =
          evaluation.evaluer(
              new EvaluerOffreCommand(
                  ao.getId(), offreDe(ao, F1.id()), Map.of(CritereEvaluation.SAV, 80)));

      assertThat(vue.evaluation().scoreGlobal()).isEqualByComparingTo("90.00");
      assertThat(vue.evaluation().rang()).isEqualTo(1);
    }

    @Test
    void should_produire_proces_verbal_selon_statut() {
      AppelOffre ao = publierAo();
      assertThatThrownBy(() -> procesVerbaux.generer(ao.getId(), TypeProcesVerbal.OUVERTURE))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.PV_INDISPONIBLE));
      horloge.regler(ao.getDateLimite());
      cloture.cloturerEchus();
      when(generateurPdf.generer(eq(TypeProcesVerbal.OUVERTURE), any(), anyList(), any()))
          .thenReturn(new byte[] {'%', 'P', 'D', 'F'});

      DocumentPdf pdf = procesVerbaux.generer(ao.getId(), TypeProcesVerbal.OUVERTURE);

      assertThat(pdf.nomFichier()).isEqualTo("PV-ouverture-AO-2026-001.pdf");
      assertThat(pdf.contenu()).startsWith((byte) '%', (byte) 'P');
    }
  }

  @Nested
  class Attribution {

    @Test
    void should_attribuer_apres_co_validation_et_generer_contrat_sous_seuil_R11() {
      ContratAchat contrat = contratGenere();

      assertThat(contrat.getStatut()).isEqualTo(StatutContrat.BROUILLON);
      assertThat(contrat.getNumero()).isEqualTo("CTR-2026-001");
      assertThat(ports.evenements)
          .containsSubsequence(
              EventTypes.ATTRIBUTION_PROPOSEE,
              EventTypes.MARCHE_ATTRIBUE,
              EventTypes.CONTRAT_GENERE);
    }

    @Test
    void should_attendre_approbation_dg_au_dela_du_seuil_R11() {
      AppelOffre ao = aoCloture("298000000", "312000000");
      attribution.proposer(
          new ProposerAttributionCommand(ao.getId(), offreDe(ao, F1.id()), "Meilleur score", RA));

      AppelOffre apresRm = attribution.coValider(new DecisionCommand(ao.getId(), true, null, RM));

      assertThat(apresRm.getStatut()).isEqualTo(StatutAO.CLOTURE);
      assertThat(ports.contrats).isEmpty();
      AppelOffre apresDg =
          attribution.approuver(new DecisionCommand(ao.getId(), true, "Approuvé", DG));
      assertThat(apresDg.getStatut()).isEqualTo(StatutAO.ATTRIBUE);
      assertThat(ports.contrats).hasSize(1);
    }

    @Test
    void should_supprimer_proposition_et_publier_refus_R11() {
      AppelOffre ao = aoCloture("298000000", "312000000");
      attribution.proposer(
          new ProposerAttributionCommand(ao.getId(), offreDe(ao, F1.id()), "Meilleur score", RA));

      AppelOffre refuse =
          attribution.approuver(new DecisionCommand(ao.getId(), false, "Budget", DG));

      assertThat(refuse.getAttribution()).isEmpty();
      assertThat(ports.evenements).contains(EventTypes.ATTRIBUTION_REFUSEE);
    }

    @Test
    void should_refuser_second_contrat_pour_un_meme_ao_R03() {
      ContratAchat contrat = contratGenere();

      assertThatThrownBy(() -> generation.generer(contrat.getAppelOffreId()))
          .isInstanceOf(ConflictException.class)
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.CONTRAT_EXISTANT));
    }

    @Test
    void should_refuser_contrat_pour_ao_non_attribue_R03() {
      AppelOffre ao = aoCloture("100", "110");

      assertThatThrownBy(() -> generation.generer(ao.getId()))
          .isInstanceOf(BusinessRuleException.class)
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.AO_NON_ATTRIBUE));
      assertThatThrownBy(() -> generation.generer(UUID.randomUUID()))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  class Execution {

    @Test
    void should_signer_et_demander_engagement() {
      ContratAchat c = contratGenere();
      execution.modifierClauses(
          new ModifierClausesCommand(
              c.getId(),
              new ClausesContrat(24, null, "Franco", new BigDecimal("0.2")),
              20,
              List.of(new NouveauJalon("Expédition", LocalDate.of(2026, 10, 25)))));

      ContratAchat signe = execution.signer(c.getId());

      assertThat(signe.getStatut()).isEqualTo(StatutContrat.SIGNE);
      assertThat(signe.getJalons()).hasSize(1);
      assertThat(ports.evenements)
          .containsSubsequence(EventTypes.CONTRAT_SIGNE, EventTypes.ENGAGEMENT_DEMANDE);
      assertThat(consultationContrats.engagements(StatutApprobation.EN_ATTENTE, PAGE).content())
          .hasSize(1);
      assertThat(consultationContrats.detail(c.getId()).getNumero()).isEqualTo("CTR-2026-001");
    }

    @Test
    void should_refuser_livraison_tant_que_engagement_non_valide_R06() {
      ContratAchat c = contratGenere();
      execution.signer(c.getId());

      assertThatThrownBy(() -> execution.demarrerLivraison(c.getId()))
          .isInstanceOf(BusinessRuleException.class)
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.ENGAGEMENT_NON_VALIDE));
    }

    @Test
    void should_livrer_apres_validation_daf_R06() {
      ContratAchat c = contratEngage();
      assertThat(ports.evenements).contains(EventTypes.ENGAGEMENT_VALIDE);
      execution.demarrerLivraison(c.getId());
      LocalDate aujourdhui = LocalDate.ofInstant(horloge.instant(), ZoneOffset.UTC);

      ContratAchat livre =
          execution.confirmerLivraison(
              new ConfirmerLivraisonCommand(c.getId(), aujourdhui, "BL-1", null));

      assertThat(livre.getStatut()).isEqualTo(StatutContrat.LIVRE);
      assertThat(ports.evenements)
          .containsSubsequence(EventTypes.LIVRAISON_DEMARREE, EventTypes.LIVRAISON_EFFECTUEE);
      assertThat(execution.cloturer(c.getId()).getStatut()).isEqualTo(StatutContrat.CLOTURE);
      assertThat(ports.evenements).contains(EventTypes.CONTRAT_CLOTURE);
    }

    @Test
    void should_renvoyer_en_brouillon_et_publier_rejet_engagement() {
      ContratAchat c = contratGenere();
      execution.signer(c.getId());

      ContratAchat rejete =
          engagements.decider(new DecisionCommand(c.getId(), false, "Budget", DAF));

      assertThat(rejete.getStatut()).isEqualTo(StatutContrat.BROUILLON);
      assertThat(ports.evenements).contains(EventTypes.ENGAGEMENT_REJETE);
    }

    @Test
    void should_enregistrer_jalon() {
      ContratAchat c = contratGenere();
      execution.modifierClauses(
          new ModifierClausesCommand(
              c.getId(),
              c.getClauses(),
              20,
              List.of(new NouveauJalon("Expédition", LocalDate.of(2026, 10, 25)))));
      execution.signer(c.getId());
      UUID jalon = ports.contrats.get(c.getId()).getJalons().get(0).id();

      ContratAchat apres =
          execution.enregistrerJalon(
              c.getId(), jalon, LocalDate.ofInstant(horloge.instant(), ZoneOffset.UTC));

      assertThat(apres.getJalons().get(0).dateReelle()).isNotNull();
    }

    @Test
    void should_refuser_cloture_when_factures_a_payer() {
      ContratAchat c = contratEngage();
      execution.demarrerLivraison(c.getId());
      LocalDate aujourdhui = LocalDate.ofInstant(horloge.instant(), ZoneOffset.UTC);
      facturation.recevoir(
          new RecevoirFactureCommand(
              c.getId(),
              "FA-1",
              new BigDecimal("1000"),
              BigDecimal.ZERO,
              aujourdhui,
              aujourdhui,
              RA));
      execution.confirmerLivraison(
          new ConfirmerLivraisonCommand(c.getId(), aujourdhui, "BL-1", null));

      assertThatThrownBy(() -> execution.cloturer(c.getId()))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.FACTURES_EN_COURS));
    }

    @Test
    void should_alerter_sur_retard_de_livraison() {
      ContratAchat c = contratEngage();
      horloge.regler(horloge.instant().plus(Duration.ofDays(31)));

      assertThat(surveillance.signalerRetards()).isEqualTo(1);
      assertThat(ports.contrats.get(c.getId()).getPenalitesRetard()).isPositive();
      assertThat(ports.evenements).contains(EventTypes.ALERTE_RETARD_LIVRAISON);
      assertThat(surveillance.signalerRetards()).isZero();
    }
  }

  @Nested
  class Factures {

    private Facture recevoir(ContratAchat c, String numero, String ht) {
      LocalDate jour = LocalDate.ofInstant(horloge.instant(), ZoneOffset.UTC);
      return facturation.recevoir(
          new RecevoirFactureCommand(
              c.getId(), numero, new BigDecimal(ht), BigDecimal.ZERO, jour, jour.plusDays(30), RA));
    }

    @Test
    void should_traiter_le_cycle_d_une_facture() {
      ContratAchat c = contratEngage();
      Facture f = recevoir(c, " FA-SE-001 ", "50000000");

      assertThat(f.getNumero()).isEqualTo("FA-SE-001");
      assertThat(facturation.mandater(f.getId(), "MDT-1").getStatut())
          .isEqualTo(StatutFacture.MANDATEE);
      assertThat(facturation.payer(f.getId()).getStatut()).isEqualTo(StatutFacture.PAYEE);
      Facture autre = recevoir(c, "FA-SE-002", "10000000");
      assertThat(facturation.rejeter(autre.getId(), "Erreur").getStatut())
          .isEqualTo(StatutFacture.REJETEE);
      assertThat(ports.evenements)
          .containsSubsequence(
              EventTypes.FACTURE_RECUE,
              EventTypes.FACTURE_MANDATEE,
              EventTypes.FACTURE_PAYEE,
              EventTypes.FACTURE_REJETEE);
    }

    @Test
    void should_refuser_doublon_et_depassement() {
      ContratAchat c = contratEngage();
      recevoir(c, "FA-1", "100000000");

      assertThatThrownBy(() -> recevoir(c, "FA-1", "1"))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.FACTURE_EXISTANTE));
      assertThatThrownBy(() -> recevoir(c, "FA-2", "30000000"))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.DEPASSEMENT_MONTANT_CONTRAT));
      assertThatThrownBy(() -> facturation.payer(UUID.randomUUID()))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_calculer_synthese_et_filtrer_les_retards() {
      ContratAchat c = contratEngage();
      recevoir(c, "FA-1", "10000000");
      horloge.regler(horloge.instant().plus(Duration.ofDays(31)));

      SyntheseFinance s = finance.synthese();

      assertThat(s.montantEngage()).isEqualByComparingTo("120000000");
      assertThat(s.facturesEnRetard()).isEqualTo(1);
      assertThat(
              finance
                  .factures(new FactureRepository.Filtre(null, null, null, true), PAGE)
                  .content())
          .hasSize(1);
      assertThat(
              consultationContrats
                  .rechercher(new ContratRepository.Filtre(null, F1.id(), null), PAGE)
                  .content())
          .hasSize(1);
    }
  }

  @Nested
  class Portail {

    @Test
    void should_refuser_compte_sans_fournisseur() {
      assertThatThrownBy(() -> portail.appelsOffresOuverts(PORTAIL_SANS_FOURNISSEUR, PAGE))
          .isInstanceOf(ForbiddenOperationException.class)
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.COMPTE_NON_RATTACHE));
    }

    @Test
    void should_masquer_demande_de_prix_non_ouverte_au_fournisseur() {
      projections.fournisseurObserve(
          new FournisseurObserveCommand(F1.id(), F1.raisonSociale(), StatutFournisseur.ACTIF, T0));
      AppelOffre restreint =
          publication.publier(commande(null, TypeAppelOffre.DEMANDE_PRIX, List.of(F2.id())));

      assertThat(portail.appelsOffresOuverts(PORTAIL_F1, PAGE).content()).isEmpty();
      assertThatThrownBy(
              () ->
                  portail.deposerOffre(
                      PORTAIL_F1, restreint.getId(), BigDecimal.TEN, 1, null, null))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_lister_marches_et_factures_du_fournisseur_et_accepter_sa_facture() {
      ContratAchat c = contratEngage();
      LocalDate jour = LocalDate.ofInstant(horloge.instant(), ZoneOffset.UTC);

      Facture f =
          portail.deposerFacture(
              new RecevoirFactureCommand(
                  c.getId(),
                  "FA-P-1",
                  new BigDecimal("1000"),
                  BigDecimal.ZERO,
                  jour,
                  jour,
                  PORTAIL_F1));

      assertThat(f.getFournisseurId()).isEqualTo(F1.id());
      assertThat(portail.attributions(PORTAIL_F1, PAGE).content()).hasSize(1);
      assertThat(portail.factures(PORTAIL_F1, PAGE).content()).hasSize(1);
      CurrentUser autre =
          new CurrentUser(
              UUID.randomUUID(), "X", null, Set.of(Roles.FOURNISSEUR), Set.of(), F2.id());
      assertThatThrownBy(
              () ->
                  portail.deposerFacture(
                      new RecevoirFactureCommand(
                          c.getId(), "FA-P-2", BigDecimal.TEN, BigDecimal.ZERO, jour, jour, autre)))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Test
  void should_conserver_l_ao_rattache_lors_d_un_rejeu_de_besoin() {
    recevoirBesoin();
    AppelOffre ao = publication.publier(commande(BESOIN, TypeAppelOffre.APPEL_OFFRES, List.of()));

    recevoirBesoin();

    BesoinAAcheter b = ports.besoins.get(BESOIN);
    assertThat(b.appelOffreId()).isEqualTo(ao.getId());
    assertThat(Instant.EPOCH).isBefore(b.dateReception());
  }
}
