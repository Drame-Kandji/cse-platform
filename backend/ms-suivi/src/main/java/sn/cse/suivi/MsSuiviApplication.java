package sn.cse.suivi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microservice de suivi du parc : chantiers, matériels, affectations, maintenances, pannes. */
@SpringBootApplication
public class MsSuiviApplication {

  /**
   * @param args arguments de la ligne de commande
   */
  public static void main(String[] args) {
    SpringApplication.run(MsSuiviApplication.class, args);
  }
}
