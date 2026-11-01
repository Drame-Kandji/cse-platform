package sn.cse.fournisseurs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.CseException;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.command.InscriptionCommand;
import sn.cse.fournisseurs.domain.exception.CodesErreur;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.support.Fixtures;

class InscriptionServiceTest {

  private final Contexte ctx = new Contexte();
  private final InscriptionService service =
      new InscriptionService(ctx.fournisseurs, ctx.politique, ctx.publications, Fixtures.HORLOGE);

  private static InscriptionCommand inscription(String ninea, String email) {
    return new InscriptionCommand(
        "Baobab Logistique",
        ninea,
        null,
        null,
        "Thiès",
        "Sénégal",
        "+221 33 951 22 10",
        email,
        "Seynabou Gueye",
        "Gérante",
        EnumSet.of(CategorieMateriel.CONSOMMABLE));
  }

  @Test
  void should_inscrire_en_attente_et_publier_fournisseur_inscrit() {
    Fournisseur inscrit =
        service.inscrire(inscription("008789012 2e7", "Contact@Baobab-Logistique.SN"));

    assertThat(inscrit.getStatut()).isEqualTo(StatutFournisseur.EN_ATTENTE_VALIDATION);
    assertThat(inscrit.getProfil().ninea()).isEqualTo("008789012 2E7");
    assertThat(inscrit.getProfil().email()).isEqualTo("contact@baobab-logistique.sn");
    assertThat(inscrit.getProfil().certifications()).isEmpty();
    assertThat(inscrit.getCoordonneesBancaires()).isEmpty();
    assertThat(inscrit.getPerformance().scorePerformance()).isEqualByComparingTo("50");
    assertThat(inscrit.getDateInscription()).isEqualTo(Fixtures.AUJOURDHUI);
    assertThat(ctx.fournisseurs.donnees).containsKey(inscrit.getId());
    assertThat(ctx.publications.evenements)
        .singleElement()
        .satisfies(
            e -> {
              assertThat(e.type()).isEqualTo(EventTypes.FOURNISSEUR_INSCRIT);
              assertThat(e.aggregateId()).isEqualTo(inscrit.getId());
              assertThat(e.statut()).isEqualTo("EN_ATTENTE_VALIDATION");
            });
  }

  @Test
  void should_refuser_409_fournisseur_existant_when_ninea_deja_connu() {
    ctx.enregistrer(Fixtures.actif());
    assertThatThrownBy(() -> service.inscrire(inscription("005123456 2G3", "autre@exemple.sn")))
        .isInstanceOf(ConflictException.class)
        .extracting(e -> ((CseException) e).getCode())
        .isEqualTo(CodesErreur.FOURNISSEUR_EXISTANT);
    assertThat(ctx.publications.evenements).isEmpty();
  }

  @Test
  void should_refuser_409_fournisseur_existant_when_email_deja_connu_quelle_que_soit_la_casse() {
    ctx.enregistrer(Fixtures.actif());
    assertThatThrownBy(() -> service.inscrire(inscription("001234567 1A1", "CONTACT@exemple.sn")))
        .isInstanceOf(ConflictException.class)
        .extracting(e -> ((CseException) e).getCode())
        .isEqualTo(CodesErreur.FOURNISSEUR_EXISTANT);
    assertThat(ctx.fournisseurs.donnees).hasSize(1);
  }

  @Test
  void should_publier_fournisseur_valide_avec_e_mail_when_ra_valide_l_inscription() {
    Fournisseur inscrit =
        service.inscrire(inscription("008789012 2E7", "contact@baobab-logistique.sn"));

    ctx.cycleVie.validerInscription(inscrit.getId(), Fixtures.ra());

    assertThat(ctx.publications.types())
        .containsExactly(EventTypes.FOURNISSEUR_INSCRIT, EventTypes.FOURNISSEUR_VALIDE);
    assertThat(ctx.fournisseurs.parId(inscrit.getId()).orElseThrow().getStatut())
        .isEqualTo(StatutFournisseur.ACTIF);
  }
}
