package sn.cse.chatbot.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sn.cse.chatbot.application.port.out.CritereMateriel;
import sn.cse.chatbot.application.port.out.SuiviPort;
import sn.cse.chatbot.domain.model.AffectationInfo;
import sn.cse.chatbot.domain.model.ChantierInfo;
import sn.cse.chatbot.domain.model.MaintenanceInfo;
import sn.cse.chatbot.domain.model.MaterielInfo;
import sn.cse.chatbot.infrastructure.config.ChatbotProperties;
import sn.cse.common.client.ServiceRestClientFactory;

/** Adaptateur lecture seule de ms-suivi (JWT de l'utilisateur relayé). */
@Component
public class SuiviRestClient implements SuiviPort {

  static final String SERVICE = "suivi";

  private final RestClient http;

  /**
   * @param factory fabrique de clients (relais JWT + corrélation)
   * @param properties URL du service
   */
  public SuiviRestClient(ServiceRestClientFactory factory, ChatbotProperties properties) {
    this.http = factory.creer(properties.services().suivi());
  }

  @Override
  @Retry(name = SERVICE)
  @CircuitBreaker(name = SERVICE, fallbackMethod = "materielsIndisponibles")
  public List<MaterielInfo> rechercherMateriels(CritereMateriel c, int limite) {
    return Appels.lire(
        SERVICE,
        () ->
            http
                .get()
                .uri(
                    u ->
                        u.path("/api/materiels")
                            .queryParamIfPresent("q", Optional.ofNullable(c.q()))
                            .queryParamIfPresent("categorie", Optional.ofNullable(c.categorie()))
                            .queryParamIfPresent("etat", Optional.ofNullable(c.etat()))
                            .queryParamIfPresent("chantierId", Optional.ofNullable(c.chantierId()))
                            .queryParam("size", limite)
                            .build())
                .retrieve()
                .body(new ParameterizedTypeReference<ReponsesJson.Page<ReponsesJson.Materiel>>() {})
                .elements()
                .stream()
                .map(SuiviRestClient::materiel)
                .toList());
  }

  @Override
  @Retry(name = SERVICE)
  @CircuitBreaker(name = SERVICE, fallbackMethod = "maintenancesIndisponibles")
  public List<MaintenanceInfo> maintenances(String materielId, int limite) {
    return Appels.lire(
        SERVICE,
        () ->
            http
                .get()
                .uri(
                    u ->
                        u.path("/api/maintenances")
                            .queryParamIfPresent("materielId", Optional.ofNullable(materielId))
                            .queryParam("size", limite)
                            .build())
                .retrieve()
                .body(
                    new ParameterizedTypeReference<
                        ReponsesJson.Page<ReponsesJson.Maintenance>>() {})
                .elements()
                .stream()
                .map(
                    m ->
                        new MaintenanceInfo(
                            m.id(),
                            m.numeroOt(),
                            m.materielId(),
                            m.materielCode(),
                            m.materielDesignation(),
                            m.type(),
                            m.statut(),
                            m.datePlanifiee(),
                            m.dateFin(),
                            m.description(),
                            m.cout()))
                .toList());
  }

  @Override
  @Retry(name = SERVICE)
  @CircuitBreaker(name = SERVICE, fallbackMethod = "planningIndisponible")
  public List<AffectationInfo> planning(String chantierId, LocalDate du, LocalDate au, int limite) {
    return Appels.lire(
        SERVICE,
        () -> {
          ReponsesJson.Affectation[] r =
              http.get()
                  .uri(
                      u ->
                          u.path("/api/affectations/planning")
                              .queryParamIfPresent("chantierId", Optional.ofNullable(chantierId))
                              .queryParam("du", du)
                              .queryParam("au", au)
                              .build())
                  .retrieve()
                  .body(ReponsesJson.Affectation[].class);
          return r == null
              ? List.of()
              : Arrays.stream(r).limit(limite).map(SuiviRestClient::affectation).toList();
        });
  }

  @Override
  @Retry(name = SERVICE)
  @CircuitBreaker(name = SERVICE, fallbackMethod = "affectationsIndisponibles")
  public List<AffectationInfo> affectationsDuMateriel(String materielId, int limite) {
    return Appels.lire(
        SERVICE,
        () ->
            http
                .get()
                .uri(
                    u ->
                        u.path("/api/affectations")
                            .queryParam("materielId", materielId)
                            .queryParam("size", limite)
                            .build())
                .retrieve()
                .body(
                    new ParameterizedTypeReference<
                        ReponsesJson.Page<ReponsesJson.Affectation>>() {})
                .elements()
                .stream()
                .map(SuiviRestClient::affectation)
                .toList());
  }

  @Override
  @Retry(name = SERVICE)
  @CircuitBreaker(name = SERVICE, fallbackMethod = "chantiersIndisponibles")
  public List<ChantierInfo> rechercherChantiers(String q, int limite) {
    return Appels.lire(
        SERVICE,
        () ->
            http
                .get()
                .uri(
                    u ->
                        u.path("/api/chantiers")
                            .queryParam("q", q)
                            .queryParam("size", limite)
                            .build())
                .retrieve()
                .body(new ParameterizedTypeReference<ReponsesJson.Page<ReponsesJson.Chantier>>() {})
                .elements()
                .stream()
                .map(
                    ch ->
                        new ChantierInfo(
                            ch.id(), ch.code(), ch.nom(), ch.localisation(), ch.statut()))
                .toList());
  }

  static MaterielInfo materiel(ReponsesJson.Materiel m) {
    return new MaterielInfo(
        m.id(),
        m.code(),
        m.designation(),
        m.categorie(),
        m.etat(),
        m.chantierActuelNom(),
        m.chantierActuelId(),
        m.latitude(),
        m.longitude(),
        m.prochaineMaintenance(),
        Boolean.TRUE.equals(m.maintenanceDue()));
  }

  static AffectationInfo affectation(ReponsesJson.Affectation a) {
    return new AffectationInfo(
        a.id(),
        a.materielId(),
        a.materielCode(),
        a.materielDesignation(),
        a.chantierId(),
        a.chantierNom(),
        a.dateDebut(),
        a.dateFinPrevue(),
        a.statut());
  }

  List<MaterielInfo> materielsIndisponibles(CritereMateriel c, int limite, Throwable t) {
    throw Appels.indisponible(SERVICE, t);
  }

  List<MaintenanceInfo> maintenancesIndisponibles(String materielId, int limite, Throwable t) {
    throw Appels.indisponible(SERVICE, t);
  }

  List<AffectationInfo> planningIndisponible(
      String chantierId, LocalDate du, LocalDate au, int limite, Throwable t) {
    throw Appels.indisponible(SERVICE, t);
  }

  List<AffectationInfo> affectationsIndisponibles(String materielId, int limite, Throwable t) {
    throw Appels.indisponible(SERVICE, t);
  }

  List<ChantierInfo> chantiersIndisponibles(String q, int limite, Throwable t) {
    throw Appels.indisponible(SERVICE, t);
  }
}
