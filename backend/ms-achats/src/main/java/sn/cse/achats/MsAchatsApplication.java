package sn.cse.achats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microservice des achats : appels d'offres, contrats, engagements et factures. */
@SpringBootApplication
public class MsAchatsApplication {

  /**
   * @param args arguments de la ligne de commande
   */
  public static void main(String[] args) {
    SpringApplication.run(MsAchatsApplication.class, args);
  }
}
