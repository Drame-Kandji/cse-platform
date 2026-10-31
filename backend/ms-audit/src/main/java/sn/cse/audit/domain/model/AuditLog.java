package sn.cse.audit.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import sn.cse.audit.domain.exception.EntreeAuditInvalideException;

/**
 * Entrée du journal d'audit. Objet de valeur immuable : une fois créée, une entrée n'est jamais
 * modifiée ni supprimée (exigence d'audit horodaté, B.13). Aucune méthode de modification n'est
 * donc proposée.
 *
 * @param id identifiant technique (généré côté application)
 * @param eventId identifiant de l'événement {@code AUDIT} d'origine (unique : idempotence)
 * @param occurredAt instant de l'action auditée
 * @param service microservice à l'origine de l'action
 * @param action action réalisée (type d'événement de domaine, ex. {@code BESOIN_VALIDE})
 * @param entite type d'entité concernée (ex. {@code BesoinMateriel})
 * @param entiteId identifiant de l'entité
 * @param utilisateurId auteur ({@code sub} Keycloak), {@code null} pour une action système
 * @param utilisateurNom nom affichable de l'auteur
 * @param roles rôles de l'auteur au moment de l'action
 * @param resume résumé lisible, facultatif
 * @param correlationId identifiant de corrélation de la requête d'origine, facultatif
 * @param enregistreLe instant d'enregistrement dans le journal
 */
public record AuditLog(
    UUID id,
    UUID eventId,
    Instant occurredAt,
    String service,
    String action,
    String entite,
    String entiteId,
    String utilisateurId,
    String utilisateurNom,
    List<String> roles,
    String resume,
    String correlationId,
    Instant enregistreLe) {

  /** Longueur maximale des champs d'identification (service, action). */
  public static final int LONGUEUR_CODE = 80;

  /** Longueur maximale des champs entité / identifiant / corrélation. */
  public static final int LONGUEUR_REFERENCE = 120;

  /** Longueur maximale du nom de l'auteur. */
  public static final int LONGUEUR_NOM = 200;

  public AuditLog {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(enregistreLe, "enregistreLe");
    if (eventId == null) {
      throw new EntreeAuditInvalideException("Identifiant d'événement manquant");
    }
    if (occurredAt == null) {
      throw new EntreeAuditInvalideException("Horodatage de l'action manquant");
    }
    service = obligatoire(service, "service", LONGUEUR_CODE);
    action = obligatoire(action, "action", LONGUEUR_CODE);
    entite = obligatoire(entite, "entite", LONGUEUR_REFERENCE);
    entiteId = obligatoire(entiteId, "entiteId", LONGUEUR_REFERENCE);
    utilisateurId = facultatif(utilisateurId, "utilisateurId", LONGUEUR_REFERENCE);
    utilisateurNom = facultatif(utilisateurNom, "utilisateurNom", LONGUEUR_NOM);
    correlationId = facultatif(correlationId, "correlationId", LONGUEUR_REFERENCE);
    resume = resume == null || resume.isBlank() ? null : resume.strip();
    roles = roles == null ? List.of() : roles.stream().filter(Objects::nonNull).toList();
  }

  /**
   * Fabrique une nouvelle entrée avec un identifiant généré.
   *
   * @param trace données de l'action auditée
   * @param enregistreLe instant d'enregistrement
   * @return l'entrée prête à être ajoutée au journal
   */
  public static AuditLog nouvelle(TraceAction trace, Instant enregistreLe) {
    return new AuditLog(
        UUID.randomUUID(),
        trace.eventId(),
        trace.occurredAt(),
        trace.service(),
        trace.action(),
        trace.entite(),
        trace.entiteId(),
        trace.utilisateurId(),
        trace.utilisateurNom(),
        trace.roles(),
        trace.resume(),
        trace.correlationId(),
        enregistreLe);
  }

  /**
   * @return vrai si l'action a été réalisée par un traitement automatique (aucun auteur)
   */
  public boolean estActionSysteme() {
    return utilisateurId == null;
  }

  private static String obligatoire(String valeur, String champ, int max) {
    if (valeur == null || valeur.isBlank()) {
      throw new EntreeAuditInvalideException("Champ d'audit obligatoire manquant : " + champ);
    }
    return borne(valeur.strip(), champ, max);
  }

  private static String facultatif(String valeur, String champ, int max) {
    return valeur == null || valeur.isBlank() ? null : borne(valeur.strip(), champ, max);
  }

  private static String borne(String valeur, String champ, int max) {
    if (valeur.length() > max) {
      throw new EntreeAuditInvalideException(
          "Champ d'audit trop long : " + champ + " (" + valeur.length() + " > " + max + ")");
    }
    return valeur;
  }
}
