package sn.cse.chatbot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.chatbot.application.service.HistoriqueService;
import sn.cse.chatbot.application.service.ReponseGeneree;
import sn.cse.chatbot.domain.Donnees;
import sn.cse.chatbot.domain.model.Intention;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;

class HistoriqueServiceTest {

  private final Fakes.Conversations repo = new Fakes.Conversations();
  private final HistoriqueService service =
      new HistoriqueService(repo, ChatbotServiceTest.parametres());
  private final CurrentUser moi = Donnees.user(Roles.RESPONSABLE_MATERIEL);
  private final CurrentUser autre = Donnees.user(Roles.RESPONSABLE_MATERIEL);

  @Test
  void should_create_list_read_and_delete_own_conversations_only() {
    HistoriqueService.Echange e1 =
        service.enregistrer(
            moi,
            null,
            "Où est la pelle ?",
            new ReponseGeneree("Au dépôt", true),
            List.of(),
            Intention.LOCALISER_ENGIN,
            ChatbotServiceTest.T0);
    service.enregistrer(
        moi,
        e1.conversationId(),
        "Et la niveleuse ?",
        new ReponseGeneree("À Thiès", false),
        List.of(),
        Intention.LOCALISER_ENGIN,
        ChatbotServiceTest.T0.plusSeconds(5));
    assertThat(service.lister(moi, new PageQuery(0, 20, "updatedAt", false)).content())
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.titre()).isEqualTo("Où est la pelle ?");
              assertThat(r.nombreMessages()).isEqualTo(4);
              assertThat(r.dernierMessage()).isEqualTo("À Thiès");
            });
    assertThat(service.detail(moi, e1.conversationId()).messages()).hasSize(4);
    assertThat(service.lister(autre, new PageQuery(0, 20, "updatedAt", false)).content()).isEmpty();
    assertThatThrownBy(() -> service.detail(autre, e1.conversationId()))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.supprimer(autre, e1.conversationId()))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.detail(moi, UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(service.charger(moi, null)).isEmpty();
    service.supprimer(moi, e1.conversationId());
    assertThat(repo.stock).isEmpty();
  }
}
