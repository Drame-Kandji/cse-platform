package sn.cse.besoins.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static sn.cse.besoins.Fixtures.CHANTIER_1;
import static sn.cse.besoins.Fixtures.CHEF;
import static sn.cse.besoins.Fixtures.MAINTENANT;
import static sn.cse.besoins.Fixtures.RM;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.besoins.Fixtures;
import sn.cse.besoins.domain.exception.ValidationNonAutoriseeException;
import sn.cse.besoins.domain.model.Acteur;
import sn.cse.besoins.domain.model.ActionHistorique;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.common.security.Roles;

class ChaineValidationTest {

  private static final String DP = "DIRECTEUR_PROJET";
  private static final Acteur DIRECTEUR = new Acteur(UUID.randomUUID(), "Directeur", Set.of(DP));

  /** Chaîne à deux étapes : RM puis Directeur de projet à partir de 10 unités ou CRITIQUE. */
  private static final ChaineValidation DEUX_ETAPES =
      new ChaineValidation(
          List.of(
              new EtapeValidation(
                  ChaineValidation.ETAPE_RESPONSABLE_MATERIEL, Roles.RESPONSABLE_MATERIEL, null),
              new EtapeValidation(DP, DP, new CritereDeclenchement(10, Priorite.CRITIQUE))));

  private static BesoinMateriel besoin(int quantite, Priorite priorite) {
    return BesoinMateriel.soumettre(
        UUID.randomUUID(),
        "BES-2026-0001",
        CHANTIER_1,
        "Chantier",
        CHEF,
        Fixtures.description(quantite, priorite),
        MAINTENANT);
  }

  @Test
  void should_valider_en_une_etape_par_defaut_R01() {
    BesoinMateriel b = besoin(50, Priorite.CRITIQUE);
    DecisionValidation decision = ChaineValidation.parDefaut().traiter(b, RM);

    assertThat(decision.estFinale()).isTrue();
    assertThat(decision.etapeApprouvee().roleRequis()).isEqualTo(Roles.RESPONSABLE_MATERIEL);
  }

  @Test
  void should_exiger_l_etape_superieure_au_dela_du_seuil_R01() {
    BesoinMateriel b = besoin(12, Priorite.NORMAL);

    DecisionValidation premiere = b.valider(DEUX_ETAPES, RM, "OK RM", MAINTENANT);
    assertThat(premiere.estFinale()).isFalse();
    assertThat(premiere.etapeSuivante()).get().extracting(EtapeValidation::code).isEqualTo(DP);
    assertThat(b.statut()).isEqualTo(StatutBesoin.EN_ATTENTE);
    assertThat(b.historique())
        .last()
        .extracting(h -> h.action())
        .isEqualTo(ActionHistorique.APPROBATION_ETAPE);
    assertThat(b.extraireEvenements()).hasSize(1); // seulement SOUMIS

    // Le RM ne peut pas franchir l'étape du directeur (R01).
    assertThatThrownBy(() -> b.valider(DEUX_ETAPES, RM, null, MAINTENANT))
        .isInstanceOf(ValidationNonAutoriseeException.class);
    assertThatThrownBy(() -> b.rejeter(DEUX_ETAPES, RM, "motif", MAINTENANT))
        .isInstanceOf(ValidationNonAutoriseeException.class);

    DecisionValidation finale = b.valider(DEUX_ETAPES, DIRECTEUR, null, MAINTENANT);
    assertThat(finale.estFinale()).isTrue();
    assertThat(b.statut()).isEqualTo(StatutBesoin.VALIDE);
    assertThat(b.validateurNom()).isEqualTo("Directeur");
    assertThat(DEUX_ETAPES.etapeEnAttente(b)).isEmpty();
  }

  @Test
  void should_declencher_l_etape_superieure_par_priorite() {
    BesoinMateriel b = besoin(1, Priorite.CRITIQUE);
    assertThat(DEUX_ETAPES.traiter(b, RM).estFinale()).isFalse();
  }

  @Test
  void should_ignorer_l_etape_superieure_sous_le_seuil() {
    BesoinMateriel b = besoin(2, Priorite.URGENT);
    assertThat(b.valider(DEUX_ETAPES, RM, null, MAINTENANT).estFinale()).isTrue();
    assertThat(b.statut()).isEqualTo(StatutBesoin.VALIDE);
  }

  @Test
  void should_refuser_une_configuration_incoherente() {
    assertThatThrownBy(() -> new ChaineValidation(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new ChaineValidation(
                    List.of(new EtapeValidation(DP, DP, new CritereDeclenchement(5, null)))))
        .isInstanceOf(IllegalArgumentException.class);
    EtapeValidation rm = new EtapeValidation("RM", Roles.RESPONSABLE_MATERIEL, null);
    assertThatThrownBy(() -> new ChaineValidation(List.of(rm, rm)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new EtapeValidation(" ", "R", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new EtapeValidation("X", null, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(DEUX_ETAPES.etapes()).hasSize(2);
  }
}
