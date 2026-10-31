package sn.cse.e2e.support;

/**
 * Paramètres des tests E2E, lus dans les propriétés système (renseignées par failsafe depuis le
 * POM, surchargeables par {@code -De2e.gateway=...}).
 */
public final class E2eConfig {

  private E2eConfig() {}

  /**
   * URL de la passerelle.
   *
   * @return URL de base, sans barre finale
   */
  public static String gateway() {
    return propriete("e2e.gateway", "http://localhost:8080");
  }

  /**
   * Point de jeton Keycloak du realm.
   *
   * @return URL du token endpoint OpenID Connect
   */
  public static String tokenEndpoint() {
    return propriete("e2e.keycloak", "http://localhost:8180")
        + "/realms/"
        + propriete("e2e.realm", "cse")
        + "/protocol/openid-connect/token";
  }

  /**
   * Client Keycloak de développement autorisant le password grant.
   *
   * @return identifiant du client
   */
  public static String client() {
    return propriete("e2e.client", "cse-tests-e2e");
  }

  /**
   * Mot de passe des comptes de démonstration.
   *
   * @return mot de passe
   */
  public static String motDePasse() {
    return propriete("e2e.password", "Cse@2026");
  }

  private static String propriete(String cle, String defaut) {
    String valeur = System.getProperty(cle);
    String brute = valeur == null || valeur.isBlank() ? defaut : valeur;
    return brute.endsWith("/") ? brute.substring(0, brute.length() - 1) : brute;
  }
}
