package sn.cse.auth.infrastructure.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import sn.cse.auth.application.exception.CompteIdentiteIntrouvableException;
import sn.cse.auth.application.exception.IdentiteIndisponibleException;
import sn.cse.auth.application.port.out.CompteIdentite;
import sn.cse.auth.application.port.out.IdentityProviderPort;
import sn.cse.auth.application.port.out.LienActivation;
import sn.cse.auth.application.port.out.NouveauCompte;
import sn.cse.auth.application.port.out.ProfilIdentite;
import sn.cse.auth.domain.exception.EmailDejaUtiliseException;
import sn.cse.auth.domain.model.Role;
import sn.cse.auth.domain.model.Utilisateur.Identite;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;

/**
 * Adaptateur de l'API REST d'administration Keycloak (client HTTP simple, sans dépendance
 * Keycloak). Les représentations utilisateur sont lues puis réécrites intégralement afin de ne
 * jamais effacer les attributs que ms-auth ne gère pas.
 */
@Component
public class KeycloakAdminAdapter implements IdentityProviderPort {

  static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";
  static final String CONFIGURE_TOTP = "CONFIGURE_TOTP";
  static final String TYPE_OTP = "otp";
  static final String TYPE_MOT_DE_PASSE = "password";

  private static final Logger LOG = LoggerFactory.getLogger(KeycloakAdminAdapter.class);
  private static final ParameterizedTypeReference<Map<String, Object>> OBJET =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTE =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<RoleRepresentation>> ROLES =
      new ParameterizedTypeReference<>() {};

  private final RestClient http;
  private final KeycloakProperties properties;
  private final KeycloakTokenProvider jetons;

  /**
   * @param builder builder RestClient de Spring Boot
   * @param properties paramètres Keycloak
   * @param clock horloge (expiration du jeton)
   */
  public KeycloakAdminAdapter(
      RestClient.Builder builder, KeycloakProperties properties, Clock clock) {
    HttpClient client =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(properties.connectTimeout())
            .build();
    JdkClientHttpRequestFactory fabrique = new JdkClientHttpRequestFactory(client);
    fabrique.setReadTimeout(properties.readTimeout());
    this.http = builder.requestFactory(fabrique).build();
    this.properties = properties;
    this.jetons = new KeycloakTokenProvider(http, properties, clock);
  }

  /** Représentation minimale d'un rôle realm Keycloak. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record RoleRepresentation(String id, String name) {}

  @Override
  public UUID creerCompte(NouveauCompte compte) {
    Identite identite = compte.identite();
    Map<String, Object> representation = new LinkedHashMap<>();
    representation.put("username", identite.email());
    representation.put("email", identite.email());
    representation.put("firstName", identite.prenom());
    representation.put("lastName", identite.nom());
    representation.put("enabled", true);
    representation.put("emailVerified", true);
    representation.put("attributes", KeycloakAttributs.versKeycloak(compte.attributs()));
    if (compte.motDePasseTemporaire() != null) {
      representation.put(
          "credentials",
          List.of(
              Map.of(
                  "type", "password", "value", compte.motDePasseTemporaire(), "temporary", true)));
    }
    representation.put(
        "requiredActions",
        compte.otpObligatoire()
            ? List.of(UPDATE_PASSWORD, CONFIGURE_TOTP)
            : List.of(UPDATE_PASSWORD));
    try {
      URI location =
          executer(
              jeton ->
                  http.post()
                      .uri(properties.adminBase() + "/users")
                      .headers(h -> h.setBearerAuth(jeton))
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(representation)
                      .retrieve()
                      .toBodilessEntity()
                      .getHeaders()
                      .getLocation(),
              null);
      if (location == null) {
        throw new IdentiteIndisponibleException("Réponse de création sans en-tête Location", null);
      }
      String chemin = location.getPath();
      return UUID.fromString(chemin.substring(chemin.lastIndexOf('/') + 1));
    } catch (ConflictException e) {
      throw new EmailDejaUtiliseException(identite.email());
    }
  }

  @Override
  public void supprimerCompte(UUID id) {
    try {
      executer(
          jeton ->
              http.delete()
                  .uri(utilisateur(id))
                  .headers(h -> h.setBearerAuth(jeton))
                  .retrieve()
                  .toBodilessEntity(),
          id);
    } catch (CompteIdentiteIntrouvableException e) {
      LOG.debug("Compte {} déjà absent de Keycloak", id);
    }
  }

  @Override
  public void mettreAJourProfil(UUID id, ProfilIdentite profil) {
    Map<String, Object> representation = lireUtilisateur(id);
    representation.put("email", profil.identite().email());
    representation.put("firstName", profil.identite().prenom());
    representation.put("lastName", profil.identite().nom());
    Map<String, Object> attributs = new LinkedHashMap<>();
    if (representation.get("attributes") instanceof Map<?, ?> existants) {
      existants.forEach((cle, valeur) -> attributs.put(String.valueOf(cle), valeur));
    }
    KeycloakAttributs.GERES.forEach(attributs::remove);
    attributs.putAll(KeycloakAttributs.versKeycloak(profil.attributs()));
    representation.put("attributes", attributs);
    ecrireUtilisateur(id, representation);
  }

  @Override
  public void definirActif(UUID id, boolean actif) {
    Map<String, Object> representation = lireUtilisateur(id);
    representation.put("enabled", actif);
    ecrireUtilisateur(id, representation);
  }

  @Override
  public void reinitialiserMotDePasse(UUID id, String motDePasseTemporaire) {
    Map<String, Object> credential =
        Map.of("type", "password", "value", motDePasseTemporaire, "temporary", true);
    executer(
        jeton ->
            http.put()
                .uri(utilisateur(id) + "/reset-password")
                .headers(h -> h.setBearerAuth(jeton))
                .contentType(MediaType.APPLICATION_JSON)
                .body(credential)
                .retrieve()
                .toBodilessEntity(),
        id);
  }

  @Override
  public void assignerRole(UUID id, Role role) {
    String mappings = utilisateur(id) + "/role-mappings/realm";
    List<RoleRepresentation> actuels = rolesRealm(id);
    List<RoleRepresentation> aRetirer =
        actuels.stream()
            .filter(r -> Role.depuisCode(r.name()).isPresent() && !role.code().equals(r.name()))
            .toList();
    if (!aRetirer.isEmpty()) {
      executer(
          jeton ->
              http.method(HttpMethod.DELETE)
                  .uri(mappings)
                  .headers(h -> h.setBearerAuth(jeton))
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(aRetirer)
                  .retrieve()
                  .toBodilessEntity(),
          id);
    }
    if (actuels.stream().noneMatch(r -> role.code().equals(r.name()))) {
      RoleRepresentation cible =
          executer(
              jeton ->
                  http.get()
                      .uri(properties.adminBase() + "/roles/" + role.code())
                      .headers(h -> h.setBearerAuth(jeton))
                      .retrieve()
                      .body(RoleRepresentation.class),
              null);
      executer(
          jeton ->
              http.post()
                  .uri(mappings)
                  .headers(h -> h.setBearerAuth(jeton))
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(List.of(cible))
                  .retrieve()
                  .toBodilessEntity(),
          id);
    }
  }

  @Override
  public void exigerConfigurationOtp(UUID id) {
    Map<String, Object> representation = lireUtilisateur(id);
    Set<String> actions = new LinkedHashSet<>();
    if (representation.get("requiredActions") instanceof List<?> existantes) {
      existantes.forEach(a -> actions.add(String.valueOf(a)));
    }
    if (actions.add(CONFIGURE_TOTP)) {
      representation.put("requiredActions", new ArrayList<>(actions));
      ecrireUtilisateur(id, representation);
    }
  }

  @Override
  public boolean otpConfigure(UUID id) {
    return credentials(id).stream().anyMatch(c -> TYPE_OTP.equals(c.get("type")));
  }

  @Override
  public List<CompteIdentite> listerComptes() {
    Map<UUID, Set<String>> rolesParCompte = new LinkedHashMap<>();
    for (Role role : Role.values()) {
      for (UUID id : membres(role)) {
        rolesParCompte.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(role.code());
      }
    }
    List<CompteIdentite> comptes = new ArrayList<>();
    rolesParCompte.forEach(
        (id, roles) -> {
          try {
            comptes.add(compte(id, roles));
          } catch (CompteIdentiteIntrouvableException e) {
            // Compte supprimé entre les deux lectures : il sera traité au passage suivant.
            LOG.debug("Compte {} disparu pendant la synchronisation", id);
          }
        });
    return comptes;
  }

  @Override
  public Optional<CompteIdentite> compteParEmail(String email) {
    List<Map<String, Object>> trouves =
        executer(
            jeton ->
                http.get()
                    .uri(properties.adminBase() + "/users?email={email}&exact=true", email)
                    .headers(h -> h.setBearerAuth(jeton))
                    .retrieve()
                    .body(LISTE),
            null);
    Optional<UUID> id =
        Objects.requireNonNullElse(trouves, List.<Map<String, Object>>of()).stream()
            .filter(u -> email.equalsIgnoreCase(texte(u, "email")))
            .map(u -> UUID.fromString(String.valueOf(u.get("id"))))
            .findFirst();
    if (id.isEmpty()) {
      return Optional.empty();
    }
    Set<String> roles = new LinkedHashSet<>();
    rolesRealm(id.get()).stream()
        .map(RoleRepresentation::name)
        .filter(nom -> Role.depuisCode(nom).isPresent())
        .forEach(roles::add);
    return Optional.of(compte(id.get(), roles));
  }

  @Override
  public void rattacherFournisseur(UUID id, UUID fournisseurId) {
    Map<String, Object> representation = lireUtilisateur(id);
    Map<String, Object> attributs = new LinkedHashMap<>();
    if (representation.get("attributes") instanceof Map<?, ?> existants) {
      existants.forEach((cle, valeur) -> attributs.put(String.valueOf(cle), valeur));
    }
    attributs.put(KeycloakAttributs.FOURNISSEUR_ID, List.of(fournisseurId.toString()));
    representation.put("attributes", attributs);
    ecrireUtilisateur(id, representation);
  }

  @Override
  public boolean motDePasseDefini(UUID id) {
    return credentials(id).stream().anyMatch(c -> TYPE_MOT_DE_PASSE.equals(c.get("type")));
  }

  @Override
  public void envoyerEmailActivation(UUID id, LienActivation lien) {
    executer(
        jeton ->
            http.put()
                .uri(
                    utilisateur(id)
                        + "/execute-actions-email?client_id={client}&redirect_uri={retour}"
                        + "&lifespan={duree}",
                    lien.clientId(),
                    lien.redirectUri(),
                    lien.validite().toSeconds())
                .headers(h -> h.setBearerAuth(jeton))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(UPDATE_PASSWORD))
                .retrieve()
                .toBodilessEntity(),
        id);
  }

  private List<RoleRepresentation> rolesRealm(UUID id) {
    return Objects.requireNonNullElse(
        executer(
            jeton ->
                http.get()
                    .uri(utilisateur(id) + "/role-mappings/realm")
                    .headers(h -> h.setBearerAuth(jeton))
                    .retrieve()
                    .body(ROLES),
            id),
        List.of());
  }

  private List<Map<String, Object>> credentials(UUID id) {
    return Objects.requireNonNullElse(
        executer(
            jeton ->
                http.get()
                    .uri(utilisateur(id) + "/credentials")
                    .headers(h -> h.setBearerAuth(jeton))
                    .retrieve()
                    .body(LISTE),
            id),
        List.of());
  }

  private List<UUID> membres(Role role) {
    List<UUID> ids = new ArrayList<>();
    int premier = 0;
    int taille = properties.taillePage();
    while (true) {
      int debut = premier;
      List<Map<String, Object>> page =
          executer(
              jeton ->
                  http.get()
                      .uri(
                          properties.adminBase()
                              + "/roles/"
                              + role.code()
                              + "/users?first="
                              + debut
                              + "&max="
                              + taille)
                      .headers(h -> h.setBearerAuth(jeton))
                      .retrieve()
                      .body(LISTE),
              null);
      List<Map<String, Object>> membres = page == null ? List.of() : page;
      membres.forEach(m -> ids.add(UUID.fromString(String.valueOf(m.get("id")))));
      if (membres.size() < taille) {
        return ids;
      }
      premier += taille;
    }
  }

  private CompteIdentite compte(UUID id, Set<String> roles) {
    Map<String, Object> rep = lireUtilisateur(id);
    Map<String, List<String>> attributs = new LinkedHashMap<>();
    if (rep.get("attributes") instanceof Map<?, ?> brut) {
      brut.forEach(
          (cle, valeur) ->
              attributs.put(
                  String.valueOf(cle),
                  valeur instanceof List<?> liste
                      ? liste.stream().map(String::valueOf).toList()
                      : List.of(String.valueOf(valeur))));
    }
    return new CompteIdentite(
        id,
        new Identite(texte(rep, "email"), texte(rep, "firstName"), texte(rep, "lastName"), null),
        Boolean.TRUE.equals(rep.get("enabled")),
        roles,
        KeycloakAttributs.depuisKeycloak(attributs),
        otpConfigure(id));
  }

  private static String texte(Map<String, Object> rep, String champ) {
    Object valeur = rep.get(champ);
    return valeur == null ? null : valeur.toString();
  }

  private Map<String, Object> lireUtilisateur(UUID id) {
    Map<String, Object> rep =
        executer(
            jeton ->
                http.get()
                    .uri(utilisateur(id))
                    .headers(h -> h.setBearerAuth(jeton))
                    .retrieve()
                    .body(OBJET),
            id);
    if (rep == null) {
      throw new CompteIdentiteIntrouvableException(id);
    }
    return new LinkedHashMap<>(rep);
  }

  private void ecrireUtilisateur(UUID id, Map<String, Object> representation) {
    executer(
        jeton ->
            http.put()
                .uri(utilisateur(id))
                .headers(h -> h.setBearerAuth(jeton))
                .contentType(MediaType.APPLICATION_JSON)
                .body(representation)
                .retrieve()
                .toBodilessEntity(),
        id);
  }

  private String utilisateur(UUID id) {
    return properties.adminBase() + "/users/" + id;
  }

  /**
   * Exécute un appel authentifié (un renouvellement de jeton sur 401) et traduit les erreurs HTTP
   * en exceptions applicatives.
   */
  private <T> T executer(Function<String, T> appel, UUID compte) {
    try {
      try {
        return appel.apply(jetons.jeton());
      } catch (HttpClientErrorException.Unauthorized e) {
        jetons.invalider();
        return appel.apply(jetons.jeton());
      }
    } catch (RestClientResponseException e) {
      throw traduire(e, compte);
    } catch (RestClientException e) {
      throw new IdentiteIndisponibleException("Keycloak injoignable", e);
    }
  }

  private static RuntimeException traduire(RestClientResponseException e, UUID compte) {
    HttpStatusCode statut = e.getStatusCode();
    if (statut.value() == 404 && compte != null) {
      return new CompteIdentiteIntrouvableException(compte);
    }
    if (statut.value() == 409) {
      return new ConflictException(
          "CONFLIT_IDENTITE", "Le compte est en conflit avec un compte existant.");
    }
    if (statut.value() == 400) {
      String corps = e.getResponseBodyAsString();
      if (corps.contains("invalidPassword")) {
        return new BusinessRuleException(
            "MOT_DE_PASSE_NON_CONFORME",
            "Le mot de passe ne respecte pas la politique de mots de passe de la plateforme.");
      }
      return new BusinessRuleException(
          "IDENTITE_REFUSEE", "Le fournisseur d'identité a refusé les données du compte.");
    }
    LOG.error("Erreur Keycloak {} : {}", statut.value(), e.getResponseBodyAsString());
    return new IdentiteIndisponibleException("Erreur Keycloak " + statut.value(), e);
  }
}
