package sn.cse.chatbot.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sn.cse.chatbot.application.port.out.BesoinsPort;
import sn.cse.chatbot.domain.model.BesoinInfo;
import sn.cse.chatbot.infrastructure.config.ChatbotProperties;
import sn.cse.common.client.ServiceRestClientFactory;

/** Adaptateur lecture seule de ms-besoins. */
@Component
public class BesoinsRestClient implements BesoinsPort {

  static final String SERVICE = "besoins";

  private final RestClient http;

  /**
   * @param factory fabrique de clients
   * @param properties URL du service
   */
  public BesoinsRestClient(ServiceRestClientFactory factory, ChatbotProperties properties) {
    this.http = factory.creer(properties.services().besoins());
  }

  @Override
  @Retry(name = SERVICE)
  @CircuitBreaker(name = SERVICE, fallbackMethod = "indisponible")
  public List<BesoinInfo> rechercher(String q, String statut, int limite) {
    return Appels.lire(
        SERVICE,
        () ->
            http
                .get()
                .uri(
                    u ->
                        u.path("/api/besoins")
                            .queryParamIfPresent("q", Optional.ofNullable(q))
                            .queryParamIfPresent("statut", Optional.ofNullable(statut))
                            .queryParam("size", limite)
                            .build())
                .retrieve()
                .body(new ParameterizedTypeReference<ReponsesJson.Page<ReponsesJson.Besoin>>() {})
                .elements()
                .stream()
                .map(
                    b ->
                        new BesoinInfo(
                            b.id(),
                            b.reference(),
                            b.designation(),
                            b.quantite() == null ? 0 : b.quantite(),
                            b.statut(),
                            b.priorite(),
                            b.chantierNom(),
                            b.dateBesoin(),
                            b.motifRejet(),
                            b.validateurNom()))
                .toList());
  }

  List<BesoinInfo> indisponible(String q, String statut, int limite, Throwable t) {
    throw Appels.indisponible(SERVICE, t);
  }
}
