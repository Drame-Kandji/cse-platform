package sn.cse.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Point d'entrée de ms-notification : notifications in-app (SSE) et e-mail. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class MsNotificationApplication {

  /**
   * @param args arguments de la ligne de commande
   */
  public static void main(String[] args) {
    SpringApplication.run(MsNotificationApplication.class, args);
  }
}
