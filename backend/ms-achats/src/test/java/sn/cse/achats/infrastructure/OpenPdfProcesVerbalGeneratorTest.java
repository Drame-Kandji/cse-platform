package sn.cse.achats.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import sn.cse.achats.Donnees;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.TypeProcesVerbal;
import sn.cse.achats.domain.service.MoteurEvaluation;
import sn.cse.achats.infrastructure.pdf.OpenPdfProcesVerbalGenerator;

class OpenPdfProcesVerbalGeneratorTest {

  private final OpenPdfProcesVerbalGenerator generateur = new OpenPdfProcesVerbalGenerator();
  private final MoteurEvaluation moteur = MoteurEvaluation.standard();

  @Test
  void should_produire_pv_ouverture_et_evaluation() {
    AppelOffre cloture = Donnees.aoCloture("298000000", "312000000");
    AppelOffre attribue = Donnees.aoAttribue();

    byte[] ouverture =
        generateur.generer(
            TypeProcesVerbal.OUVERTURE, cloture, moteur.evaluer(cloture), Instant.now());
    byte[] evaluation =
        generateur.generer(
            TypeProcesVerbal.EVALUATION, attribue, moteur.evaluer(attribue), Instant.now());

    assertThat(new String(ouverture, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    assertThat(evaluation.length).isGreaterThan(1500);
  }

  @Test
  void should_produire_pv_sans_offre() {
    AppelOffre ao = Donnees.aoPublie();
    ao.cloturer(Donnees.LIMITE);

    assertThat(
            generateur.generer(TypeProcesVerbal.OUVERTURE, ao, moteur.evaluer(ao), Instant.now()))
        .isNotEmpty();
  }
}
