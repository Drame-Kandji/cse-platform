package sn.cse.fournisseurs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Point d'entrée du microservice ms-fournisseurs (port 8084, schéma {@code fournisseurs}). */
@SpringBootApplication
public class MsFournisseursApplication {

  /**
   * @param args arguments de la ligne de commande
   */
  public static void main(String[] args) {
    SpringApplication.run(MsFournisseursApplication.class, args);
  }
}
