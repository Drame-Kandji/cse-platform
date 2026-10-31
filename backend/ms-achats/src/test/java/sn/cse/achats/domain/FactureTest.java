package sn.cse.achats.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import sn.cse.achats.Donnees;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.Facture;
import sn.cse.achats.domain.model.StatutFacture;
import sn.cse.achats.domain.service.SyntheseFinance;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.CseException;

class FactureTest {

  private static final LocalDate EMISSION = LocalDate.of(2026, 10, 20);

  private static String code(Throwable t) {
    return ((CseException) t).getCode();
  }

  static ContratAchat facturable() {
    ContratAchat c = Donnees.contratBrouillon();
    c.signer(Instant.parse("2026-10-15T10:00:00Z"));
    c.deciderEngagement(true, null, Donnees.DAF, Instant.parse("2026-10-16T10:00:00Z"));
    return c;
  }

  private static Facture recue(ContratAchat c, String ht) {
    return Facture.recevoir(
        c,
        "FA-001",
        new BigDecimal(ht),
        new BigDecimal(ht).multiply(new BigDecimal("0.18")),
        EMISSION,
        EMISSION.plusDays(30),
        BigDecimal.ZERO);
  }

  @Test
  void should_recevoir_facture_sur_contrat_engage() {
    Facture f = recue(facturable(), "50000000");

    assertThat(f.getStatut()).isEqualTo(StatutFacture.RECUE);
    assertThat(f.getMontantTtc()).isEqualByComparingTo("59000000");
    assertThat(f.getContratNumero()).isEqualTo("CTR-2026-001");
    assertThat(f.getFournisseurNom()).isEqualTo("Sahel Engins SA");
  }

  @Test
  void should_refuser_facture_when_contrat_non_engage() {
    ContratAchat brouillon = Donnees.contratBrouillon();

    assertThatThrownBy(() -> recue(brouillon, "100"))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.CONTRAT_NON_FACTURABLE));
  }

  @Test
  void should_refuser_depassement_montant_et_dates_incoherentes() {
    ContratAchat c = facturable();

    assertThatThrownBy(
            () ->
                Facture.recevoir(
                    c,
                    "FA-2",
                    new BigDecimal("100"),
                    BigDecimal.ZERO,
                    EMISSION,
                    EMISSION.plusDays(1),
                    new BigDecimal("119999950")))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.DEPASSEMENT_MONTANT_CONTRAT));
    assertThatThrownBy(
            () ->
                Facture.recevoir(
                    c,
                    "FA-3",
                    new BigDecimal("100"),
                    BigDecimal.ZERO,
                    EMISSION,
                    EMISSION.minusDays(1),
                    BigDecimal.ZERO))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.ECHEANCE_INVALIDE));
    assertThatThrownBy(
            () ->
                Facture.recevoir(
                    c,
                    "FA-4",
                    new BigDecimal("100"),
                    new BigDecimal("-1"),
                    EMISSION,
                    EMISSION,
                    BigDecimal.ZERO))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.DONNEE_INVALIDE));
  }

  @Test
  void should_mandater_puis_payer() {
    Facture f = recue(facturable(), "100");

    f.mandater("MDT-2026-0001", EMISSION.plusDays(5));
    assertThat(f.getStatut()).isEqualTo(StatutFacture.MANDATEE);
    assertThat(f.getReferenceMandat()).isEqualTo("MDT-2026-0001");
    f.payer(EMISSION.plusDays(10));

    assertThat(f.getStatut()).isEqualTo(StatutFacture.PAYEE);
    assertThat(f.getDatePaiement()).isEqualTo(EMISSION.plusDays(10));
    assertThat(f.estEnRetard(EMISSION.plusDays(90))).isFalse();
  }

  @Test
  void should_refuser_transitions_interdites() {
    Facture f = recue(facturable(), "100");

    assertThatThrownBy(() -> f.payer(EMISSION)).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> f.rejeter(" "))
        .satisfies(e -> assertThat(code(e)).isEqualTo(CodesErreur.MOTIF_OBLIGATOIRE));
    f.rejeter("Doublon");
    assertThat(f.getMotifRejet()).isEqualTo("Doublon");
    assertThatThrownBy(() -> f.mandater("M", EMISSION)).isInstanceOf(ConflictException.class);
  }

  @Test
  void should_signaler_retard_when_echeance_depassee_et_non_payee() {
    Facture f = recue(facturable(), "100");

    assertThat(f.estEnRetard(EMISSION.plusDays(30))).isFalse();
    assertThat(f.estEnRetard(EMISSION.plusDays(31))).isTrue();
    f.rejeter("Erreur");
    assertThat(f.estEnRetard(EMISSION.plusDays(31))).isFalse();
  }

  @Test
  void should_calculer_synthese_financiere_en_memoire() {
    ContratAchat engage = facturable();
    ContratAchat enAttente = Donnees.contratBrouillon();
    enAttente.signer(Instant.parse("2026-10-15T10:00:00Z"));
    Facture payee = recue(engage, "10000000");
    payee.mandater("M1", EMISSION);
    payee.payer(EMISSION);
    Facture mandatee = recue(engage, "20000000");
    mandatee.mandater("M2", EMISSION);
    Facture enRetard = recue(engage, "5000000");
    Facture rejetee = recue(engage, "1000000");
    rejetee.rejeter("Erreur");

    SyntheseFinance s =
        SyntheseFinance.calculer(
            List.of(engage, enAttente, Donnees.contratBrouillon()),
            List.of(payee, mandatee, enRetard, rejetee),
            EMISSION.plusDays(40));

    assertThat(s.montantEngage()).isEqualByComparingTo("120000000");
    assertThat(s.montantEngagementsEnAttente()).isEqualByComparingTo("120000000");
    assertThat(s.engagementsEnAttente()).isEqualTo(1);
    assertThat(s.montantFacture()).isEqualByComparingTo("41300000");
    assertThat(s.montantMandate()).isEqualByComparingTo("35400000");
    assertThat(s.montantPaye()).isEqualByComparingTo("11800000");
    assertThat(s.facturesEnRetard()).isEqualTo(2);
  }
}
