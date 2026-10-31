package sn.cse.chatbot.domain.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import sn.cse.chatbot.domain.model.ContexteDonnees;
import sn.cse.chatbot.domain.model.Section;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;

/**
 * Contrôle d'accès du chatbot, en complément des services interrogés (qui appliquent leurs propres
 * règles sur le JWT relayé) : sections visibles par rôle et visibilité des montants. Le chatbot ne
 * montre jamais plus que ce que l'utilisateur pourrait voir lui-même.
 */
public class PolitiqueAcces {

  private final Map<Section, Set<String>> rolesParSection;
  private final Set<String> rolesMontants;

  /**
   * @param rolesParSection rôles autorisés par section (section absente = tous les rôles internes)
   * @param rolesMontants rôles autorisés à voir les montants et coûts
   */
  public PolitiqueAcces(Map<Section, Set<String>> rolesParSection, Set<String> rolesMontants) {
    this.rolesParSection = new EnumMap<>(Section.class);
    rolesParSection.forEach((k, v) -> this.rolesParSection.put(k, Set.copyOf(v)));
    this.rolesMontants = Set.copyOf(rolesMontants);
  }

  /**
   * Étape « verifierRole » du diagramme de séquence.
   *
   * @param utilisateur utilisateur courant
   * @return les sections qu'il peut consulter
   * @throws ForbiddenOperationException s'il ne détient aucun rôle interne
   */
  public Set<Section> verifierRole(CurrentUser utilisateur) {
    if (Roles.INTERNES.stream().noneMatch(utilisateur::hasRole)) {
      throw new ForbiddenOperationException(
          "CHATBOT_RESERVE_PERSONNEL", "L'assistant est réservé au personnel de la CSE.");
    }
    Set<Section> autorisees = EnumSet.noneOf(Section.class);
    for (Section s : Section.values()) {
      if (peutVoir(utilisateur, s)) {
        autorisees.add(s);
      }
    }
    return autorisees;
  }

  /**
   * @param utilisateur utilisateur
   * @param section section
   * @return vrai si la section lui est accessible
   */
  public boolean peutVoir(CurrentUser utilisateur, Section section) {
    Set<String> roles = rolesParSection.get(section);
    return roles == null
        ? Roles.INTERNES.stream().anyMatch(utilisateur::hasRole)
        : roles.stream().anyMatch(utilisateur::hasRole);
  }

  /**
   * Étape « appliquerFiltreRole » : retire les sections interdites et les montants non visibles.
   *
   * @param contexte données collectées
   * @param utilisateur utilisateur
   * @return les données filtrées
   */
  public ContexteDonnees appliquerFiltreRole(ContexteDonnees contexte, CurrentUser utilisateur) {
    ContexteDonnees filtre = contexte;
    for (Section s : Section.values()) {
      if (!peutVoir(utilisateur, s) && aDesDonnees(filtre, s)) {
        filtre = filtre.masquer(s);
      }
    }
    return rolesMontants.stream().anyMatch(utilisateur::hasRole) ? filtre : filtre.sansMontants();
  }

  private static boolean aDesDonnees(ContexteDonnees c, Section s) {
    return switch (s) {
      case MATERIELS -> !c.materiels().isEmpty();
      case BESOINS -> !c.besoins().isEmpty();
      case CONTRATS -> !c.contrats().isEmpty();
      case APPELS_OFFRES -> !c.appelsOffres().isEmpty();
      case FOURNISSEURS -> !c.fournisseurs().isEmpty();
      case MAINTENANCES -> !c.maintenances().isEmpty();
      case AFFECTATIONS -> !c.affectations().isEmpty();
      case CHANTIERS -> !c.chantiers().isEmpty();
      case PROCEDURES -> !c.procedures().isEmpty();
    };
  }
}
