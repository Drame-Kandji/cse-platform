package sn.cse.chatbot.infrastructure.llm;

import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import sn.cse.chatbot.application.port.out.LlmPort;
import sn.cse.chatbot.infrastructure.config.LlmProperties;

/**
 * Création explicite du client Spring AI (pas d'auto-configuration OpenAI : le démarrage ne dépend
 * jamais de la présence d'une clé). Sans configuration, un adaptateur « non configuré » force le
 * mode dégradé.
 */
@Configuration(proxyBeanMethods = false)
public class LlmConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(LlmConfiguration.class);

  /**
   * @param properties configuration du LLM
   * @param restClientBuilder builder HTTP de Spring Boot
   * @param observations observabilité
   * @return l'adaptateur LLM
   */
  @Bean
  public LlmPort llmPort(
      LlmProperties properties,
      RestClient.Builder restClientBuilder,
      ObjectProvider<ObservationRegistry> observations) {
    String prompt = lirePrompt(properties);
    if (!properties.estConfigure()) {
      LOG.info(
          "Aucun LLM configuré (LLM_BASE_URL / LLM_MODEL) : l'assistant fonctionne en mode dégradé");
      return new LlmNonConfigure();
    }
    RestClient.Builder http =
        restClientBuilder
            .clone()
            .requestFactory(
                // HTTP/1.1 explicite : la mise à niveau h2c du client JDK sur HTTP en clair (Ollama
                // local) est mal supportée par certains serveurs ; HTTPS négocie toujours via ALPN.
                ClientHttpRequestFactoryBuilder.jdk()
                    .withHttpClientCustomizer(b -> b.version(HttpClient.Version.HTTP_1_1))
                    .build(
                        ClientHttpRequestFactorySettings.defaults()
                            .withConnectTimeout(properties.delaiConnexion())
                            .withReadTimeout(properties.delaiReponse())));
    OpenAiApi api =
        OpenAiApi.builder()
            .baseUrl(properties.baseUrlNormalisee())
            .completionsPath(properties.completionsPath())
            .apiKey(
                properties.apiKey() == null || properties.apiKey().isBlank()
                    ? new NoopApiKey()
                    : new SimpleApiKey(properties.apiKey()))
            .restClientBuilder(http)
            .build();
    OpenAiChatModel modele =
        OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .model(properties.model())
                    .temperature(properties.temperature())
                    .maxTokens(properties.maxTokens())
                    .build())
            .retryTemplate(RetryTemplate.builder().maxAttempts(properties.tentatives()).build())
            .observationRegistry(observations.getIfAvailable(() -> ObservationRegistry.NOOP))
            .build();
    LOG.info(
        "LLM configuré : modèle {} sur {}", properties.model(), properties.baseUrlNormalisee());
    return new SpringAiLlmAdapter(ChatClient.builder(modele).build(), prompt);
  }

  private static String lirePrompt(LlmProperties properties) {
    try {
      return properties.promptSysteme().getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Prompt système introuvable : " + properties.promptSysteme(), e);
    }
  }
}
