package sn.cse.chatbot.infrastructure.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

/**
 * LLM OpenAI-compatible ({@code cse.chatbot.llm.*}) : OpenAI, Mistral ou Ollama ({@code /v1}). Sans
 * URL ni modèle, aucun client n'est créé et le service fonctionne en mode dégradé.
 *
 * @param baseUrl URL de base (ex. {@code https://api.openai.com}, {@code
 *     http://localhost:11434/v1})
 * @param apiKey clé d'API (variable d'environnement uniquement ; vide pour Ollama)
 * @param model nom du modèle
 * @param completionsPath chemin de l'API de complétion
 * @param temperature température
 * @param maxTokens jetons maximum de la réponse
 * @param delaiConnexion délai de connexion
 * @param delaiReponse délai de réponse
 * @param tentatives tentatives (1 = pas de nouvel essai : le mode dégradé prend le relais)
 * @param promptSysteme ressource du prompt système versionné
 */
@Validated
@ConfigurationProperties(prefix = "cse.chatbot.llm")
public record LlmProperties(
    String baseUrl,
    String apiKey,
    String model,
    @NotBlank String completionsPath,
    @DecimalMin("0.0") @DecimalMax("2.0") double temperature,
    @Min(16) int maxTokens,
    @NotNull Duration delaiConnexion,
    @NotNull Duration delaiReponse,
    @Min(1) int tentatives,
    @NotNull Resource promptSysteme) {

  private static final String SUFFIXE_V1 = "/v1";

  /**
   * @return vrai si une URL et un modèle sont renseignés
   */
  public boolean estConfigure() {
    return baseUrl != null && !baseUrl.isBlank() && model != null && !model.isBlank();
  }

  /**
   * Accepte les URL terminées par {@code /v1} (usage courant avec Ollama) sans doubler le segment.
   *
   * @return l'URL de base normalisée
   */
  public String baseUrlNormalisee() {
    String url = baseUrl.trim();
    while (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    return url.endsWith(SUFFIXE_V1) && completionsPath.startsWith(SUFFIXE_V1 + "/")
        ? url.substring(0, url.length() - SUFFIXE_V1.length())
        : url;
  }
}
