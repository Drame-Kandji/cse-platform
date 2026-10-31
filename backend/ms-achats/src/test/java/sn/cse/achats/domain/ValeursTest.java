package sn.cse.achats.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.achats.Donnees;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.exception.Regles;
import sn.cse.achats.domain.model.Acteur;
import sn.cse.achats.domain.model.Approbation;
import sn.cse.achats.domain.model.BesoinAAcheter;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.ClausesContrat;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.CriterePondere;
import sn.cse.achats.domain.model.Engagement;
import sn.cse.achats.domain.model.Jalon;
import sn.cse.achats.domain.model.PonderationCriteres;
import sn.cse.achats.domain.model.Priorite;
import sn.cse.achats.domain.model.StatutApprobation;
import sn.cse.achats.domain.model.StatutFournisseur;
import sn.cse.achats.domain.model.StatutJalon;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.CseException;

class ValeursTest {

  @Test
  void should_valider_ponderation_somme_100() {
    PonderationCriteres grille = Donnees.grille();

    assertThat(grille.poids(CritereEvaluation.PRIX)).contains(40);
    assertThat(grille.contient(CritereEvaluation.SAV)).isFalse();
  }

  @Test
  void should_refuser_ponderation_invalide() {
    assertThatThrownBy(
            () -> new PonderationCriteres(List.of(new CriterePondere(CritereEvaluation.PRIX, 90))))
        .satisfies(
            e ->
                assertThat(((CseException) e).getCode())
                    .isEqualTo(CodesErreur.PONDERATION_INVALIDE))
        .hasMessageContaining("90");
    assertThatThrownBy(
            () ->
                new PonderationCriteres(
                    List.of(
                        new CriterePondere(CritereEvaluation.PRIX, 50),
                        new CriterePondere(CritereEvaluation.PRIX, 50))))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> new PonderationCriteres(List.of()))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> new CriterePondere(CritereEvaluation.SAV, 0))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_n_autoriser_que_les_fournisseurs_actifs_R14() {
    assertThat(StatutFournisseur.ACTIF.peutParticiper()).isTrue();
    assertThat(StatutFournisseur.SUSPENDU.peutParticiper()).isFalse();
    assertThat(StatutFournisseur.BLACKLISTE.peutParticiper()).isFalse();
    assertThat(StatutFournisseur.EN_ATTENTE_VALIDATION.peutParticiper()).isFalse();
    assertThat(CritereEvaluation.PRIX.estCalcule()).isTrue();
    assertThat(CritereEvaluation.SAV.estCalcule()).isFalse();
  }

  @Test
  void should_decider_approbation_une_seule_fois() {
    Approbation decidee = Approbation.enAttente().decider(false, Donnees.RM, Instant.EPOCH, "Non");

    assertThat(decidee.statut()).isEqualTo(StatutApprobation.REFUSEE);
    assertThat(decidee.estApprouvee()).isFalse();
    assertThat(decidee.parNom()).isEqualTo("Ibrahima Fall");
    assertThatThrownBy(() -> decidee.decider(true, Donnees.RM, Instant.EPOCH, null))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void should_decider_engagement_une_seule_fois() {
    Engagement demande = Engagement.demander(new BigDecimal("100"), Instant.EPOCH);
    Engagement valide = demande.decider(true, Donnees.DAF, Instant.EPOCH.plusSeconds(1), "OK");

    assertThat(valide.statut()).isEqualTo(StatutApprobation.APPROUVEE);
    assertThat(valide.parNom()).isEqualTo("Ousmane Ba");
    assertThatThrownBy(() -> valide.decider(false, Donnees.DAF, Instant.EPOCH, null))
        .satisfies(
            e ->
                assertThat(((CseException) e).getCode())
                    .isEqualTo(CodesErreur.ENGAGEMENT_NON_EN_ATTENTE));
  }

  @Test
  void should_calculer_statut_jalon_a_une_date() {
    LocalDate prevue = LocalDate.of(2026, 11, 10);
    Jalon jalon = new Jalon(UUID.randomUUID(), "Expédition", prevue, null);

    assertThat(jalon.statutAu(prevue)).isEqualTo(StatutJalon.A_VENIR);
    assertThat(jalon.statutAu(prevue.plusDays(1))).isEqualTo(StatutJalon.EN_RETARD);
    assertThat(
            new Jalon(jalon.id(), "Expédition", prevue, prevue.plusDays(3))
                .statutAu(prevue.plusDays(9)))
        .isEqualTo(StatutJalon.ATTEINT);
  }

  @Test
  void should_valider_clauses() {
    ClausesContrat c = new ClausesContrat(12, "  ", " Franco chantier ", new BigDecimal("0.1"));

    assertThat(c.conditionsMaintenance()).isNull();
    assertThat(c.conditionsLivraison()).isEqualTo("Franco chantier");
    assertThatThrownBy(() -> new ClausesContrat(-1, null, null, null))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> new ClausesContrat(1, null, null, new BigDecimal("101")))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void should_rattacher_besoin_une_seule_fois() {
    BesoinAAcheter b =
        new BesoinAAcheter(
            UUID.randomUUID(),
            "BES-2026-0001",
            "Pelle",
            CategorieMateriel.MATERIEL_ROULANT,
            2,
            null,
            Priorite.URGENT,
            UUID.randomUUID(),
            "Chantier",
            Instant.EPOCH,
            null);
    UUID ao = UUID.randomUUID();

    BesoinAAcheter rattache = b.rattacherA(ao);

    assertThat(rattache.appelOffreId()).isEqualTo(ao);
    assertThat(b.commeOrigine().designation()).isEqualTo("Pelle");
    assertThatThrownBy(() -> rattache.rattacherA(UUID.randomUUID()))
        .satisfies(
            e ->
                assertThat(((CseException) e).getCode())
                    .isEqualTo(CodesErreur.BESOIN_DEJA_EN_ACHAT));
  }

  @Test
  void should_appliquer_regles_utilitaires() {
    assertThat(Regles.facultatif("  ")).isNull();
    assertThat(Regles.texteObligatoire(" a ", "x")).isEqualTo("a");
    assertThatThrownBy(() -> Regles.texteObligatoire(null, "x"))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> Regles.montantPositif(null, "x"))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(Acteur.SYSTEME.id()).isNull();
  }
}
