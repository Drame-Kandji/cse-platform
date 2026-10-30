package sn.cse.gateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration de la passerelle ({@code cse.gateway.*}) : services routés, flux SSE, limitation de
 * débit, coupe-circuits, CORS, sonde de santé.
 *
 * @param services services routés, dans l'ordre de déclaration (nom → URL et chemins)
 * @param sse route de flux Server-Sent Events (sans coupe-circuit, délai long)
 * @param limiteDebit limitation de débit Redis
 * @param coupeCircuit coupe-circuits Resilience4j
 * @param cors origines autorisées
 * @param sonde sonde de santé des services ({@code /api/plateforme/etat})
 * @param securite règles d'accès spécifiques
 */
@Validated
@ConfigurationProperties(prefix = "cse.gateway")
public record GatewayProperties(
    @NotEmpty Map<String, @Valid Service> services,
    @NotNull @Valid Sse sse,
    @NotNull @Valid LimiteDebit limiteDebit,
    @NotNull @Valid CoupeCircuit coupeCircuit,
    @NotNull @Valid Cors cors,
    @NotNull @Valid Sonde sonde,
    @NotNull @Valid Securite securite) {

  /**
   * @param url URL de base du service ({@code MS_<NOM>_URL})
   * @param chemins motifs de chemins routés vers ce service
   * @param documentation expose {@code /v3/api-docs/<service>} dans le Swagger agrégé
   */
  public record Service(@NotNull URI url, @NotEmpty List<String> chemins, Boolean documentation) {

    public Service {
      chemins = chemins == null ? List.of() : List.copyOf(chemins);
      documentation = documentation == null || documentation;
    }
  }

  /**
   * @param service service émetteur du flux
   * @param chemins chemins du flux
   * @param delai délai maximal sans donnée avant coupure (le serveur envoie un {@code ping})
   */
  public record Sse(
      @NotBlank String service, @NotEmpty List<String> chemins, @NotNull Duration delai) {}

  /**
   * @param actif active la limitation (désactivée dans les tests sans Redis)
   * @param debit jetons rechargés par seconde
   * @param rafale capacité du seau (rafale maximale)
   * @param jetonsParRequete coût d'une requête
   */
  public record LimiteDebit(
      boolean actif, @Min(1) int debit, @Min(1) int rafale, @Min(1) int jetonsParRequete) {}

  /**
   * @param actif active les coupe-circuits
   * @param delai délai maximal d'une réponse avant repli
   * @param fenetre nombre d'appels de la fenêtre glissante
   * @param seuilEchec pourcentage d'échecs ouvrant le circuit
   * @param attenteOuvert durée d'ouverture avant essai
   */
  public record CoupeCircuit(
      boolean actif,
      @NotNull Duration delai,
      @Min(1) int fenetre,
      @Min(1) @Max(100) int seuilEchec,
      @NotNull Duration attenteOuvert) {}

  /**
   * @param originesAutorisees origines exactes (jamais {@code *} : les identifiants sont autorisés)
   * @param maxAge durée de cache des pré-vérifications
   */
  public record Cors(@NotEmpty List<String> originesAutorisees, @NotNull Duration maxAge) {

    public Cors {
      originesAutorisees = originesAutorisees == null ? List.of() : List.copyOf(originesAutorisees);
      if (originesAutorisees.stream().anyMatch(o -> o.contains("*"))) {
        throw new IllegalArgumentException(
            "cse.gateway.cors.origines-autorisees : joker interdit avec allowCredentials");
      }
    }
  }

  /**
   * @param chemin chemin de santé interrogé sur chaque service
   * @param delai délai maximal de réponse
   */
  public record Sonde(@NotBlank String chemin, @NotNull Duration delai) {}

  /**
   * @param portailSansRoleFournisseur chemins du portail accessibles à tout utilisateur authentifié
   *     (par défaut aucun : {@code /api/portail/**} exige le rôle FOURNISSEUR)
   */
  public record Securite(List<String> portailSansRoleFournisseur) {

    public Securite {
      portailSansRoleFournisseur =
          portailSansRoleFournisseur == null ? List.of() : List.copyOf(portailSansRoleFournisseur);
    }
  }
}
