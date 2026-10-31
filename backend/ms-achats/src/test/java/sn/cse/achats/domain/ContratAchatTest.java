package sn.cse.achats.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sn.cse.achats.Donnees.DAF;
import static sn.cse.achats.Donnees.PARAMETRES;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sn.cse.achats.Donnees;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.ClausesContrat;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.NouveauJalon;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.achats.domain.model.StatutContrat;
import sn.cse.achats.domain.model.StatutJalon;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.CseException;
import sn.cse.common.exception.ResourceNotFoundException;

class ContratAchatTest {

  private static final Instant SIGNATURE = Instant.parse("2026-10-15T10:00:00Z");
  private static final LocalDate JOUR_SIGNATURE = LocalDate.of(2026, 10, 15);

  private static String code(Throwable t) {
    return ((CseException) t).getCode();
  }

  private static ContratAchat signe() {
    ContratAchat c = Donnees.contratBrouillon();
    c.modifierClauses(
        new ClausesContrat(24, null, "Franco chantier", new BigDecimal("0.1")),
        30,
        List.of(new NouveauJalon("Expédition", JOUR_SIGNATURE.plusDays(20))));
    c.signer(SIGNATURE);
    return c;
  }

  private static ContratAchat enLivraison() {
    ContratAchat c = signe();
    c.deciderEngagement(true, "Crédits disponibles", DAF, SIGNATURE.plusSeconds(3600));
    c.demarrerLivraison();
    return c;
  }

  @Test
  void should_generer_contrat_brouillon_depuis_ao_attribue_R03() {
    ContratAchat c = Donnees.contratBrouillon();

    assertThat(c.getStatut()).isEqualTo(StatutContrat.BROUILLON);
    assertThat(c.getNumero()).isEqualTo("CTR-2026-001");
    assertThat(c.getFournisseurNom()).isEqualTo("Sahel Engins SA");
    assertThat(c.getMontant()).isEqualByComparingTo("120000000");
    assertThat(c.getDesignation()).isEqualTo("Pelle hydraulique 20 t");
    assertThat(c.getChantierNom()).isEqualTo("Extension autoroute Dakar-AIBD");
    assertThat(c.getDelaiLivraisonJours()).isEqualTo(60);
    assertThat(c.getClauses().garantieMois()).isEqualTo(24);
    assertThat(c.getClauses().penaliteRetardPctParJour()).isEqualByComparingTo("0.1");
    assertThat(c.getEngagement()).isEmpty();
  }

  @Test
  void should_refuser_contrat_when_ao_non_attribue_R03() {
    AppelOffre cloture = Donnees.aoCloture("100", "110");
    AtomicInteger reservations = new AtomicInteger();

    assertThatThrownBy(
            () ->
                ContratAchat.generer(
                    cloture,
                    () -> {
                      reservations.incrementAndGet();
                      return "CTR-2026-002";
                    },
                    PARAMETRES))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.AO_NON_ATTRIBUE))
        .hasMessageContaining("R03");
    assertThat(reservations).hasValue(0);
  }

  @Test
  void should_utiliser_objet_when_ao_sans_besoin_d_origine() {
    AppelOffre ao = Donnees.aoAttribue();
    AppelOffre sansOrigine =
        new AppelOffre(
            ao.getId(),
            ao.getReference(),
            ao.getObjet(),
            null,
            ao.getType(),
            null,
            ao.getCategorie(),
            ao.getQuantite(),
            null,
            null,
            ao.getDatePublication(),
            ao.getDateLimite(),
            ao.getDateCloture(),
            ao.getStatut(),
            ao.getPonderation(),
            ao.getInvites(),
            ao.getOffres(),
            ao.getAttribution().orElse(null),
            ao.getOffreRetenueId(),
            ao.getMontantAttribue(),
            ao.getCreePar(),
            3);

    ContratAchat c = ContratAchat.generer(sansOrigine, "CTR-2026-003", PARAMETRES);

    assertThat(c.getDesignation()).isEqualTo(ao.getObjet());
    assertThat(c.getBesoinId()).isNull();
  }

  @Test
  void should_signer_et_demander_engagement_daf() {
    ContratAchat c = signe();

    assertThat(c.getStatut()).isEqualTo(StatutContrat.SIGNE);
    assertThat(c.getDateSignature()).isEqualTo(JOUR_SIGNATURE);
    assertThat(c.getDateLivraisonPrevue()).isEqualTo(JOUR_SIGNATURE.plusDays(30));
    assertThat(c.getEngagement())
        .get()
        .satisfies(
            e -> {
              assertThat(e.statut()).isEqualTo(StatutApprobation.EN_ATTENTE);
              assertThat(e.montant()).isEqualByComparingTo("120000000");
              assertThat(e.dateDemande()).isEqualTo(SIGNATURE);
            });
    assertThat(c.getJalons()).hasSize(1);
  }

  @Test
  void should_refuser_modification_clauses_when_contrat_signe() {
    ContratAchat c = signe();

    assertThatThrownBy(() -> c.modifierClauses(c.getClauses(), 10, List.of()))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.CONTRAT_NON_MODIFIABLE));
  }

  @Test
  void should_refuser_clauses_invalides() {
    ContratAchat c = Donnees.contratBrouillon();

    assertThatThrownBy(() -> c.modifierClauses(c.getClauses(), 0, List.of()))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () -> c.modifierClauses(c.getClauses(), 10, List.of(new NouveauJalon("X", null))))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                c.modifierClauses(
                    c.getClauses(), 10, List.of(new NouveauJalon(" ", JOUR_SIGNATURE))))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_refuser_livraison_when_engagement_non_valide_R06() {
    ContratAchat c = signe();

    assertThatThrownBy(c::demarrerLivraison)
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.ENGAGEMENT_NON_VALIDE))
        .hasMessageContaining("R06");
    assertThatThrownBy(() -> Donnees.contratBrouillon().demarrerLivraison())
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.ENGAGEMENT_NON_VALIDE));
  }

  @Test
  void should_autoriser_livraison_when_engagement_valide_R06() {
    ContratAchat c = enLivraison();

    assertThat(c.getStatut()).isEqualTo(StatutContrat.EN_LIVRAISON);
    assertThat(c.getEngagement().orElseThrow().parNom()).isEqualTo("Ousmane Ba");
    assertThatThrownBy(c::demarrerLivraison)
        .isInstanceOf(ConflictException.class)
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.TRANSITION_INTERDITE));
  }

  @Test
  void should_renvoyer_en_brouillon_when_engagement_refuse() {
    ContratAchat c = signe();

    c.deciderEngagement(false, "Budget épuisé", DAF, SIGNATURE);

    assertThat(c.getStatut()).isEqualTo(StatutContrat.BROUILLON);
    assertThat(c.getEngagement().orElseThrow().statut()).isEqualTo(StatutApprobation.REFUSEE);
    assertThat(c.getDateSignature()).isNull();
    assertThatThrownBy(() -> c.deciderEngagement(true, null, DAF, SIGNATURE))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.ENGAGEMENT_NON_EN_ATTENTE));
    // Après renégociation, une nouvelle signature redemande un engagement.
    c.signer(SIGNATURE.plusSeconds(86_400));
    assertThat(c.getEngagement().orElseThrow().statut()).isEqualTo(StatutApprobation.EN_ATTENTE);
  }

  @Test
  void should_enregistrer_jalon_pendant_execution() {
    ContratAchat c = signe();
    UUID jalon = c.getJalons().get(0).id();
    LocalDate aujourdhui = JOUR_SIGNATURE.plusDays(25);

    c.enregistrerJalon(jalon, JOUR_SIGNATURE.plusDays(22), aujourdhui);

    assertThat(c.getJalons().get(0).statutAu(aujourdhui)).isEqualTo(StatutJalon.ATTEINT);
    assertThatThrownBy(() -> c.enregistrerJalon(jalon, aujourdhui.plusDays(1), aujourdhui))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.DATE_FUTURE));
    assertThatThrownBy(() -> c.enregistrerJalon(UUID.randomUUID(), aujourdhui, aujourdhui))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(
            () -> Donnees.contratBrouillon().enregistrerJalon(jalon, aujourdhui, aujourdhui))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.CONTRAT_NON_EN_EXECUTION));
  }

  @Test
  void should_confirmer_livraison_a_l_heure_sans_penalite() {
    ContratAchat c = enLivraison();
    LocalDate livraison = JOUR_SIGNATURE.plusDays(28);

    c.confirmerLivraison(livraison, "BL-001", " RAS ", livraison, BigDecimal.TEN);

    assertThat(c.getStatut()).isEqualTo(StatutContrat.LIVRE);
    assertThat(c.getPenalitesRetard()).isEqualByComparingTo("0");
    assertThat(c.getNumeroBonLivraison()).isEqualTo("BL-001");
    assertThat(c.getObservationsLivraison()).isEqualTo("RAS");
    c.cloturer();
    assertThat(c.getStatut()).isEqualTo(StatutContrat.CLOTURE);
  }

  @Test
  void should_calculer_penalites_de_retard_a_la_livraison() {
    ContratAchat c = enLivraison();
    // 12 jours de retard × 0,1 % × 120 000 000 = 1 440 000 FCFA
    LocalDate livraison = JOUR_SIGNATURE.plusDays(42);

    c.confirmerLivraison(livraison, "BL-002", null, livraison, BigDecimal.TEN);

    assertThat(c.joursRetard(livraison)).isEqualTo(12);
    assertThat(c.getPenalitesRetard()).isEqualByComparingTo("1440000");
  }

  @Test
  void should_plafonner_penalites() {
    ContratAchat c = enLivraison();

    // 200 jours × 0,1 % = 20 % > plafond de 10 % → 12 000 000
    assertThat(c.penalitesAu(JOUR_SIGNATURE.plusDays(230), BigDecimal.TEN))
        .isEqualByComparingTo("12000000");
    assertThat(c.joursRetard(null)).isZero();
    assertThat(Donnees.contratBrouillon().joursRetard(JOUR_SIGNATURE)).isZero();
  }

  @Test
  void should_refuser_confirmation_livraison_invalide() {
    ContratAchat c = enLivraison();
    LocalDate aujourdhui = JOUR_SIGNATURE.plusDays(10);

    assertThatThrownBy(
            () ->
                c.confirmerLivraison(
                    aujourdhui.plusDays(1), "BL", null, aujourdhui, BigDecimal.TEN))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.DATE_FUTURE));
    assertThatThrownBy(
            () ->
                c.confirmerLivraison(
                    JOUR_SIGNATURE.minusDays(1), "BL", null, aujourdhui, BigDecimal.TEN))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.DATE_LIVRAISON_INVALIDE));
    assertThatThrownBy(
            () -> c.confirmerLivraison(aujourdhui, " ", null, aujourdhui, BigDecimal.TEN))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () -> signe().confirmerLivraison(aujourdhui, "BL", null, aujourdhui, BigDecimal.TEN))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void should_signaler_retard_une_fois_par_intervalle() {
    ContratAchat c = enLivraison();
    LocalDate echeance = c.getDateLivraisonPrevue();

    assertThat(c.signalerRetard(echeance, PARAMETRES)).isFalse();
    assertThat(c.estEnRetard(echeance.plusDays(1))).isTrue();
    assertThat(c.signalerRetard(echeance.plusDays(1), PARAMETRES)).isTrue();
    assertThat(c.getPenalitesRetard()).isEqualByComparingTo("120000");
    assertThat(c.signalerRetard(echeance.plusDays(3), PARAMETRES)).isFalse();
    assertThat(c.getPenalitesRetard()).isEqualByComparingTo("360000");
    assertThat(c.signalerRetard(echeance.plusDays(8), PARAMETRES)).isTrue();
    assertThat(c.getDateDerniereAlerteRetard()).isEqualTo(echeance.plusDays(8));
  }

  @Test
  void should_estimer_tarif_journalier_unitaire() {
    // 120 000 000 / 2 unités × 0,25 % = 150 000 FCFA par jour
    assertThat(Donnees.contratBrouillon().tarifJournalierEstime(new BigDecimal("0.0025")))
        .isEqualByComparingTo("150000");
  }

  @ParameterizedTest(name = "{0} → {1} : {2}")
  @CsvSource({
    "BROUILLON, SIGNE, true",
    "BROUILLON, EN_LIVRAISON, false",
    "SIGNE, ENGAGEMENT_VALIDE, true",
    "SIGNE, BROUILLON, true",
    "SIGNE, EN_LIVRAISON, false",
    "ENGAGEMENT_VALIDE, EN_LIVRAISON, true",
    "EN_LIVRAISON, LIVRE, true",
    "LIVRE, CLOTURE, true",
    "LIVRE, EN_LIVRAISON, false",
    "CLOTURE, BROUILLON, false"
  })
  void should_respecter_table_de_transitions(
      StatutContrat de, StatutContrat vers, boolean autorise) {
    assertThat(de.peutPasserA(vers)).isEqualTo(autorise);
    if (!autorise) {
      assertThatThrownBy(() -> de.exigerTransition(vers)).isInstanceOf(ConflictException.class);
    }
  }

  @Test
  void should_qualifier_statuts_facturables_et_en_execution() {
    assertThat(StatutContrat.SIGNE.estEnExecution()).isTrue();
    assertThat(StatutContrat.LIVRE.estEnExecution()).isFalse();
    assertThat(StatutContrat.SIGNE.estFacturable()).isFalse();
    assertThat(StatutContrat.LIVRE.estFacturable()).isTrue();
    assertThat(StatutContrat.CLOTURE.estFacturable()).isFalse();
  }
}
