package sn.cse.besoins.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sn.cse.besoins.Fixtures.CHANTIER_1;
import static sn.cse.besoins.Fixtures.CHANTIER_2;
import static sn.cse.besoins.Fixtures.CHEF;
import static sn.cse.besoins.Fixtures.MAINTENANT;
import static sn.cse.besoins.Fixtures.RA;
import static sn.cse.besoins.Fixtures.RM;
import static sn.cse.besoins.Fixtures.besoinEnAttente;
import static sn.cse.besoins.Fixtures.description;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sn.cse.besoins.domain.exception.BesoinNonModifiableException;
import sn.cse.besoins.domain.exception.TransitionInterditeException;
import sn.cse.besoins.domain.exception.ValidationNonAutoriseeException;
import sn.cse.besoins.domain.validation.ChaineValidation;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.ForbiddenOperationException;

class BesoinMaterielTest {

  private static final ChaineValidation CHAINE = ChaineValidation.parDefaut();
  private static final Instant PLUS_TARD = MAINTENANT.plus(Duration.ofHours(4));

  private static List<TypeEvenementBesoin> types(BesoinMateriel b) {
    return b.extraireEvenements().stream().map(EvenementBesoin::type).toList();
  }

  private static List<ActionHistorique> actions(BesoinMateriel b) {
    return b.historique().stream().map(HistoriqueBesoin::action).toList();
  }

  @Nested
  class Soumission {

    @Test
    void should_creer_un_besoin_en_attente_avec_historique_et_evenement() {
      BesoinMateriel b = besoinEnAttente(2);

      assertThat(b.statut()).isEqualTo(StatutBesoin.EN_ATTENTE);
      assertThat(b.demandeurId()).isEqualTo(CHEF.id());
      assertThat(b.demandeurNom()).isEqualTo("Mamadou Diop");
      assertThat(b.creeLe()).isEqualTo(MAINTENANT);
      assertThat(b.modifieLe()).isEqualTo(MAINTENANT);
      assertThat(b.version()).isNull();
      assertThat(b.orientationEnAttente()).isFalse();
      assertThat(b.historique())
          .singleElement()
          .satisfies(
              h -> {
                assertThat(h.action()).isEqualTo(ActionHistorique.SOUMISSION);
                assertThat(h.statut()).isEqualTo(StatutBesoin.EN_ATTENTE);
                assertThat(h.auteurId()).isEqualTo(CHEF.id());
              });
      assertThat(types(b)).containsExactly(TypeEvenementBesoin.SOUMIS);
      assertThat(b.extraireEvenements()).isEmpty();
    }

    @Test
    void should_refuser_une_date_de_besoin_passee() {
      assertThatThrownBy(
              () ->
                  BesoinMateriel.soumettre(
                      UUID.randomUUID(),
                      "BES-2026-0002",
                      CHANTIER_1,
                      "Chantier",
                      CHEF,
                      description(1, Priorite.NORMAL),
                      MAINTENANT.plus(Duration.ofDays(30))))
          .isInstanceOf(BusinessRuleException.class)
          .hasFieldOrPropertyWithValue("code", "DATE_BESOIN_PASSEE");
    }
  }

  @Nested
  class Modification {

    @Test
    void should_modifier_le_contenu_when_en_attente_et_demandeur_R05() {
      BesoinMateriel b = besoinEnAttente(1);
      b.extraireEvenements();

      b.modifier(
          null,
          CHANTIER_2,
          "Contournement de Thiès RN2",
          description(3, Priorite.URGENT),
          CHEF,
          PLUS_TARD);

      assertThat(b.description().quantite()).isEqualTo(3);
      assertThat(b.chantierId()).isEqualTo(CHANTIER_2);
      assertThat(b.chantierNom()).isEqualTo("Contournement de Thiès RN2");
      assertThat(b.modifieLe()).isEqualTo(PLUS_TARD);
      assertThat(actions(b))
          .containsExactly(ActionHistorique.SOUMISSION, ActionHistorique.MODIFICATION);
      assertThat(types(b)).containsExactly(TypeEvenementBesoin.MODIFIE);
    }

    @Test
    void should_refuser_modification_when_besoin_valide_R05() {
      BesoinMateriel b = besoinEnAttente(1);
      b.valider(CHAINE, RM, null, PLUS_TARD);

      assertThatThrownBy(
              () ->
                  b.modifier(
                      null, CHANTIER_1, "x", description(2, Priorite.NORMAL), CHEF, PLUS_TARD))
          .isInstanceOf(BesoinNonModifiableException.class)
          .hasFieldOrPropertyWithValue("code", "BESOIN_NON_MODIFIABLE");
    }

    @Test
    void should_refuser_modification_when_besoin_rejete_R05() {
      BesoinMateriel b = besoinEnAttente(1);
      b.rejeter(CHAINE, RM, "Doublon", PLUS_TARD);

      assertThatThrownBy(
              () ->
                  b.modifier(
                      null, CHANTIER_1, "x", description(2, Priorite.NORMAL), CHEF, PLUS_TARD))
          .isInstanceOf(BesoinNonModifiableException.class);
    }

    @Test
    void should_refuser_modification_par_un_autre_que_le_demandeur() {
      BesoinMateriel b = besoinEnAttente(1);
      assertThatThrownBy(
              () ->
                  b.modifier(null, CHANTIER_1, "x", description(2, Priorite.NORMAL), RM, PLUS_TARD))
          .isInstanceOf(ForbiddenOperationException.class)
          .hasFieldOrPropertyWithValue("code", "MODIFICATION_RESERVEE_DEMANDEUR");
      assertThatThrownBy(
              () ->
                  b.modifier(
                      null,
                      CHANTIER_1,
                      "x",
                      description(2, Priorite.NORMAL),
                      Acteur.systeme("batch"),
                      PLUS_TARD))
          .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void should_refuser_une_version_perimee() {
      BesoinMateriel b =
          BesoinMateriel.reconstituer(
              new EtatBesoin(
                  UUID.randomUUID(),
                  "BES-2026-0009",
                  CHANTIER_1,
                  "Chantier",
                  CHEF.id(),
                  CHEF.nom(),
                  description(1, Priorite.NORMAL),
                  StatutBesoin.EN_ATTENTE,
                  null,
                  null,
                  null,
                  null,
                  null,
                  false,
                  null,
                  3L,
                  MAINTENANT,
                  null));
      assertThatThrownBy(
              () ->
                  b.modifier(2L, CHANTIER_1, "x", description(2, Priorite.NORMAL), CHEF, PLUS_TARD))
          .isInstanceOf(ConflictException.class)
          .hasFieldOrPropertyWithValue("code", "CONFLIT_CONCURRENCE");
      b.verifierVersion(3L);
      assertThat(b.modifieLe()).isEqualTo(MAINTENANT);
      assertThat(b.historique()).isEmpty();
    }
  }

  @Nested
  class Validation {

    @Test
    void should_valider_et_mettre_en_attente_d_orientation_when_responsable_materiel_R01() {
      BesoinMateriel b = besoinEnAttente(1);
      b.extraireEvenements();

      b.valider(CHAINE, RM, "Conforme", PLUS_TARD);

      assertThat(b.statut()).isEqualTo(StatutBesoin.VALIDE);
      assertThat(b.orientationEnAttente()).isTrue();
      assertThat(b.validateurId()).isEqualTo(RM.id());
      assertThat(b.validateurNom()).isEqualTo(RM.nom());
      assertThat(b.dateValidation()).isEqualTo(PLUS_TARD);
      assertThat(b.aFranchiEtape(ChaineValidation.ETAPE_RESPONSABLE_MATERIEL)).isTrue();
      HistoriqueBesoin derniere = b.historique().get(1);
      assertThat(derniere.action()).isEqualTo(ActionHistorique.VALIDATION);
      assertThat(derniere.commentaire()).isEqualTo("Conforme");
      assertThat(derniere.etapeValidation()).isEqualTo("RESPONSABLE_MATERIEL");
      assertThat(types(b)).containsExactly(TypeEvenementBesoin.VALIDE);
    }

    @Test
    void should_refuser_la_validation_when_pas_responsable_materiel_R01() {
      BesoinMateriel b = besoinEnAttente(1);
      assertThatThrownBy(() -> b.valider(CHAINE, CHEF, null, PLUS_TARD))
          .isInstanceOf(ValidationNonAutoriseeException.class)
          .hasFieldOrPropertyWithValue("code", "VALIDATION_NON_AUTORISEE");
      assertThatThrownBy(() -> b.valider(CHAINE, RA, null, PLUS_TARD))
          .isInstanceOf(ValidationNonAutoriseeException.class);
      assertThat(b.statut()).isEqualTo(StatutBesoin.EN_ATTENTE);
    }

    @Test
    void should_refuser_une_seconde_validation() {
      BesoinMateriel b = besoinEnAttente(1);
      b.valider(CHAINE, RM, null, PLUS_TARD);
      assertThatThrownBy(() -> b.valider(CHAINE, RM, null, PLUS_TARD))
          .isInstanceOf(TransitionInterditeException.class)
          .hasFieldOrPropertyWithValue("code", "TRANSITION_STATUT_INTERDITE");
    }
  }

  @Nested
  class Rejet {

    @Test
    void should_rejeter_avec_motif_when_responsable_materiel_R01_R08() {
      BesoinMateriel b = besoinEnAttente(1);
      b.extraireEvenements();

      b.rejeter(CHAINE, RM, "  Doublon du besoin BES-2026-0003 ", PLUS_TARD);

      assertThat(b.statut()).isEqualTo(StatutBesoin.REJETE);
      assertThat(b.motifRejet()).isEqualTo("Doublon du besoin BES-2026-0003");
      assertThat(b.dateValidation()).isEqualTo(PLUS_TARD);
      assertThat(b.historique().get(1).commentaire()).isEqualTo(b.motifRejet());
      assertThat(types(b)).containsExactly(TypeEvenementBesoin.REJETE);
    }

    @Test
    void should_refuser_le_rejet_sans_motif_R08() {
      BesoinMateriel b = besoinEnAttente(1);
      assertThatThrownBy(() -> b.rejeter(CHAINE, RM, " ", PLUS_TARD))
          .isInstanceOf(BusinessRuleException.class)
          .hasFieldOrPropertyWithValue("code", "MOTIF_REJET_OBLIGATOIRE");
      assertThatThrownBy(() -> b.rejeter(CHAINE, RM, null, PLUS_TARD))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void should_refuser_le_rejet_when_pas_responsable_materiel_R01() {
      BesoinMateriel b = besoinEnAttente(1);
      assertThatThrownBy(() -> b.rejeter(CHAINE, CHEF, "Motif", PLUS_TARD))
          .isInstanceOf(ValidationNonAutoriseeException.class);
    }

    @Test
    void should_refuser_le_rejet_d_un_besoin_valide() {
      BesoinMateriel b = besoinEnAttente(1);
      b.valider(CHAINE, RM, null, PLUS_TARD);
      assertThatThrownBy(() -> b.rejeter(CHAINE, RM, "Motif", PLUS_TARD))
          .isInstanceOf(TransitionInterditeException.class);
    }
  }

  @Nested
  class Orientation {

    private BesoinMateriel valide(int quantite) {
      BesoinMateriel b = besoinEnAttente(quantite);
      b.valider(CHAINE, RM, null, PLUS_TARD);
      b.extraireEvenements();
      return b;
    }

    @Test
    void should_passer_affecte_direct_when_quantite_complete() {
      BesoinMateriel b = valide(2);
      UUID m1 = UUID.randomUUID();
      UUID m2 = UUID.randomUUID();

      b.affecterDirectement(List.of(m1, m2, m1), RM, PLUS_TARD);

      assertThat(b.statut()).isEqualTo(StatutBesoin.AFFECTE_DIRECT);
      assertThat(b.orientationEnAttente()).isFalse();
      assertThat(b.materielIdsAffectes()).containsExactly(m1, m2);
      assertThat(b.quantiteRestanteAAffecter()).isZero();
      assertThat(actions(b)).last().isEqualTo(ActionHistorique.AFFECTATION_DIRECTE);
      assertThat(types(b)).containsExactly(TypeEvenementBesoin.AFFECTE_DIRECT);
    }

    @Test
    void should_refuser_une_affectation_directe_incomplete() {
      BesoinMateriel b = valide(2);
      assertThatThrownBy(() -> b.affecterDirectement(List.of(UUID.randomUUID()), RM, PLUS_TARD))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_passer_en_achat() {
      BesoinMateriel b = valide(1);
      b.orienterVersAchat(null, RM, "0 disponible", PLUS_TARD);

      assertThat(b.statut()).isEqualTo(StatutBesoin.EN_ACHAT);
      assertThat(b.orientationEnAttente()).isFalse();
      assertThat(b.historique())
          .last()
          .extracting(HistoriqueBesoin::commentaire)
          .isEqualTo("0 disponible");
      assertThat(types(b)).containsExactly(TypeEvenementBesoin.EN_ACHAT);
    }

    @Test
    void should_suspendre_en_conservant_les_affectations_partielles_puis_completer() {
      BesoinMateriel b = valide(2);
      UUID m1 = UUID.randomUUID();
      b.suspendreOrientation(List.of(m1), RM, "ms-suivi indisponible", PLUS_TARD);

      assertThat(b.statut()).isEqualTo(StatutBesoin.VALIDE);
      assertThat(b.orientationEnAttente()).isTrue();
      assertThat(b.quantiteRestanteAAffecter()).isEqualTo(1);
      assertThat(types(b)).isEmpty();

      b.affecterDirectement(List.of(UUID.randomUUID()), RM, PLUS_TARD);
      assertThat(b.statut()).isEqualTo(StatutBesoin.AFFECTE_DIRECT);
      assertThat(b.materielIdsAffectes()).hasSize(2).startsWith(m1);
    }

    @Test
    void should_refuser_l_orientation_d_un_besoin_non_valide() {
      BesoinMateriel enAttente = besoinEnAttente(1);
      assertThatThrownBy(enAttente::exigerOrientationEnAttente)
          .isInstanceOf(ConflictException.class)
          .hasFieldOrPropertyWithValue("code", "ORIENTATION_NON_REQUISE");
      BesoinMateriel achat = valide(1);
      achat.orienterVersAchat(List.of(), RM, null, PLUS_TARD);
      assertThatThrownBy(() -> achat.orienterVersAchat(List.of(), RM, null, PLUS_TARD))
          .isInstanceOf(ConflictException.class);
    }

    @Test
    void should_journaliser_une_affectation_notifiee_sans_changer_l_etat() {
      BesoinMateriel b = valide(1);
      b.orienterVersAchat(List.of(), RM, null, PLUS_TARD);
      b.journaliserAffectationMateriel("Matériel PEL-01 affecté", Acteur.systeme(null), PLUS_TARD);

      assertThat(b.statut()).isEqualTo(StatutBesoin.EN_ACHAT);
      assertThat(b.historique())
          .last()
          .satisfies(
              h -> {
                assertThat(h.action()).isEqualTo(ActionHistorique.MATERIEL_AFFECTE);
                assertThat(h.auteurNom()).isEqualTo("Système");
              });
    }

    @Test
    void should_restituer_un_etat_coherent() {
      BesoinMateriel b = valide(1);
      EtatBesoin etat = b.etat();
      BesoinMateriel copie = BesoinMateriel.reconstituer(etat);

      assertThat(copie.etat()).isEqualTo(etat);
      assertThat(copie.reference()).isEqualTo("BES-2026-0001");
      assertThat(copie.motifRejet()).isNull();
      assertThat(copie.historique()).hasSize(2);
    }
  }
}
