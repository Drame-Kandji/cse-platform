package sn.cse.besoins.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sn.cse.besoins.Fixtures.AUJOURDHUI;
import static sn.cse.besoins.Fixtures.CHANTIER_1;
import static sn.cse.besoins.Fixtures.CHANTIER_2;
import static sn.cse.besoins.Fixtures.CHANTIER_3;
import static sn.cse.besoins.Fixtures.MAINTENANT;
import static sn.cse.besoins.Fixtures.USER_CHEF;
import static sn.cse.besoins.Fixtures.USER_RM;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sn.cse.besoins.application.command.ContenuBesoin;
import sn.cse.besoins.application.command.ModifierBesoinCommand;
import sn.cse.besoins.application.command.SoumettreBesoinCommand;
import sn.cse.besoins.application.exception.ServiceSuiviIndisponibleException;
import sn.cse.besoins.application.port.out.ChantierPort;
import sn.cse.besoins.application.port.out.ChantierPort.ChantierInfo;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.besoins.domain.model.TypeEvenementBesoin;
import sn.cse.besoins.domain.validation.ChaineValidation;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.ForbiddenOperationException;

class SoumettreEtModifierBesoinServiceTest {

  private final Fakes.Depot depot = new Fakes.Depot();
  private final Fakes.Publication publication = new Fakes.Publication();
  private final ChantierPort chantiers = mock(ChantierPort.class);
  private final Clock clock = Clock.fixed(MAINTENANT, ZoneOffset.UTC);
  private SoumettreBesoinService soumission;
  private ModifierBesoinService modification;

  @BeforeEach
  void preparer() {
    EnregistrementBesoin enregistrement = new EnregistrementBesoin(depot, publication);
    VerificationChantier verification = new VerificationChantier(chantiers);
    soumission =
        new SoumettreBesoinService(
            verification, annee -> "BES-" + annee + "-0042", enregistrement, clock);
    modification = new ModifierBesoinService(verification, enregistrement, clock);
    when(chantiers.trouver(CHANTIER_1))
        .thenReturn(
            Optional.of(new ChantierInfo(CHANTIER_1, "Extension autoroute Dakar–AIBD", true)));
    when(chantiers.trouver(CHANTIER_2))
        .thenReturn(Optional.of(new ChantierInfo(CHANTIER_2, "Contournement de Thiès RN2", false)));
  }

  private static ContenuBesoin contenu(java.util.UUID chantier, Priorite priorite, String justif) {
    return new ContenuBesoin(
        chantier,
        "Pelle hydraulique 20 t",
        CategorieMateriel.MATERIEL_ROULANT,
        null,
        2,
        null,
        priorite,
        30,
        AUJOURDHUI.plusDays(3),
        justif);
  }

  @Test
  void should_soumettre_un_besoin_sur_un_chantier_actif() {
    BesoinVue vue =
        soumission.soumettre(
            new SoumettreBesoinCommand(contenu(CHANTIER_1, Priorite.NORMAL, null), USER_CHEF));

    assertThat(vue.reference()).isEqualTo("BES-2026-0042");
    assertThat(vue.statut()).isEqualTo(StatutBesoin.EN_ATTENTE);
    assertThat(vue.chantierNom()).isEqualTo("Extension autoroute Dakar–AIBD");
    assertThat(vue.demandeurNom()).isEqualTo("Mamadou Diop");
    assertThat(vue.version()).isZero();
    assertThat(publication.types).containsExactly(TypeEvenementBesoin.SOUMIS);
  }

  @Test
  void should_refuser_sans_justification_avant_tout_appel_distant_R08() {
    assertThatThrownBy(
            () ->
                soumission.soumettre(
                    new SoumettreBesoinCommand(
                        contenu(CHANTIER_1, Priorite.CRITIQUE, null), USER_CHEF)))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", "JUSTIFICATION_OBLIGATOIRE");
    verify(chantiers, never()).trouver(any());
  }

  @Test
  void should_refuser_un_chantier_non_attribue_au_chef() {
    assertThatThrownBy(
            () ->
                soumission.soumettre(
                    new SoumettreBesoinCommand(
                        contenu(CHANTIER_3, Priorite.NORMAL, null), USER_CHEF)))
        .isInstanceOf(ForbiddenOperationException.class)
        .hasFieldOrPropertyWithValue("code", "CHANTIER_NON_ATTRIBUE");
  }

  @Test
  void should_refuser_un_chantier_inexistant_ou_termine() {
    when(chantiers.trouver(CHANTIER_3)).thenReturn(Optional.empty());
    assertThatThrownBy(
            () ->
                soumission.soumettre(
                    new SoumettreBesoinCommand(
                        contenu(CHANTIER_3, Priorite.NORMAL, null), USER_RM)))
        .hasFieldOrPropertyWithValue("code", "CHANTIER_INTROUVABLE");
    assertThatThrownBy(
            () ->
                soumission.soumettre(
                    new SoumettreBesoinCommand(
                        contenu(CHANTIER_2, Priorite.NORMAL, null), USER_CHEF)))
        .hasFieldOrPropertyWithValue("code", "CHANTIER_INACTIF");
    assertThat(depot.etats).isEmpty();
  }

  @Test
  void should_propager_l_indisponibilite_de_ms_suivi_a_la_soumission() {
    when(chantiers.trouver(CHANTIER_3)).thenThrow(new ServiceSuiviIndisponibleException("test"));
    assertThatThrownBy(
            () ->
                soumission.soumettre(
                    new SoumettreBesoinCommand(
                        contenu(CHANTIER_3, Priorite.NORMAL, null), USER_RM)))
        .isInstanceOf(ServiceSuiviIndisponibleException.class)
        .satisfies(
            e -> assertThat(((ServiceSuiviIndisponibleException) e).status()).isEqualTo(503));
  }

  @Test
  void should_modifier_un_besoin_en_attente_R05() {
    BesoinVue cree =
        soumission.soumettre(
            new SoumettreBesoinCommand(contenu(CHANTIER_1, Priorite.NORMAL, null), USER_CHEF));

    BesoinVue modifie =
        modification.modifier(
            new ModifierBesoinCommand(
                cree.id(),
                cree.version(),
                contenu(CHANTIER_1, Priorite.URGENT, "Accélération"),
                USER_CHEF));

    assertThat(modifie.priorite()).isEqualTo(Priorite.URGENT);
    assertThat(modifie.version()).isEqualTo(1);
    assertThat(publication.types)
        .containsExactly(TypeEvenementBesoin.SOUMIS, TypeEvenementBesoin.MODIFIE);
  }

  @Test
  void should_verifier_le_nouveau_chantier_lors_d_une_modification() {
    BesoinVue cree =
        soumission.soumettre(
            new SoumettreBesoinCommand(contenu(CHANTIER_1, Priorite.NORMAL, null), USER_CHEF));
    assertThatThrownBy(
            () ->
                modification.modifier(
                    new ModifierBesoinCommand(
                        cree.id(), null, contenu(CHANTIER_2, Priorite.NORMAL, null), USER_CHEF)))
        .hasFieldOrPropertyWithValue("code", "CHANTIER_INACTIF");
  }

  @Test
  void should_refuser_la_modification_d_un_besoin_valide_R05() {
    BesoinVue cree =
        soumission.soumettre(
            new SoumettreBesoinCommand(contenu(CHANTIER_1, Priorite.NORMAL, null), USER_CHEF));
    var besoin = depot.trouver(cree.id()).orElseThrow();
    besoin.valider(ChaineValidation.parDefaut(), sn.cse.besoins.Fixtures.RM, null, MAINTENANT);
    BesoinVue valide = BesoinVue.depuis(depot.enregistrer(besoin));

    assertThatThrownBy(
            () ->
                modification.modifier(
                    new ModifierBesoinCommand(
                        cree.id(),
                        valide.version(),
                        contenu(CHANTIER_2, Priorite.NORMAL, null),
                        USER_CHEF)))
        .isInstanceOf(ConflictException.class)
        .hasFieldOrPropertyWithValue("code", "BESOIN_NON_MODIFIABLE");
    verify(chantiers, never()).trouver(CHANTIER_2);
  }
}
