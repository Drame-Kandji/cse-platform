package sn.cse.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Point d'entrée de ms-reporting : tableaux de bord CQRS alimentés par Kafka. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsReportingApplication {

  /**
   * @param args arguments de la ligne de commande
   */
  public static void main(String[] args) {
    SpringApplication.run(MsReportingApplication.class, args);
  }
}
