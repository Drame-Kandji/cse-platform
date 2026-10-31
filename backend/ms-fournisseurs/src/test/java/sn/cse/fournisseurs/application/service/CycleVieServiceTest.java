package sn.cse.fournisseurs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.ConflictException;
import sn.cse.events.EventTypes;
import sn.cse.fournisseurs.application.port.out.FournisseurEventPublisher;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.support.Fixtures;
import sn.cse.fournisseurs.support.Memoire;

class CycleVieServiceTest {

  private final Contexte ctx = new Contexte();

  @Test
  void should_valider_inscription_et_publier() {
    UUID id = ctx.enregistrer(Fixtures.enAttente()).getId();
    assertThat(ctx.cycleVie.validerInscription(id, Fixtures.ra()).fournisseur().getStatut())
        .isEqualTo(StatutFournisseur.ACTIF);
    assertThat(ctx.publications.types()).containsExactly(EventTypes.FOURNISSEUR_VALIDE);
  }

  @Test
  void should_suspendre_puis_reactiver_avec_evenements() {
    UUID id = ctx.enregistrer(Fixtures.actif()).getId();
    ctx.cycleVie.suspendre(id, " Retards ", Fixtures.ra());
    ctx.cycleVie.reactiver(id, Fixtures.ra());
    assertThat(ctx.publications.evenements)
        .extracting(Memoire.Evenement::type, Memoire.Evenement::motif)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(EventTypes.FOURNISSEUR_SUSPENDU, "Retards"),
            org.assertj.core.groups.Tuple.tuple(EventTypes.FOURNISSEUR_REACTIVE, null));
  }

  @Test
  void should_blacklister_en_deux_etapes_R09() {
    UUID id = ctx.enregistrer(Fixtures.actif()).getId();
    ctx.cycleVie.suspendre(id, "Non-conformités", Fixtures.ra());
    var apresDemande = ctx.cycleVie.demanderBlacklistage(id, "Fraude", Fixtures.ra());
    assertThat(apresDemande.fournisseur().getStatut()).isEqualTo(StatutFournisseur.SUSPENDU);
    assertThat(apresDemande.fournisseur().getDemandeBlacklistage())
        .get()
        .satisfies(
            d -> {
              assertThat(d.demandeurId()).isEqualTo(Fixtures.RA_ID);
              assertThat(d.date()).isEqualTo(Fixtures.MAINTENANT);
            });
    var apresDecision = ctx.cycleVie.deciderBlacklistage(id, true, "Validé", Fixtures.dg());
    assertThat(apresDecision.fournisseur().getStatut()).isEqualTo(StatutFournisseur.BLACKLISTE);
    assertThat(ctx.publications.types())
        .containsExactly(
            EventTypes.FOURNISSEUR_SUSPENDU,
            EventTypes.FOURNISSEUR_BLACKLISTAGE_DEMANDE,
            EventTypes.FOURNISSEUR_BLACKLISTE);
    assertThat(ctx.publications.evenements.get(2).motif()).isEqualTo("Fraude · Validé");
  }

  @Test
  void should_publier_refus_when_dg_refuse_R09() {
    UUID id = ctx.enregistrer(Fixtures.actif()).getId();
    ctx.cycleVie.suspendre(id, "Non-conformités", Fixtures.ra());
    ctx.cycleVie.demanderBlacklistage(id, "Fraude", Fixtures.ra());
    var vue = ctx.cycleVie.deciderBlacklistage(id, false, " Preuves insuffisantes ", Fixtures.dg());
    assertThat(vue.fournisseur().getStatut()).isEqualTo(StatutFournisseur.SUSPENDU);
    Memoire.Evenement refus = ctx.publications.evenements.get(2);
    assertThat(refus.type()).isEqualTo(FournisseurEventPublisher.FOURNISSEUR_BLACKLISTAGE_REFUSE);
    assertThat(refus.motif()).isEqualTo("Demande de Fatou Sarr refusée : Preuves insuffisantes");
    ctx.cycleVie.demanderBlacklistage(id, "Nouveau motif", Fixtures.ra());
    ctx.cycleVie.deciderBlacklistage(id, false, null, Fixtures.dg());
    assertThat(ctx.publications.evenements.get(4).motif())
        .isEqualTo("Demande de Fatou Sarr refusée");
  }

  @Test
  void should_refuser_blacklistage_direct_d_un_actif_R09() {
    UUID id = ctx.enregistrer(Fixtures.actif()).getId();
    assertThatThrownBy(() -> ctx.cycleVie.demanderBlacklistage(id, "Fraude", Fixtures.ra()))
        .isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> ctx.cycleVie.deciderBlacklistage(id, true, null, Fixtures.dg()))
        .isInstanceOf(ConflictException.class);
    assertThat(ctx.publications.evenements).isEmpty();
  }
}
