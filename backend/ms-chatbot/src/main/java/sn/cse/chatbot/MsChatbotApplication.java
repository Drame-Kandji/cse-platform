package sn.cse.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Point d'entrée de ms-chatbot : assistant matériel et logistique. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MsChatbotApplication {

  /**
   * @param args arguments de la ligne de commande
   */
  public static void main(String[] args) {
    SpringApplication.run(MsChatbotApplication.class, args);
  }
}
