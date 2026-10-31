package sn.cse.chatbot.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.cse.chatbot.application.ParametresChatbot;
import sn.cse.chatbot.domain.model.Procedure;

/** Traduction de la configuration validée en paramètres applicatifs. */
@Configuration(proxyBeanMethods = false)
public class ChatbotConfiguration {

  /**
   * @param p configuration
   * @return les paramètres du chatbot
   */
  @Bean
  public ParametresChatbot parametresChatbot(ChatbotProperties p) {
    return new ParametresChatbot(
        p.questionMaxCaracteres(),
        p.contexteMaxCaracteres(),
        p.reponseMaxCaracteres(),
        p.resultatsMax(),
        p.sourcesMax(),
        p.horizonPlanningJours(),
        p.messagesHistorique(),
        p.longueurTitre(),
        p.longueurApercu(),
        p.suggestionsMax(),
        p.liens(),
        p.acces(),
        p.rolesMontants(),
        p.suggestions(),
        p.procedures().stream()
            .map(x -> new Procedure(x.sujet(), x.motsCles(), x.etapes()))
            .toList());
  }
}
