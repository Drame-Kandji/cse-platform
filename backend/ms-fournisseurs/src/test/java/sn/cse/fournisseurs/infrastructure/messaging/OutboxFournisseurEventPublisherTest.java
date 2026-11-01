package sn.cse.fournisseurs.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sn.cse.common.outbox.OutboxWriter;
import sn.cse.events.EventTypes;
import sn.cse.events.Topics;
import sn.cse.events.payload.FournisseurPayload;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.support.Fixtures;

class OutboxFournisseurEventPublisherTest {

  @Test
  void should_transmettre_l_e_mail_du_fournisseur_when_inscription_validee() {
    OutboxWriter outbox = mock(OutboxWriter.class);
    Fournisseur f = Fixtures.enAttente();
    f.validerInscription();

    new OutboxFournisseurEventPublisher(outbox)
        .publierFournisseur(EventTypes.FOURNISSEUR_VALIDE, f, null);

    ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
    verify(outbox)
        .publier(
            eq(Topics.FOURNISSEURS),
            eq(EventTypes.FOURNISSEUR_VALIDE),
            eq("Fournisseur"),
            eq(f.getId()),
            payload.capture(),
            anyString());
    assertThat(payload.getValue())
        .isInstanceOfSatisfying(
            FournisseurPayload.class,
            p -> {
              assertThat(p.fournisseurId()).isEqualTo(f.getId());
              assertThat(p.email()).isEqualTo("contact@exemple.sn");
              assertThat(p.raisonSociale()).isEqualTo("Baobab Logistique");
              assertThat(p.statut()).isEqualTo("ACTIF");
            });
  }
}
