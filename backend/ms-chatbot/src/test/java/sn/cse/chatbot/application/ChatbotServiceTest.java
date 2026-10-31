package sn.cse.chatbot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sn.cse.chatbot.application.exception.LimiteDebitDepasseeException;
import sn.cse.chatbot.application.port.in.QuestionCommand;
import sn.cse.chatbot.application.port.in.ReponseChatbot;
import sn.cse.chatbot.application.service.ChatbotService;
import sn.cse.chatbot.application.service.HistoriqueService;
import sn.cse.chatbot.application.service.SuggestionsService;
import sn.cse.chatbot.domain.Donnees;
import sn.cse.chatbot.domain.model.Intention;
import sn.cse.chatbot.domain.model.Procedure;
import sn.cse.chatbot.domain.model.Source;
import sn.cse.chatbot.domain.model.TypeSource;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;

class ChatbotServiceTest {

  static final Instant T0 = Instant.parse("2026-10-30T09:15:00Z");

  static ParametresChatbot parametres() {
    return new ParametresChatbot(
        1000,
        6000,
        4000,
        10,
        10,
        30,
        6,
        80,
        160,
        6,
        Map.of(
            TypeSource.MATERIEL,
            "/materiels/{id}",
            TypeSource.BESOIN,
            "/besoins/{id}",
            TypeSource.CONTRAT,
            "/contrats/{id}"),
        Donnees.ACCES,
        Donnees.MONTANTS,
        Map.of(
            Roles.CHEF_CHANTIER,
            List.of("Quels engins sont affectés à mon chantier ?"),
            "DEFAUT",
            List.of("Comment exprimer un besoin ?")),
        List.of(
            new Procedure(
                "Signaler une panne",
                List.of("signaler une panne"),
                List.of("Ouvrir la fiche du matériel"))));
  }

  private final Fakes.Suivi suivi = new Fakes.Suivi();
  private final Fakes.Besoins besoins = new Fakes.Besoins();
  private final Fakes.Achats achats = new Fakes.Achats();
  private final Fakes.Fournisseurs fournisseurs = new Fakes.Fournisseurs();
  private final Fakes.Llm llm = new Fakes.Llm();
  private final Fakes.Conversations conversations = new Fakes.Conversations();
  private boolean debitAutorise = true;
  private final ChatbotService service =
      new ChatbotService(
          suivi,
          besoins,
          achats,
          fournisseurs,
          llm,
          id -> debitAutorise,
          new HistoriqueService(conversations, parametres()),
          new SuggestionsService(parametres()),
          parametres(),
          Clock.fixed(T0, ZoneOffset.UTC));

  private ReponseChatbot demander(CurrentUser u, String question) {
    return service.traiter(new QuestionCommand(u, question, null));
  }

  @Test
  void should_answer_in_degraded_mode_with_sources_when_no_llm() {
    ReponseChatbot r =
        demander(Donnees.user(Roles.RESPONSABLE_MATERIEL), "Où se trouve la niveleuse ?");
    assertThat(r.intention()).isEqualTo(Intention.LOCALISER_ENGIN);
    assertThat(r.modeDegrade()).isTrue();
    assertThat(r.reponse()).contains("NIV-002").contains("Contournement de Thiès RN2");
    assertThat(r.sources()).extracting(Source::lien).containsExactly("/materiels/m1");
    assertThat(suivi.appels)
        .containsExactly(
            "materiels q=niveleuse categorie=MATERIEL_ROULANT etat=null chantier=null");
    assertThat(conversations.stock.get(r.conversationId()).messages()).hasSize(2);
    assertThat(r.horodatage()).isEqualTo(T0);
  }

  @Test
  void should_use_llm_with_delimited_context_when_configured() {
    llm.disponible = true;
    CurrentUser rm = Donnees.user(Roles.RESPONSABLE_MATERIEL);
    ReponseChatbot r = demander(rm, "Où se trouve la niveleuse ?");
    assertThat(r.modeDegrade()).isFalse();
    assertThat(r.reponse()).isEqualTo("Réponse du LLM : la niveleuse NIV-002 est à Thiès.");
    assertThat(llm.derniere.contexte()).contains("[MATERIEL m1]");
    assertThat(llm.derniere.profil()).isEqualTo(Roles.RESPONSABLE_MATERIEL);
    assertThat(llm.derniere.historique()).isEmpty();

    ReponseChatbot suite =
        service.traiter(new QuestionCommand(rm, "Et la pelle ?", r.conversationId()));
    assertThat(suite.conversationId()).isEqualTo(r.conversationId());
    assertThat(llm.derniere.historique()).hasSize(2);
  }

  @Test
  void should_fall_back_to_degraded_mode_when_llm_fails_or_returns_blank() {
    llm.disponible = true;
    llm.enEchec = true;
    assertThat(demander(Donnees.user(Roles.DAF), "Statut du contrat CTR-2026-003").modeDegrade())
        .isTrue();
    llm.enEchec = false;
    llm.reponse = "  \u0000 ";
    ReponseChatbot r = demander(Donnees.user(Roles.DAF), "Statut du contrat CTR-2026-003");
    assertThat(r.modeDegrade()).isTrue();
    assertThat(r.reponse()).contains("CTR-2026-003").contains("180 000 000 FCFA");
  }

  @Test
  void should_never_call_restricted_services_nor_show_amounts_for_chef() {
    ReponseChatbot r =
        demander(Donnees.chef(Donnees.CHANTIER_THIES), "Où en est le contrat CTR-2026-003 ?");
    assertThat(r.intention()).isEqualTo(Intention.STATUT_COMMANDE);
    assertThat(achats.appels).isEmpty();
    assertThat(r.reponse()).isEqualTo("Ces informations ne sont pas accessibles à votre profil.");
    assertThat(r.sources()).isEmpty();

    ReponseChatbot maintenance =
        demander(Donnees.chef(Donnees.CHANTIER_THIES), "Historique de maintenance de NIV-002");
    assertThat(maintenance.reponse()).contains("OT-2026-0007").doesNotContain("FCFA");
  }

  @Test
  void should_degrade_gracefully_when_a_service_is_down() {
    suivi.enPanne = true;
    ReponseChatbot r =
        demander(Donnees.user(Roles.RESPONSABLE_MATERIEL), "Quels compacteurs sont disponibles ?");
    assertThat(r.modeDegrade()).isTrue();
    assertThat(r.reponse()).contains("momentanément indisponible");
    assertThat(demander(Donnees.chef(Donnees.CHANTIER_THIES), "Planning de mon chantier").reponse())
        .contains("momentanément indisponible");
    assertThat(
            demander(Donnees.user(Roles.RESPONSABLE_MATERIEL), "Planning du chantier de Thiès")
                .reponse())
        .contains("momentanément indisponible");
  }

  @Test
  void should_fetch_context_according_to_intent() {
    CurrentUser dg = Donnees.user(Roles.DIRECTION_GENERALE);
    demander(dg, "Où en est mon besoin BES-2026-0012 ?");
    demander(dg, "Quels besoins sont en attente ?");
    assertThat(besoins.appels)
        .containsExactly("besoins q=BES-2026-0012 statut=null", "besoins q=null statut=EN_ATTENTE");

    demander(dg, "Avancement de l'appel d'offres AO-2026-001");
    demander(dg, "Les contrats en cours");
    assertThat(achats.appels).containsExactly("ao AO-2026-001", "contrats null", "ao null");

    ReponseChatbot f = demander(dg, "Meilleurs fournisseurs d'outillage");
    assertThat(fournisseurs.appels).containsExactly("classement OUTILLAGE");
    assertThat(f.reponse()).contains("Sahel Engins SA");

    demander(dg, "Historique des maintenances");
    demander(dg, "Historique de maintenance de PEL-001");
    demander(dg, "Quels matériels sont disponibles sur le chantier de Thiès ?");
    demander(dg, "Planning du chantier de Thiès");
    demander(dg, "Jusqu'à quand la niveleuse NIV-002 est-elle affectée ?");
    demander(dg, "Planning des affectations");
    assertThat(suivi.appels)
        .containsExactly(
            "maintenances null",
            "materiels q=PEL-001 categorie=null etat=null chantier=null",
            "maintenances m2",
            "chantiers Thiès",
            "materiels q=null categorie=null etat=DISPONIBLE chantier=" + Donnees.CHANTIER_THIES,
            "chantiers Thiès",
            "planning " + Donnees.CHANTIER_THIES + " 2026-10-30 2026-11-29",
            "materiels q=NIV-002 categorie=MATERIEL_ROULANT etat=null chantier=null",
            "affectations m1",
            "planning null 2026-10-30 2026-11-29");
  }

  @Test
  void should_restrict_chef_planning_to_his_chantiers() {
    UUID autre = UUID.fromString("c0000000-0000-0000-0000-000000000005");
    ReponseChatbot r = demander(Donnees.chef(autre), "Planning du chantier de Thiès");
    assertThat(suivi.appels).containsExactly("chantiers Thiès");
    assertThat(r.reponse()).contains("aucune affectation");
    demander(Donnees.chef(autre), "Planning des affectations");
    assertThat(suivi.appels).last().isEqualTo("planning " + autre + " 2026-10-30 2026-11-29");
  }

  @Test
  void should_answer_procedures_and_out_of_scope_questions_without_data_calls() {
    ReponseChatbot p = demander(Donnees.user(Roles.ADMIN_SYSTEME), "Comment signaler une panne ?");
    assertThat(p.intention()).isEqualTo(Intention.PROCEDURE);
    assertThat(p.reponse()).contains("1. Ouvrir la fiche du matériel");
    ReponseChatbot autre = demander(Donnees.chef(), "Quelle est la capitale du Japon ?");
    assertThat(autre.intention()).isEqualTo(Intention.AUTRE);
    assertThat(autre.reponse())
        .contains("- Quels engins sont affectés à mon chantier ?")
        .contains("- Comment exprimer un besoin ?");
    assertThat(suivi.appels).isEmpty();
  }

  @Test
  void should_reject_invalid_questions_rate_limit_and_foreign_conversations() {
    CurrentUser rm = Donnees.user(Roles.RESPONSABLE_MATERIEL);
    assertThatThrownBy(() -> demander(rm, " \u0007 ")).isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> demander(rm, "x".repeat(1001)))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("1000");
    assertThatThrownBy(() -> demander(Donnees.user(Roles.FOURNISSEUR), "Où est la pelle ?"))
        .isInstanceOf(ForbiddenOperationException.class);
    UUID conversationAutrui =
        demander(Donnees.user(Roles.DAF), "Contrats en cours").conversationId();
    assertThatThrownBy(
            () -> service.traiter(new QuestionCommand(rm, "Et ensuite ?", conversationAutrui)))
        .isInstanceOf(ResourceNotFoundException.class);
    debitAutorise = false;
    assertThatThrownBy(() -> demander(rm, "Où est la pelle ?"))
        .isInstanceOf(LimiteDebitDepasseeException.class)
        .satisfies(e -> assertThat(((LimiteDebitDepasseeException) e).status()).isEqualTo(429));
  }

  @Test
  void should_list_suggestions_by_role_without_duplicates() {
    SuggestionsService s = new SuggestionsService(parametres());
    assertThat(s.suggestions(Donnees.chef()))
        .containsExactly(
            "Quels engins sont affectés à mon chantier ?", "Comment exprimer un besoin ?");
    assertThat(s.suggestions(Donnees.user(Roles.DAF)))
        .containsExactly("Comment exprimer un besoin ?");
    assertThat(
            Set.copyOf(
                s.suggestions(Donnees.user(Roles.CHEF_CHANTIER, Roles.RESPONSABLE_MATERIEL))))
        .hasSize(2);
  }
}
