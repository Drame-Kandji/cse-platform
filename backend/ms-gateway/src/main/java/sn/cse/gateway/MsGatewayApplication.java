package sn.cse.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Passerelle d'API de la plateforme CSE (point d'entrée unique {@code /api/**}). */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsGatewayApplication {

  /**
   * @param args arguments de la ligne de commande
   */
  public static void main(String[] args) {
    SpringApplication.run(MsGatewayApplication.class, args);
  }
}
