package sn.cse.chatbot.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sn.cse.chatbot.application.port.out.AchatsPort;
import sn.cse.chatbot.domain.model.AppelOffreInfo;
import sn.cse.chatbot.domain.model.ContratInfo;
import sn.cse.chatbot.infrastructure.config.ChatbotProperties;
import sn.cse.common.client.ServiceRestClientFactory;

/** Adaptateur lecture seule de ms-achats. */
@Component
public class AchatsRestClient implements AchatsPort {

  static final String SERVICE = "achats";

  private final RestClient http;

  /**
   * @param factory fabrique de clients
   * @param properties URL du service
   */
  public AchatsRestClient(ServiceRestClientFactory factory, ChatbotProperties properties) {
    this.http = factory.creer(properties.services().achats());
  }

  @Override
  @Retry(name = SERVICE)
  @CircuitBreaker(name = SERVICE, fallbackMethod = "contratsIndisponibles")
  public List<ContratInfo> rechercherContrats(String q, int limite) {
    return Appels.lire(
        SERVICE,
        () ->
            http
                .get()
                .uri(
                    u ->
                        u.path("/api/contrats")
                            .queryParamIfPresent("q", Optional.ofNullable(q))
                            .queryParam("size", limite)
                            .build())
                .retrieve()
                .body(new ParameterizedTypeReference<ReponsesJson.Page<ReponsesJson.Contrat>>() {})
                .elements()
                .stream()
                .map(
                    k ->
                        new ContratInfo(
                            k.id(),
                            k.numero(),
                            k.objet(),
                            k.fournisseurNom(),
                            k.statut(),
                            k.montant(),
                            k.dateLivraisonPrevue(),
                            k.dateLivraisonReelle(),
                            k.chantierNom()))
                .toList());
  }

  @Override
  @Retry(name = SERVICE)
  @CircuitBreaker(name = SERVICE, fallbackMethod = "aoIndisponibles")
  public List<AppelOffreInfo> rechercherAppelsOffres(String q, int limite) {
    return Appels.lire(
        SERVICE,
        () ->
            http
                .get()
                .uri(
                    u ->
                        u.path("/api/appels-offres")
                            .queryParamIfPresent("q", Optional.ofNullable(q))
                            .queryParam("size", limite)
                            .build())
                .retrieve()
                .body(
                    new ParameterizedTypeReference<ReponsesJson.Page<ReponsesJson.AppelOffre>>() {})
                .elements()
                .stream()
                .map(
                    a ->
                        new AppelOffreInfo(
                            a.id(),
                            a.reference(),
                            a.objet(),
                            a.statut(),
                            a.dateLimite(),
                            a.nombreOffres() == null ? 0 : a.nombreOffres(),
                            a.montantAttribue()))
                .toList());
  }

  List<ContratInfo> contratsIndisponibles(String q, int limite, Throwable t) {
    throw Appels.indisponible(SERVICE, t);
  }

  List<AppelOffreInfo> aoIndisponibles(String q, int limite, Throwable t) {
    throw Appels.indisponible(SERVICE, t);
  }
}
