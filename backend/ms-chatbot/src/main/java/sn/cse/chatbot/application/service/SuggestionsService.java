package sn.cse.chatbot.application.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import sn.cse.chatbot.application.ParametresChatbot;
import sn.cse.chatbot.application.port.in.SuggestionsUseCase;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;

/** Questions types selon les rôles de l'utilisateur (configuration). */
@Service
public class SuggestionsService implements SuggestionsUseCase {

  private final ParametresChatbot parametres;

  /**
   * @param parametres paramètres
   */
  public SuggestionsService(ParametresChatbot parametres) {
    this.parametres = parametres;
  }

  @Override
  public List<String> suggestions(CurrentUser utilisateur) {
    Set<String> resultat = new LinkedHashSet<>();
    List<String> cles = new ArrayList<>();
    Roles.INTERNES.stream().filter(utilisateur::hasRole).forEach(cles::add);
    cles.add(ParametresChatbot.SUGGESTIONS_DEFAUT);
    for (String cle : cles) {
      resultat.addAll(parametres.suggestions().getOrDefault(cle, List.of()));
    }
    return resultat.stream().limit(parametres.suggestionsMax()).toList();
  }
}
