package sn.cse.chatbot.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import sn.cse.chatbot.domain.model.Section;
import sn.cse.chatbot.domain.model.TypeSource;
import sn.cse.common.security.Roles;

/**
 * Configuration du chatbot ({@code cse.chatbot.*}), validée au démarrage.
 *
 * @param questionMaxCaracteres longueur maximale d'une question
 * @param contexteMaxCaracteres longueur maximale du contexte envoyé au LLM
 * @param reponseMaxCaracteres longueur maximale d'une réponse
 * @param resultatsMax éléments demandés à chaque service
 * @param sourcesMax sources citées au maximum
 * @param horizonPlanningJours horizon par défaut du planning
 * @param messagesHistorique messages précédents transmis au LLM
 * @param longueurTitre longueur du titre de conversation
 * @param longueurApercu longueur de l'aperçu du dernier message
 * @param suggestionsMax suggestions au maximum
 * @param limiteDebit limitation par utilisateur
 * @param services URL des services interrogés
 * @param liens modèles de liens frontend par type de source
 * @param acces rôles autorisés par section (section absente = tous les rôles internes)
 * @param rolesMontants rôles autorisés à voir montants et coûts
 * @param suggestions questions types par rôle (clé DEFAUT pour tous)
 * @param procedures base de procédures
 */
@Validated
@ConfigurationProperties(prefix = "cse.chatbot")
public record ChatbotProperties(
    @Min(10) @Max(10000) int questionMaxCaracteres,
    @Min(500) int contexteMaxCaracteres,
    @Min(200) int reponseMaxCaracteres,
    @Min(1) @Max(50) int resultatsMax,
    @Min(1) int sourcesMax,
    @Min(1) @Max(366) int horizonPlanningJours,
    @Min(0) int messagesHistorique,
    @Min(10) int longueurTitre,
    @Min(10) int longueurApercu,
    @Min(1) int suggestionsMax,
    @NotNull @Valid LimiteDebit limiteDebit,
    @NotNull @Valid Services services,
    @NotEmpty Map<TypeSource, String> liens,
    Map<Section, Set<String>> acces,
    Set<String> rolesMontants,
    Map<String, List<String>> suggestions,
    List<@Valid ProcedureConfig> procedures) {

  public ChatbotProperties {
    acces = acces == null ? Map.of() : Map.copyOf(acces);
    rolesMontants = rolesMontants == null ? Set.of() : Set.copyOf(rolesMontants);
    suggestions = suggestions == null ? Map.of() : Map.copyOf(suggestions);
    procedures = procedures == null ? List.of() : List.copyOf(procedures);
  }

  /**
   * @return vrai si tous les rôles cités sont des rôles internes
   */
  @AssertTrue(message = "rôle inconnu dans cse.chatbot.acces ou cse.chatbot.roles-montants")
  public boolean isRolesConnus() {
    return Roles.INTERNES.containsAll(rolesMontants)
        && acces.values().stream().allMatch(Roles.INTERNES::containsAll);
  }

  /**
   * Seau de jetons par utilisateur.
   *
   * @param capacite questions autorisées par période
   * @param periode période de recharge complète
   */
  public record LimiteDebit(@Min(1) int capacite, @NotNull Duration periode) {}

  /**
   * URL de base des services (DNS Docker ou localhost).
   *
   * @param suivi ms-suivi
   * @param besoins ms-besoins
   * @param achats ms-achats
   * @param fournisseurs ms-fournisseurs
   */
  public record Services(
      @NotBlank String suivi,
      @NotBlank String besoins,
      @NotBlank String achats,
      @NotBlank String fournisseurs) {}

  /**
   * Procédure documentée.
   *
   * @param sujet titre
   * @param motsCles mots-clés
   * @param etapes étapes
   */
  public record ProcedureConfig(
      @NotBlank String sujet, @NotEmpty List<String> motsCles, @NotEmpty List<String> etapes) {}
}
