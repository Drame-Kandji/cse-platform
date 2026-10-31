package sn.cse.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Point d'entrée de ms-audit : journal d'audit immuable de la plateforme CSE. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsAuditApplication {

  /**
   * @param args arguments de la ligne de commande
   */
  public static void main(String[] args) {
    SpringApplication.run(MsAuditApplication.class, args);
  }
}
