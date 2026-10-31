package sn.cse.chatbot.infrastructure.llm;

import sn.cse.chatbot.application.exception.LlmIndisponibleException;
import sn.cse.chatbot.application.port.out.LlmPort;
import sn.cse.chatbot.application.port.out.RequeteLlm;

/** Adaptateur utilisé quand aucun LLM n'est configuré : toujours indisponible. */
public class LlmNonConfigure implements LlmPort {

  @Override
  public boolean estDisponible() {
    return false;
  }

  @Override
  public String generer(RequeteLlm requete) {
    throw new LlmIndisponibleException("Aucun LLM configuré", null);
  }
}
