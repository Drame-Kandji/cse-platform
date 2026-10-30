package sn.cse.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** ms-auth : profils utilisateurs et administration des comptes Keycloak. */
@SpringBootApplication
public class MsAuthApplication {

  /**
   * @param args arguments de la ligne de commande
   */
  public static void main(String[] args) {
    SpringApplication.run(MsAuthApplication.class, args);
  }
}
