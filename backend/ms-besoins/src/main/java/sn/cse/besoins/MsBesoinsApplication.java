package sn.cse.besoins;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Microservice ms-besoins : expression, validation et orientation des besoins en matériel. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsBesoinsApplication {

  /**
   * @param args arguments de la ligne de commande
   */
  public static void main(String[] args) {
    SpringApplication.run(MsBesoinsApplication.class, args);
  }
}
