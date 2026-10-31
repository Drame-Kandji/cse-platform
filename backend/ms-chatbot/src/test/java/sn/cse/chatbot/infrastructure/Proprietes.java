package sn.cse.chatbot.infrastructure;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import sn.cse.chatbot.domain.model.TypeSource;
import sn.cse.chatbot.infrastructure.config.ChatbotProperties;

/** Configuration minimale pour les tests d'adaptateurs. */
public final class Proprietes {

  private Proprietes() {}

  public static ChatbotProperties chatbot(String url, int capacite, Duration periode) {
    return new ChatbotProperties(
        1000,
        6000,
        4000,
        10,
        10,
        30,
        6,
        80,
        160,
        6,
        new ChatbotProperties.LimiteDebit(capacite, periode),
        new ChatbotProperties.Services(url, url, url, url),
        Map.of(TypeSource.MATERIEL, "/materiels/{id}"),
        null,
        null,
        null,
        null);
  }

  public static ChatbotProperties chatbot(String url) {
    return chatbot(url, 10, Duration.ofMinutes(1));
  }

  public static List<String> vide() {
    return List.of();
  }
}
