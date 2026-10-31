package sn.cse.achats.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sn.cse.achats.Donnees.BLACKLISTE;
import static sn.cse.achats.Donnees.DG;
import static sn.cse.achats.Donnees.EN_ATTENTE;
import static sn.cse.achats.Donnees.F1;
import static sn.cse.achats.Donnees.F2;
import static sn.cse.achats.Donnees.F3;
import static sn.cse.achats.Donnees.LIMITE;
import static sn.cse.achats.Donnees.RA;
import static sn.cse.achats.Donnees.RM;
import static sn.cse.achats.Donnees.SEUIL;
import static sn.cse.achats.Donnees.SUSPENDU;
import static sn.cse.achats.Donnees.T0;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sn.cse.achats.Donnees;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.NouvelAppelOffre;
import sn.cse.achats.domain.model.Offre;
import sn.cse.achats.domain.model.ResultatDecision;
import sn.cse.achats.domain.model.StatutAO;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.achats.domain.model.TypeAppelOffre;
import sn.cse.achats.domain.model.TypeProcesVerbal;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.CseException;
import sn.cse.common.exception.ResourceNotFoundException;

class AppelOffreTest {

  private static BigDecimal m(String v) {
    return new BigDecimal(v);
  }

  private static String code(Throwable t) {
    return ((CseException) t).getCode();
  }

  @Nested
  class Publication {

    @Test
    void should_publier_ao_avec_invites_actifs_et_origine() {
      AppelOffre ao = Donnees.aoPublie();

      assertThat(ao.getStatut()).isEqualTo(StatutAO.PUBLIE);
      assertThat(ao.getReference()).isEqualTo("AO-2026-001");
      assertThat(ao.getInvites())
          .extracting(i -> i.raisonSociale())
          .containsExactly("Sahel Engins SA", "Teranga Équipements BTP");
      assertThat(ao.getOrigine()).isPresent();
      assertThat(ao.getDatePublication()).isEqualTo(T0);
      assertThat(ao.getVersion()).isZero();
      assertThat(ao.getCreePar()).isEqualTo(RA);
    }

    @Test
    void should_refuser_invitation_when_fournisseur_suspendu_R14() {
      NouvelAppelOffre donnees =
          Donnees.nouvelAo(TypeAppelOffre.DEMANDE_PRIX, List.of(F1, SUSPENDU));

      assertThatThrownBy(() -> AppelOffre.publier(donnees, "AO-2026-002", T0))
          .isInstanceOf(BusinessRuleException.class)
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.FOURNISSEUR_NON_ELIGIBLE))
          .hasMessageContaining("R14");
    }

    @Test
    void should_refuser_invitation_when_fournisseur_blackliste_ou_non_valide_R14() {
      assertThatThrownBy(
              () ->
                  AppelOffre.publier(
                      Donnees.nouvelAo(TypeAppelOffre.APPEL_OFFRES, List.of(BLACKLISTE)), "R", T0))
          .isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(
              () ->
                  AppelOffre.publier(
                      Donnees.nouvelAo(TypeAppelOffre.APPEL_OFFRES, List.of(EN_ATTENTE)), "R", T0))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void should_refuser_demande_prix_sans_invite() {
      assertThatThrownBy(
              () ->
                  AppelOffre.publier(
                      Donnees.nouvelAo(TypeAppelOffre.DEMANDE_PRIX, List.of()), "R", T0))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.INVITES_OBLIGATOIRES));
    }

    @Test
    void should_refuser_date_limite_passee() {
      assertThatThrownBy(
              () ->
                  AppelOffre.publier(
                      Donnees.nouvelAo(TypeAppelOffre.APPEL_OFFRES, List.of()), "R", LIMITE))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.DATE_LIMITE_INVALIDE));
    }

    @Test
    void should_ne_pas_reserver_de_reference_when_donnees_invalides() {
      AtomicInteger appels = new AtomicInteger();
      NouvelAppelOffre invalide = Donnees.nouvelAo(TypeAppelOffre.DEMANDE_PRIX, List.of());

      assertThatThrownBy(
              () ->
                  AppelOffre.publier(
                      invalide,
                      () -> {
                        appels.incrementAndGet();
                        return "AO-2026-009";
                      },
                      T0))
          .isInstanceOf(BusinessRuleException.class);
      assertThat(appels).hasValue(0);
    }

    @Test
    void should_dedoublonner_les_invites_et_refuser_objet_vide() {
      AppelOffre ao =
          AppelOffre.publier(
              Donnees.nouvelAo(TypeAppelOffre.APPEL_OFFRES, List.of(F1, F1)), "R", T0);
      assertThat(ao.getInvites()).hasSize(1);

      NouvelAppelOffre base = Donnees.nouvelAo(TypeAppelOffre.APPEL_OFFRES, List.of());
      NouvelAppelOffre sansObjet =
          new NouvelAppelOffre(
              " ",
              null,
              base.type(),
              null,
              base.categorie(),
              1,
              null,
              null,
              base.dateLimite(),
              base.ponderation(),
              List.of(),
              RA);
      assertThatThrownBy(() -> AppelOffre.publier(sansObjet, "R", T0))
          .isInstanceOf(BusinessRuleException.class);
      NouvelAppelOffre quantiteNulle =
          new NouvelAppelOffre(
              "Objet",
              null,
              base.type(),
              null,
              base.categorie(),
              0,
              null,
              null,
              base.dateLimite(),
              base.ponderation(),
              List.of(),
              RA);
      assertThatThrownBy(() -> AppelOffre.publier(quantiteNulle, "R", T0))
          .isInstanceOf(BusinessRuleException.class);
      NouvelAppelOffre budgetNegatif =
          new NouvelAppelOffre(
              "Objet",
              null,
              base.type(),
              null,
              base.categorie(),
              1,
              null,
              m("-1"),
              base.dateLimite(),
              base.ponderation(),
              List.of(),
              RA);
      assertThatThrownBy(() -> AppelOffre.publier(budgetNegatif, "R", T0))
          .isInstanceOf(BusinessRuleException.class);
    }
  }

  @Nested
  class DepotOffres {

    @Test
    void should_accepter_offre_jusqu_a_la_date_limite_incluse_R02() {
      AppelOffre ao = Donnees.aoPublie();

      Offre offre = ao.deposerOffre(F1, m("298000000"), 60, 24, "Komatsu", LIMITE);

      assertThat(ao.getOffres()).containsExactly(offre);
      assertThat(offre.getCodeAnonyme()).matches("OFR-[0-9A-F]{6}");
      assertThat(offre.getFournisseurNom()).isEqualTo("Sahel Engins SA");
    }

    @Test
    void should_refuser_offre_when_date_limite_depassee_R02() {
      AppelOffre ao = Donnees.aoPublie();

      assertThatThrownBy(
              () -> ao.deposerOffre(F1, m("298000000"), 60, 24, null, LIMITE.plusSeconds(1)))
          .isInstanceOf(BusinessRuleException.class)
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.DATE_LIMITE_DEPASSEE))
          .hasMessageContaining("R02");
      assertThat(ao.getOffres()).isEmpty();
    }

    @Test
    void should_refuser_offre_when_fournisseur_suspendu_R14() {
      AppelOffre ao = Donnees.aoPublie();

      assertThatThrownBy(() -> ao.deposerOffre(SUSPENDU, m("100"), 10, null, null, T0))
          .isInstanceOf(BusinessRuleException.class)
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.FOURNISSEUR_NON_ELIGIBLE));
    }

    @Test
    void should_refuser_offre_when_fournisseur_blackliste_R14() {
      AppelOffre ao = Donnees.aoPublie();

      assertThatThrownBy(() -> ao.deposerOffre(BLACKLISTE, m("100"), 10, null, null, T0))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void should_refuser_fournisseur_non_invite_sur_demande_de_prix() {
      AppelOffre ao =
          AppelOffre.publier(Donnees.nouvelAo(TypeAppelOffre.DEMANDE_PRIX, List.of(F1)), "R", T0);

      assertThatThrownBy(() -> ao.deposerOffre(F2, m("100"), 10, null, null, T0))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.FOURNISSEUR_NON_INVITE));
      assertThat(ao.estOuvertAuFournisseur(F1.id())).isTrue();
      assertThat(ao.estOuvertAuFournisseur(F2.id())).isFalse();
    }

    @Test
    void should_accepter_fournisseur_non_invite_sur_appel_ouvert() {
      AppelOffre ao = Donnees.aoPublie();

      ao.deposerOffre(F3, m("100"), 10, null, null, T0);

      assertThat(ao.estOuvertAuFournisseur(F3.id())).isTrue();
      assertThat(ao.offreDu(F3.id())).isPresent();
    }

    @Test
    void should_refuser_deuxieme_offre_du_meme_fournisseur() {
      AppelOffre ao = Donnees.aoPublie();
      ao.deposerOffre(F1, m("100"), 10, null, null, T0);

      assertThatThrownBy(() -> ao.deposerOffre(F1, m("90"), 10, null, null, T0))
          .isInstanceOf(ConflictException.class)
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.OFFRE_DEJA_DEPOSEE));
    }

    @Test
    void should_refuser_offre_invalide() {
      AppelOffre ao = Donnees.aoPublie();

      assertThatThrownBy(() -> ao.deposerOffre(F1, m("0"), 10, null, null, T0))
          .isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> ao.deposerOffre(F1, m("10"), 0, null, null, T0))
          .isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> ao.deposerOffre(F1, m("10"), 5, -1, null, T0))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void should_refuser_offre_when_ao_cloture() {
      AppelOffre ao = Donnees.aoCloture("100", "110");

      assertThatThrownBy(() -> ao.deposerOffre(F3, m("100"), 10, null, null, T0))
          .isInstanceOf(ConflictException.class)
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.AO_NON_PUBLIE));
    }
  }

  @Nested
  class AnonymatEtCloture {

    @Test
    void should_masquer_identite_when_ao_publie_R10() {
      AppelOffre ao = Donnees.aoPublie();
      ao.deposerOffre(F1, m("100"), 10, null, null, T0);

      assertThat(ao.estAnonyme()).isTrue();
      assertThatThrownBy(ao::exigerEvaluable)
          .isInstanceOf(ConflictException.class)
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.AO_NON_CLOTURE));
    }

    @Test
    void should_lever_anonymat_when_ao_cloture_R10() {
      AppelOffre ao = Donnees.aoCloture("100", "110");

      assertThat(ao.estAnonyme()).isFalse();
      assertThat(ao.getStatut()).isEqualTo(StatutAO.CLOTURE);
      assertThat(ao.getDateCloture()).isEqualTo(LIMITE);
    }

    @Test
    void should_detecter_ao_echu_et_refuser_double_cloture() {
      AppelOffre ao = Donnees.aoPublie();

      assertThat(ao.estEchu(LIMITE.minusSeconds(1))).isFalse();
      assertThat(ao.estEchu(LIMITE)).isTrue();
      ao.cloturer(LIMITE);
      assertThat(ao.estEchu(LIMITE.plus(Duration.ofDays(1)))).isFalse();
      assertThatThrownBy(() -> ao.cloturer(LIMITE)).isInstanceOf(ConflictException.class);
    }
  }

  @Nested
  class Evaluation {

    @Test
    void should_enregistrer_notes_saisies_when_ao_cloture() {
      AppelOffre ao = Donnees.aoCloture("100", "110");
      UUID id = ao.offreDu(F1.id()).orElseThrow().getId();

      Offre offre = ao.evaluerOffre(id, Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 85));

      assertThat(offre.noteSaisie(CritereEvaluation.QUALITE_TECHNIQUE)).contains(85);
      assertThat(offre.getNotesSaisies()).hasSize(1);
    }

    @Test
    void should_refuser_evaluation_when_ao_publie() {
      AppelOffre ao = Donnees.aoPublie();
      Offre offre = ao.deposerOffre(F1, m("100"), 10, null, null, T0);

      assertThatThrownBy(() -> ao.evaluerOffre(offre.getId(), Map.of()))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.AO_NON_CLOTURE));
    }

    @Test
    void should_refuser_notes_invalides() {
      AppelOffre ao = Donnees.aoCloture("100", "110");
      UUID id = ao.offreDu(F1.id()).orElseThrow().getId();

      assertThatThrownBy(() -> ao.evaluerOffre(id, Map.of(CritereEvaluation.PRIX, 50)))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.CRITERE_CALCULE));
      assertThatThrownBy(() -> ao.evaluerOffre(id, Map.of(CritereEvaluation.SAV, 50)))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.CRITERE_NON_RETENU));
      assertThatThrownBy(
              () -> ao.evaluerOffre(id, Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 101)))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.NOTE_INVALIDE));
      assertThatThrownBy(() -> ao.evaluerOffre(UUID.randomUUID(), Map.of()))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_figer_notes_when_attribution_en_cours_R11() {
      AppelOffre ao = Donnees.aoCloture("100", "110");
      UUID id = ao.offreDu(F1.id()).orElseThrow().getId();
      ao.proposerAttribution(id, "Justifiée", RA, LIMITE, SEUIL);

      assertThatThrownBy(() -> ao.evaluerOffre(id, Map.of()))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.ATTRIBUTION_EN_COURS));
    }
  }

  @Nested
  class Attribution {

    @Test
    void should_exiger_seulement_co_validation_rm_when_montant_sous_seuil_R11() {
      AppelOffre ao = Donnees.aoCloture("150000000", "160000000");
      UUID id = ao.offreDu(F1.id()).orElseThrow().getId();

      var attribution = ao.proposerAttribution(id, "Meilleur score", RA, LIMITE, SEUIL);

      assertThat(attribution.approbationDgRequise()).isFalse();
      assertThat(attribution.coValidationMateriel().statut())
          .isEqualTo(StatutApprobation.EN_ATTENTE);
      assertThat(ao.coValiderAttribution(true, "OK", RM, LIMITE.plusSeconds(10)))
          .isEqualTo(ResultatDecision.MARCHE_ATTRIBUE);
      assertThat(ao.getStatut()).isEqualTo(StatutAO.ATTRIBUE);
      assertThat(ao.getOffreRetenueId()).isEqualTo(id);
      assertThat(ao.getMontantAttribue()).isEqualByComparingTo("150000000");
      assertThat(ao.offreRetenue()).get().extracting(Offre::isRetenue).isEqualTo(true);
      assertThat(ao.getAttribution().orElseThrow().coValidationMateriel().parNom())
          .isEqualTo("Ibrahima Fall");
    }

    @Test
    void should_exiger_approbation_dg_when_montant_depasse_seuil_R11() {
      AppelOffre ao = Donnees.aoCloture("298000000", "312000000");
      UUID id = ao.offreDu(F1.id()).orElseThrow().getId();
      ao.proposerAttribution(id, "Meilleur score", RA, LIMITE, SEUIL);

      assertThat(ao.getAttribution().orElseThrow().approbationDgRequise()).isTrue();
      assertThat(ao.coValiderAttribution(true, null, RM, LIMITE))
          .isEqualTo(ResultatDecision.EN_ATTENTE_AUTRES_APPROBATIONS);
      assertThat(ao.getStatut()).isEqualTo(StatutAO.CLOTURE);
      assertThat(ao.approuverAttribution(true, "Approuvé", DG, LIMITE))
          .isEqualTo(ResultatDecision.MARCHE_ATTRIBUE);
      assertThat(ao.getStatut()).isEqualTo(StatutAO.ATTRIBUE);
    }

    @Test
    void should_accepter_approbation_dg_avant_co_validation_R11() {
      AppelOffre ao = Donnees.aoCloture("298000000", "312000000");
      ao.proposerAttribution(ao.offreDu(F2.id()).orElseThrow().getId(), "Délai", RA, LIMITE, SEUIL);

      assertThat(ao.approuverAttribution(true, null, DG, LIMITE))
          .isEqualTo(ResultatDecision.EN_ATTENTE_AUTRES_APPROBATIONS);
      assertThat(ao.coValiderAttribution(true, null, RM, LIMITE))
          .isEqualTo(ResultatDecision.MARCHE_ATTRIBUE);
    }

    @Test
    void should_supprimer_proposition_when_refus_rm_R11() {
      AppelOffre ao = Donnees.aoCloture("298000000", "312000000");
      UUID id = ao.offreDu(F1.id()).orElseThrow().getId();
      ao.proposerAttribution(id, "Meilleur score", RA, LIMITE, SEUIL);

      assertThat(ao.coValiderAttribution(false, "Inadapté", RM, LIMITE))
          .isEqualTo(ResultatDecision.ATTRIBUTION_REFUSEE);
      assertThat(ao.getAttribution()).isEmpty();
      assertThat(ao.getStatut()).isEqualTo(StatutAO.CLOTURE);
      // Une nouvelle proposition est possible après un refus.
      ao.proposerAttribution(
          ao.offreDu(F2.id()).orElseThrow().getId(), "Second", RA, LIMITE, SEUIL);
      assertThat(ao.getAttribution()).isPresent();
    }

    @Test
    void should_supprimer_proposition_when_refus_dg_R11() {
      AppelOffre ao = Donnees.aoCloture("298000000", "312000000");
      ao.proposerAttribution(ao.offreDu(F1.id()).orElseThrow().getId(), "X", RA, LIMITE, SEUIL);
      ao.coValiderAttribution(true, null, RM, LIMITE);

      assertThat(ao.approuverAttribution(false, "Budget", DG, LIMITE))
          .isEqualTo(ResultatDecision.ATTRIBUTION_REFUSEE);
      assertThat(ao.getAttribution()).isEmpty();
    }

    @Test
    void should_refuser_approbation_dg_when_non_requise_R11() {
      AppelOffre ao = Donnees.aoCloture("100", "110");
      ao.proposerAttribution(ao.offreDu(F1.id()).orElseThrow().getId(), "X", RA, LIMITE, SEUIL);

      assertThatThrownBy(() -> ao.approuverAttribution(true, null, DG, LIMITE))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.APPROBATION_DG_NON_REQUISE));
    }

    @Test
    void should_refuser_double_decision_et_decision_sans_proposition_R11() {
      AppelOffre ao = Donnees.aoCloture("298000000", "312000000");
      assertThatThrownBy(() -> ao.coValiderAttribution(true, null, RM, LIMITE))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.AUCUNE_ATTRIBUTION));
      ao.proposerAttribution(ao.offreDu(F1.id()).orElseThrow().getId(), "X", RA, LIMITE, SEUIL);
      ao.coValiderAttribution(true, null, RM, LIMITE);

      assertThatThrownBy(() -> ao.coValiderAttribution(true, null, RM, LIMITE))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.DECISION_DEJA_PRISE));
    }

    @Test
    void should_refuser_proposition_invalide_R11() {
      AppelOffre publie = Donnees.aoPublie();
      assertThatThrownBy(
              () -> publie.proposerAttribution(UUID.randomUUID(), "X", RA, LIMITE, SEUIL))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.AO_NON_CLOTURE));

      AppelOffre ao = Donnees.aoCloture("100", "110");
      UUID id = ao.offreDu(F1.id()).orElseThrow().getId();
      assertThatThrownBy(() -> ao.proposerAttribution(id, " ", RA, LIMITE, SEUIL))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.JUSTIFICATION_OBLIGATOIRE));
      ao.proposerAttribution(id, "X", RA, LIMITE, SEUIL);
      assertThatThrownBy(() -> ao.proposerAttribution(id, "Y", RA, LIMITE, SEUIL))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.ATTRIBUTION_EN_COURS));

      AppelOffre attribue = Donnees.aoAttribue();
      assertThatThrownBy(() -> attribue.proposerAttribution(id, "Z", RA, LIMITE, SEUIL))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.AO_DEJA_ATTRIBUE));
      assertThatThrownBy(() -> attribue.coValiderAttribution(true, null, RM, LIMITE))
          .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.AUCUNE_ATTRIBUTION));
    }
  }

  @Test
  void should_rendre_proces_verbaux_disponibles_selon_statut() {
    AppelOffre publie = Donnees.aoPublie();
    assertThatThrownBy(() -> publie.exigerProcesVerbalDisponible(TypeProcesVerbal.OUVERTURE))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.PV_INDISPONIBLE));

    AppelOffre cloture = Donnees.aoCloture("100", "110");
    cloture.exigerProcesVerbalDisponible(TypeProcesVerbal.OUVERTURE);
    assertThatThrownBy(() -> cloture.exigerProcesVerbalDisponible(TypeProcesVerbal.EVALUATION))
        .isInstanceOf(ConflictException.class);

    AppelOffre attribue = Donnees.aoAttribue();
    attribue.exigerProcesVerbalDisponible(TypeProcesVerbal.OUVERTURE);
    attribue.exigerProcesVerbalDisponible(TypeProcesVerbal.EVALUATION);
  }

  @Test
  void should_refuser_transition_ao_hors_table() {
    assertThat(StatutAO.PUBLIE.peutPasserA(StatutAO.ATTRIBUE)).isFalse();
    assertThatThrownBy(() -> StatutAO.ATTRIBUE.exigerTransition(StatutAO.PUBLIE))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.TRANSITION_INTERDITE));
  }
}
