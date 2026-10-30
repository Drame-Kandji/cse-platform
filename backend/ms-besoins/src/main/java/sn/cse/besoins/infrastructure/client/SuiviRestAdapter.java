package sn.cse.besoins.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sn.cse.besoins.application.exception.ServiceSuiviIndisponibleException;
import sn.cse.besoins.application.port.out.AffectationMaterielPort;
import sn.cse.besoins.application.port.out.ChantierPort;
import sn.cse.besoins.application.port.out.DisponibiliteMaterielPort;
import sn.cse.besoins.infrastructure.config.BesoinsProperties;
import sn.cse.common.client.ServiceRestClientFactory;
import sn.cse.common.exception.CseException;
import sn.cse.common.exception.ForbiddenOperationException;

/**
 * Adaptateur REST vers ms-suivi : chantiers, disponibilité et affectation du parc. Le JWT de
 * l'utilisateur et l'identifiant de corrélation sont relayés ({@link ServiceRestClientFactory}) ;
 * délais bornés par {@code cse.clients.*}, rejeu avec attente exponentielle et disjoncteur par
 * Resilience4j (instance {@value #INSTANCE}). Les refus métier (4xx) ne sont ni rejoués ni comptés
 * comme pannes ; toute panne technique se traduit par {@link ServiceSuiviIndisponibleException}.
 */
@Component
public class SuiviRestAdapter
    implements ChantierPort, DisponibiliteMaterielPort, AffectationMaterielPort {

  /** Nom de l'instance Resilience4j (retry et circuit breaker). */
  public static final String INSTANCE = "suivi";

  static final String ACCES_SUIVI_REFUSE = "ACCES_SUIVI_REFUSE";

  private static final Logger LOG = LoggerFactory.getLogger(SuiviRestAdapter.class);

  private final RestClient client;

  /**
   * @param factory fabrique de clients inter-services
   * @param properties configuration (URL de ms-suivi)
   */
  public SuiviRestAdapter(ServiceRestClientFactory factory, BesoinsProperties properties) {
    this.client = factory.creer(properties.suivi().baseUrl());
  }

  @Override
  @Retry(name = INSTANCE, fallbackMethod = "trouverRepli")
  @CircuitBreaker(name = INSTANCE)
  public Optional<ChantierInfo> trouver(UUID chantierId) {
    return client
        .get()
        .uri("/api/chantiers/{id}", chantierId)
        .exchange(
            (requete, reponse) -> {
              if (reponse.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return Optional.<ChantierInfo>empty();
              }
              exigerSucces(reponse.getStatusCode(), "consultation du chantier");
              SuiviDtos.Chantier c = reponse.bodyTo(SuiviDtos.Chantier.class);
              return Optional.ofNullable(c)
                  .map(
                      ch ->
                          new ChantierInfo(
                              ch.id(), ch.nom(), SuiviDtos.CHANTIER_EN_COURS.equals(ch.statut())));
            });
  }

  @Override
  @Retry(name = INSTANCE, fallbackMethod = "verifierRepli")
  @CircuitBreaker(name = INSTANCE)
  public Disponibilite verifier(DemandeDisponibilite demande) {
    SuiviDtos.Disponibilite d =
        client
            .get()
            .uri(
                b ->
                    b.path("/api/materiels/disponibilite")
                        .queryParam("categorie", "{categorie}")
                        .queryParam("designation", "{designation}")
                        .queryParam("quantite", "{quantite}")
                        .queryParam("du", "{du}")
                        .queryParam("au", "{au}")
                        .build(
                            Map.of(
                                "categorie", demande.categorie().name(),
                                "designation", demande.designation(),
                                "quantite", demande.quantite(),
                                "du", demande.du(),
                                "au", demande.au())))
            .exchange(
                (requete, reponse) -> {
                  exigerSucces(reponse.getStatusCode(), "disponibilité du parc");
                  return reponse.bodyTo(SuiviDtos.Disponibilite.class);
                });
    if (d == null) {
      return new Disponibilite(0, List.of());
    }
    List<UUID> ids =
        d.materiels() == null
            ? List.of()
            : d.materiels().stream().map(SuiviDtos.Materiel::id).filter(Objects::nonNull).toList();
    return new Disponibilite(d.quantiteDisponible(), ids);
  }

  @Override
  @Retry(name = INSTANCE, fallbackMethod = "affecterRepli")
  @CircuitBreaker(name = INSTANCE)
  public ResultatAffectation affecter(DemandeAffectation demande) {
    return client
        .post()
        .uri("/api/materiels/{id}/affecter", demande.materielId())
        .body(
            new SuiviDtos.DemandeAffectation(
                demande.chantierId(),
                demande.besoinId(),
                demande.dateDebut(),
                demande.dateFinPrevue()))
        .exchange(
            (requete, reponse) -> {
              int statut = reponse.getStatusCode().value();
              // Refus métier : conflit de planning (409), règle R04 (422), matériel inconnu (404).
              if (statut == HttpStatus.CONFLICT.value()
                  || statut == HttpStatus.UNPROCESSABLE_ENTITY.value()
                  || statut == HttpStatus.NOT_FOUND.value()) {
                SuiviDtos.Probleme probleme = reponse.bodyTo(SuiviDtos.Probleme.class);
                String raison =
                    probleme != null && probleme.code() != null
                        ? probleme.code()
                        : "HTTP_" + statut;
                return (ResultatAffectation) new ResultatAffectation.Refuse(raison);
              }
              exigerSucces(reponse.getStatusCode(), "affectation du matériel");
              SuiviDtos.Affectation a = reponse.bodyTo(SuiviDtos.Affectation.class);
              return new ResultatAffectation.Affecte(a == null ? null : a.id());
            });
  }

  Optional<ChantierInfo> trouverRepli(UUID chantierId, Throwable cause) {
    throw traduire("consultation du chantier", cause);
  }

  Disponibilite verifierRepli(DemandeDisponibilite demande, Throwable cause) {
    throw traduire("disponibilité du parc", cause);
  }

  ResultatAffectation affecterRepli(DemandeAffectation demande, Throwable cause) {
    throw traduire("affectation du matériel", cause);
  }

  private static void exigerSucces(HttpStatusCode statut, String operation) {
    if (statut.is2xxSuccessful()) {
      return;
    }
    if (statut.value() == HttpStatus.UNAUTHORIZED.value()
        || statut.value() == HttpStatus.FORBIDDEN.value()) {
      throw new ForbiddenOperationException(
          ACCES_SUIVI_REFUSE, "Le service de suivi refuse l'opération : " + operation + ".");
    }
    if (statut.is5xxServerError()) {
      throw new SuiviErreurTechniqueException(
          "ms-suivi HTTP " + statut.value() + " : " + operation);
    }
    throw new IllegalStateException(
        "Réponse inattendue de ms-suivi (HTTP " + statut.value() + ") : " + operation);
  }

  private static RuntimeException traduire(String operation, Throwable cause) {
    if (cause instanceof CseException metier) {
      return metier;
    }
    LOG.warn("ms-suivi indisponible ({}) : {}", operation, cause.toString());
    return new ServiceSuiviIndisponibleException(operation);
  }
}
