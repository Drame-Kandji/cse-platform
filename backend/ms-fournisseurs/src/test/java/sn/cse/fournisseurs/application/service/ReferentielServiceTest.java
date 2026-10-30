package sn.cse.fournisseurs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.command.DocumentCommand;
import sn.cse.fournisseurs.application.command.FournisseurCommand;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.domain.model.DocumentAdministratif;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.domain.model.TypeDocument;
import sn.cse.fournisseurs.support.Fixtures;

class ReferentielServiceTest {

  private final Contexte ctx = new Contexte();

  @Test
  void should_creer_fournisseur_actif_et_publier_inscription() {
    FournisseurVue vue =
        ctx.referentiel.creer(Fixtures.commande("Sahel Engins SA", "005123456 2G3"), Fixtures.ra());
    assertThat(vue.fournisseur().getStatut()).isEqualTo(StatutFournisseur.ACTIF);
    assertThat(vue.fournisseur().getPerformance().scorePerformance()).isEqualByComparingTo("50");
    assertThat(vue.coordonneesBancairesVisibles()).isTrue();
    assertThat(vue.fournisseur().getCoordonneesBancaires()).isPresent();
    assertThat(ctx.publications.types()).containsExactly(EventTypes.FOURNISSEUR_INSCRIT);
  }

  @Test
  void should_creer_sans_coordonnees_bancaires() {
    FournisseurCommand c = Fixtures.commande("Sans banque", "001234567 1A1");
    FournisseurCommand sansBanque =
        new FournisseurCommand(
            c.raisonSociale(),
            c.ninea(),
            null,
            null,
            null,
            c.pays(),
            null,
            c.email(),
            null,
            null,
            c.specialites(),
            null,
            " ",
            null);
    assertThat(
            ctx.referentiel
                .creer(sansBanque, Fixtures.ra())
                .fournisseur()
                .getCoordonneesBancaires())
        .isEmpty();
  }

  @Test
  void should_refuser_ninea_deja_utilise() {
    ctx.referentiel.creer(Fixtures.commande("A", "005123456 2G3"), Fixtures.ra());
    assertThatThrownBy(
            () -> ctx.referentiel.creer(Fixtures.commande("B", "005123456 2g3"), Fixtures.ra()))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("NINEA");
  }

  @Test
  void should_modifier_avec_version_courante_et_publier() {
    UUID id =
        ctx.referentiel
            .creer(Fixtures.commande("A", "005123456 2G3"), Fixtures.ra())
            .fournisseur()
            .getId();
    FournisseurVue vue =
        ctx.referentiel.modifier(
            id, 0, Fixtures.commande("A modifié", "005123456 2G3"), Fixtures.ra());
    assertThat(vue.fournisseur().getProfil().raisonSociale()).isEqualTo("A modifié");
    assertThat(vue.fournisseur().getVersion()).isEqualTo(1);
    assertThat(ctx.publications.types()).contains(FournisseurEventPublisher.FOURNISSEUR_MODIFIE);
  }

  @Test
  void should_refuser_modification_when_version_obsolete() {
    UUID id =
        ctx.referentiel
            .creer(Fixtures.commande("A", "005123456 2G3"), Fixtures.ra())
            .fournisseur()
            .getId();
    ctx.referentiel.modifier(id, 0, Fixtures.commande("A1", "005123456 2G3"), Fixtures.ra());
    assertThatThrownBy(
            () ->
                ctx.referentiel.modifier(
                    id, 0, Fixtures.commande("A2", "005123456 2G3"), Fixtures.ra()))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void should_refuser_modification_vers_ninea_d_un_autre() {
    ctx.referentiel.creer(Fixtures.commande("A", "005123456 2G3"), Fixtures.ra());
    UUID b =
        ctx.referentiel
            .creer(Fixtures.commande("B", "006234567 2V1"), Fixtures.ra())
            .fournisseur()
            .getId();
    assertThatThrownBy(
            () ->
                ctx.referentiel.modifier(
                    b, 0, Fixtures.commande("B", "005123456 2G3"), Fixtures.ra()))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void should_ajouter_document() {
    UUID id =
        ctx.referentiel
            .creer(Fixtures.commande("A", "005123456 2G3"), Fixtures.ra())
            .fournisseur()
            .getId();
    DocumentAdministratif doc =
        ctx.referentiel.ajouterDocument(
            id,
            new DocumentCommand(
                TypeDocument.QUITUS_FISCAL, "Quitus 2026", LocalDate.of(2026, 12, 31)));
    assertThat(doc.getType()).isEqualTo(TypeDocument.QUITUS_FISCAL);
    assertThat(ctx.fournisseurs.parId(id).orElseThrow().getDocuments()).hasSize(1);
  }

  @Test
  void should_lever_404_when_fournisseur_inconnu() {
    UUID inconnu = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                ctx.referentiel.ajouterDocument(
                    inconnu, new DocumentCommand(TypeDocument.AUTRE, "x", null)))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining(inconnu.toString());
  }
}
