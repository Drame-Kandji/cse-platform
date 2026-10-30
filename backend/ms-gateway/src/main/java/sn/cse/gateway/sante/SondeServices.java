package sn.cse.gateway.sante;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sn.cse.gateway.api.dto.EtatPlateforme;
import sn.cse.gateway.api.dto.EtatService;
import sn.cse.gateway.api.dto.EtatService.Statut;
import sn.cse.gateway.config.GatewayProperties;

/** Interroge en parallèle le point de santé de chaque service, avec un délai court. */
@Component
public class SondeServices {

  private static final String UP = "UP";

  private final WebClient client;
  private final GatewayProperties properties;

  /**
   * @param builder builder WebClient de Spring Boot
   * @param properties configuration
   */
  public SondeServices(WebClient.Builder builder, GatewayProperties properties) {
    this.client = builder.build();
    this.properties = properties;
  }

  /**
   * @return l'état de tous les services (ordre de configuration conservé)
   */
  public Mono<EtatPlateforme> sonder() {
    return Flux.fromIterable(properties.services().entrySet())
        .flatMapSequential(this::sonder)
        .collectList()
        .map(EtatPlateforme::new);
  }

  private Mono<EtatService> sonder(Map.Entry<String, GatewayProperties.Service> service) {
    URI cible = service.getValue().url().resolve(properties.sonde().chemin());
    return Mono.defer(
        () -> {
          long debut = System.nanoTime();
          return client
              .get()
              .uri(cible)
              .retrieve()
              .bodyToMono(JsonNode.class)
              .map(corps -> UP.equals(corps.path("status").asText()) ? Statut.UP : Statut.DOWN)
              .timeout(properties.sonde().delai())
              .onErrorReturn(Statut.DOWN)
              .defaultIfEmpty(Statut.DOWN)
              .map(
                  statut ->
                      new EtatService(
                          service.getKey(), statut, (System.nanoTime() - debut) / 1_000_000));
        });
  }
}
