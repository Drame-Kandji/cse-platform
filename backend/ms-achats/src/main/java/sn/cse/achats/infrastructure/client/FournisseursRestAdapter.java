package sn.cse.achats.infrastructure.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import sn.cse.achats.application.port.out.FournisseurAnnuaire;
import sn.cse.achats.application.port.out.FournisseurProjectionRepository;
import sn.cse.achats.domain.model.FournisseurConnu;
import sn.cse.achats.domain.model.StatutFournisseur;
import sn.cse.achats.infrastructure.config.AchatsProperties;
import sn.cse.common.client.ServiceRestClientFactory;
import sn.cse.common.exception.ServiceUnavailableException;

/**
 * Vérifie un fournisseur via {@code GET /api/fournisseurs/{id}} sur ms-fournisseurs (JWT de
 * l'utilisateur relayé), protégé par Resilience4j (retry + circuit breaker {@value #INSTANCE}). En
 * cas d'indisponibilité, repli explicite sur la projection locale alimentée par Kafka. Chaque
 * réponse fraîche met à jour cette projection.
 */
@Component
public class FournisseursRestAdapter implements FournisseurAnnuaire {

  /** Nom des instances Resilience4j (configuration {@code resilience4j.*.instances}). */
  public static final String INSTANCE = "fournisseurs";

  private static final Logger LOG = LoggerFactory.getLogger(FournisseursRestAdapter.class);

  private final RestClient client;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;
  private final FournisseurProjectionRepository projection;
  private final Clock clock;

  /**
   * @param fabrique fabrique de clients inter-services
   * @param properties URL de ms-fournisseurs
   * @param circuitBreakers registre Resilience4j
   * @param retries registre Resilience4j
   * @param projection projection locale (repli)
   * @param clock horloge
   */
  @Autowired
  public FournisseursRestAdapter(
      ServiceRestClientFactory fabrique,
      AchatsProperties properties,
      CircuitBreakerRegistry circuitBreakers,
      RetryRegistry retries,
      FournisseurProjectionRepository projection,
      Clock clock) {
    this(
        fabrique.creer(properties.fournisseurs().baseUrl()),
        circuitBreakers.circuitBreaker(INSTANCE),
        retries.retry(INSTANCE),
        projection,
        clock);
  }

  /**
   * @param client client HTTP configuré
   * @param circuitBreaker disjoncteur
   * @param retry politique de rejeu
   * @param projection projection locale
   * @param clock horloge
   */
  public FournisseursRestAdapter(
      RestClient client,
      CircuitBreaker circuitBreaker,
      Retry retry,
      FournisseurProjectionRepository projection,
      Clock clock) {
    this.client = client;
    this.circuitBreaker = circuitBreaker;
    this.retry = retry;
    this.projection = projection;
    this.clock = clock;
  }

  @Override
  public Optional<FournisseurConnu> verifier(UUID id) {
    Supplier<Optional<FournisseurConnu>> appel =
        Retry.decorateSupplier(
            retry, CircuitBreaker.decorateSupplier(circuitBreaker, () -> appelDistant(id)));
    try {
      Optional<FournisseurConnu> trouve = appel.get();
      trouve.ifPresent(f -> projection.enregistrerSiPlusRecent(f, Instant.now(clock)));
      return trouve;
    } catch (CallNotPermittedException | RestClientException | IllegalArgumentException e) {
      LOG.warn(
          "ms-fournisseurs indisponible ({}), repli sur la projection locale pour {}",
          e.getClass().getSimpleName(),
          id);
      // Repli explicite : statut connu localement, sinon impossible de vérifier R14 (503).
      return Optional.of(
          projection
              .parId(id)
              .orElseThrow(
                  () ->
                      new ServiceUnavailableException(
                          "FOURNISSEURS_INDISPONIBLE",
                          "ms-fournisseurs est indisponible et le fournisseur "
                              + id
                              + " est inconnu localement : vérification impossible.")));
    }
  }

  private Optional<FournisseurConnu> appelDistant(UUID id) {
    try {
      FournisseurDistant reponse =
          client.get().uri("/api/fournisseurs/{id}", id).retrieve().body(FournisseurDistant.class);
      if (reponse == null || reponse.statut() == null) {
        throw new IllegalArgumentException("Réponse de ms-fournisseurs incomplète");
      }
      return Optional.of(
          new FournisseurConnu(
              id, reponse.raisonSociale(), StatutFournisseur.valueOf(reponse.statut())));
    } catch (HttpClientErrorException.NotFound e) {
      // Fournisseur inexistant : réponse métier, ni échec technique ni repli.
      return Optional.empty();
    }
  }
}
