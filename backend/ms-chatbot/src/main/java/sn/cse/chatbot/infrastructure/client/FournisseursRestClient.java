package sn.cse.chatbot.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sn.cse.chatbot.application.port.out.FournisseursPort;
import sn.cse.chatbot.domain.model.FournisseurInfo;
import sn.cse.chatbot.infrastructure.config.ChatbotProperties;
import sn.cse.common.client.ServiceRestClientFactory;

/** Adaptateur lecture seule de ms-fournisseurs. */
@Component
public class FournisseursRestClient implements FournisseursPort {

  static final String SERVICE = "fournisseurs";

  private final RestClient http;

  /**
   * @param factory fabrique de clients
   * @param properties URL du service
   */
  public FournisseursRestClient(ServiceRestClientFactory factory, ChatbotProperties properties) {
    this.http = factory.creer(properties.services().fournisseurs());
  }

  @Override
  @Retry(name = SERVICE)
  @CircuitBreaker(name = SERVICE, fallbackMethod = "indisponible")
  public List<FournisseurInfo> classement(String categorie, int limite) {
    return Appels.lire(
        SERVICE,
        () -> {
          ReponsesJson.Classement[] r =
              http.get()
                  .uri(
                      u ->
                          u.path("/api/fournisseurs/classement")
                              .queryParamIfPresent("categorie", Optional.ofNullable(categorie))
                              .build())
                  .retrieve()
                  .body(ReponsesJson.Classement[].class);
          return r == null
              ? List.of()
              : Arrays.stream(r)
                  .limit(limite)
                  .map(
                      c ->
                          new FournisseurInfo(
                              c.fournisseurId(),
                              c.rang() == null ? 0 : c.rang(),
                              c.raisonSociale(),
                              c.scorePerformance(),
                              c.tauxLivraisonALHeure(),
                              c.nombreLitiges() == null ? 0 : c.nombreLitiges(),
                              c.specialites()))
                  .toList();
        });
  }

  List<FournisseurInfo> indisponible(String categorie, int limite, Throwable t) {
    throw Appels.indisponible(SERVICE, t);
  }
}
