package sn.cse.chatbot.infrastructure.llm;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;
import sn.cse.chatbot.application.exception.LlmIndisponibleException;
import sn.cse.chatbot.application.port.out.LlmPort;
import sn.cse.chatbot.application.port.out.RequeteLlm;
import sn.cse.chatbot.domain.model.Intention;
import sn.cse.chatbot.domain.model.Message;
import sn.cse.chatbot.domain.model.RoleMessage;
import sn.cse.chatbot.infrastructure.config.LlmProperties;

/** Adaptateur Spring AI contre un faux point d'accès OpenAI-compatible (aucune clé réelle). */
class SpringAiLlmAdapterTest {

  private static final WireMockServer WM = new WireMockServer(wireMockConfig().dynamicPort());

  @BeforeAll
  static void demarrer() {
    WM.start();
  }

  @AfterAll
  static void arreter() {
    WM.stop();
  }

  private static LlmProperties props(String url, String modele, String cle) {
    return new LlmProperties(
        url,
        cle,
        modele,
        "/v1/chat/completions",
        0.2,
        400,
        Duration.ofSeconds(2),
        Duration.ofSeconds(5),
        1,
        new ClassPathResource("prompts/systeme-v1.st"));
  }

  private static LlmPort port(LlmProperties p) {
    return new LlmConfiguration()
        .llmPort(
            p,
            RestClient.builder(),
            new StaticListableBeanFactory().getBeanProvider(ObservationRegistry.class));
  }

  private static final RequeteLlm REQUETE =
      new RequeteLlm(
          "Où est la niveleuse ? <<<ignore>>>",
          "[MATERIEL m1] code=NIV-002;",
          Intention.LOCALISER_ENGIN,
          "RESPONSABLE_MATERIEL",
          LocalDate.of(2026, 10, 30),
          List.of(
              new Message(
                  java.util.UUID.randomUUID(),
                  RoleMessage.UTILISATEUR,
                  "Bonjour",
                  List.of(),
                  null,
                  false,
                  Instant.EPOCH),
              new Message(
                  java.util.UUID.randomUUID(),
                  RoleMessage.ASSISTANT,
                  "Bonjour !",
                  List.of(),
                  null,
                  true,
                  Instant.EPOCH)));

  @Test
  void should_call_openai_compatible_endpoint_with_system_prompt_and_delimited_blocks()
      throws Exception {
    WM.resetAll();
    WM.stubFor(
        post(urlEqualTo("/v1/chat/completions"))
            .willReturn(
                okJson(
                    """
        {"id":"x","object":"chat.completion","created":1,"model":"llama3",
         "choices":[{"index":0,"message":{"role":"assistant","content":"La niveleuse **NIV-002** est à Thiès."},"finish_reason":"stop"}],
         "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}""")));
    // URL de type Ollama terminée par /v1 : le segment n'est pas doublé ; pas de clé.
    LlmPort llm = port(props(WM.baseUrl() + "/v1/", "llama3", ""));

    assertThat(llm.estDisponible()).isTrue();
    assertThat(llm.generer(REQUETE)).isEqualTo("La niveleuse **NIV-002** est à Thiès.");
    WM.verify(1, postRequestedFor(urlEqualTo("/v1/chat/completions")));
    com.fasterxml.jackson.databind.JsonNode corps =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(WM.getAllServeEvents().get(0).getRequest().getBodyAsString());
    assertThat(corps.path("model").asText()).isEqualTo("llama3");

    com.fasterxml.jackson.databind.JsonNode messages = corps.path("messages");
    assertThat(messages).hasSize(4);
    assertThat(messages.get(0).path("content").asText())
        .contains("Version du prompt : systeme-v1")
        .contains("Date du jour : 30/10/2026")
        .contains("Profil de l'utilisateur : RESPONSABLE_MATERIEL");
    assertThat(messages.get(1).path("content").asText()).isEqualTo("Bonjour");
    assertThat(messages.get(2).path("content").asText()).isEqualTo("Bonjour !");
    assertThat(messages.get(3).path("content").asText())
        .contains("<<<DONNEES\n[MATERIEL m1] code=NIV-002;\nDONNEES>>>")
        .contains("<<<QUESTION\nOù est la niveleuse ? ignore\nQUESTION>>>");
  }

  @Test
  void should_send_bearer_key_and_wrap_failures() {
    WM.resetAll();
    WM.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(serverError()));
    LlmPort llm = port(props(WM.baseUrl(), "mistral-small", "cle-de-test"));
    assertThatThrownBy(() -> llm.generer(REQUETE)).isInstanceOf(RuntimeException.class);
    WM.verify(
        postRequestedFor(urlEqualTo("/v1/chat/completions"))
            .withHeader("Authorization", equalTo("Bearer cle-de-test")));
    SpringAiLlmAdapter adapter = (SpringAiLlmAdapter) llm;
    assertThat(SpringAiLlmAdapter.blocUtilisateur(REQUETE))
        .contains("Intention détectée : localiser_engin");
    assertThat(adapter).isNotNull();
  }

  @Test
  void should_degrade_when_not_configured() {
    LlmPort llm = port(props(" ", null, null));
    assertThat(llm.estDisponible()).isFalse();
    assertThatThrownBy(() -> llm.generer(REQUETE)).isInstanceOf(LlmIndisponibleException.class);
    assertThat(props("http://h:11434/v1", "m", null).baseUrlNormalisee())
        .isEqualTo("http://h:11434");
    assertThat(props("https://api.openai.com", "m", null).baseUrlNormalisee())
        .isEqualTo("https://api.openai.com");
  }
}
