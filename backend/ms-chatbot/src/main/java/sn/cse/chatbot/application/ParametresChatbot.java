package sn.cse.chatbot.application;

import java.util.List;
import java.util.Map;
import java.util.Set;
import sn.cse.chatbot.domain.model.Procedure;
import sn.cse.chatbot.domain.model.Section;
import sn.cse.chatbot.domain.model.TypeSource;

/**
 * Paramètres métier du chatbot (issus de la configuration validée {@code cse.chatbot.*}).
 *
 * @param questionMaxCaracteres longueur maximale d'une question
 * @param contexteMaxCaracteres longueur maximale du contexte transmis au LLM
 * @param reponseMaxCaracteres longueur maximale d'une réponse
 * @param resultatsMax nombre maximal d'éléments demandés à chaque service
 * @param sourcesMax nombre maximal de sources citées
 * @param horizonPlanningJours horizon du planning par défaut (jours)
 * @param messagesHistorique messages précédents transmis au LLM
 * @param longueurTitre longueur du titre d'une conversation
 * @param longueurApercu longueur de l'aperçu du dernier message
 * @param suggestionsMax nombre maximal de suggestions
 * @param liens modèles de liens frontend par type de source
 * @param acces rôles autorisés par section
 * @param rolesMontants rôles autorisés à voir les montants
 * @param suggestions questions types par rôle (clé {@code DEFAUT} pour tous)
 * @param procedures base de procédures
 */
public record ParametresChatbot(
    int questionMaxCaracteres,
    int contexteMaxCaracteres,
    int reponseMaxCaracteres,
    int resultatsMax,
    int sourcesMax,
    int horizonPlanningJours,
    int messagesHistorique,
    int longueurTitre,
    int longueurApercu,
    int suggestionsMax,
    Map<TypeSource, String> liens,
    Map<Section, Set<String>> acces,
    Set<String> rolesMontants,
    Map<String, List<String>> suggestions,
    List<Procedure> procedures) {

  /** Clé des suggestions communes à tous les rôles. */
  public static final String SUGGESTIONS_DEFAUT = "DEFAUT";

  public ParametresChatbot {
    liens = Map.copyOf(liens);
    acces = Map.copyOf(acces);
    rolesMontants = Set.copyOf(rolesMontants);
    suggestions = Map.copyOf(suggestions);
    procedures = List.copyOf(procedures);
  }
}
