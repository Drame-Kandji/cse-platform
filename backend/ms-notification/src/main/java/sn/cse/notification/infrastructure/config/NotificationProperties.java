package sn.cse.notification.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import sn.cse.common.security.Roles;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.domain.model.Operateur;

/**
 * Configuration de ms-notification ({@code cse.notification.*}) : règles de routage, libellés,
 * e-mail (le flux SSE a sa propre configuration {@code cse.notification.sse}). Validée au démarrage
 * : une règle incohérente empêche le service de démarrer.
 *
 * @param frontendBaseUrl URL publique du frontend (liens des e-mails)
 * @param valeurAbsente texte de repli d'un marqueur introuvable
 * @param libelles traduction des codes d'énumération dans les textes
 * @param regles règles de routage
 * @param mail paramètres d'envoi des e-mails
 */
@Validated
@ConfigurationProperties(prefix = "cse.notification")
public record NotificationProperties(
    @NotBlank String frontendBaseUrl,
    @DefaultValue("-") String valeurAbsente,
    Map<String, String> libelles,
    @NotEmpty List<@Valid Regle> regles,
    @NotNull @Valid Mail mail) {

  public NotificationProperties {
    libelles = libelles == null ? Map.of() : Map.copyOf(libelles);
  }

  /**
   * Règle de routage.
   *
   * @param evenement type d'événement
   * @param roles rôles destinataires
   * @param champsDestinataires champs du payload contenant un identifiant d'utilisateur
   * @param chefsDuChantier notifier les chefs du chantier du payload
   * @param champChantier champ contenant l'identifiant du chantier
   * @param niveau criticité
   * @param titre modèle du titre
   * @param message modèle du message
   * @param lien modèle du lien frontend
   * @param entite type d'entité
   * @param entiteId champ donnant l'identifiant de l'entité
   * @param email envoyer un e-mail
   * @param condition condition supplémentaire
   */
  public record Regle(
      @NotBlank String evenement,
      List<String> roles,
      List<String> champsDestinataires,
      boolean chefsDuChantier,
      @DefaultValue("chantierId") String champChantier,
      @NotNull NiveauNotification niveau,
      @NotBlank String titre,
      @NotBlank String message,
      String lien,
      String entite,
      String entiteId,
      boolean email,
      @Valid Condition condition) {

    public Regle {
      roles = roles == null ? List.of() : List.copyOf(roles);
      champsDestinataires =
          champsDestinataires == null ? List.of() : List.copyOf(champsDestinataires);
    }

    /**
     * @return vrai si au moins un destinataire est défini
     */
    @AssertTrue(message = "chaque règle doit définir au moins un destinataire")
    public boolean isDestinatairesDefinis() {
      return !roles.isEmpty() || !champsDestinataires.isEmpty() || chefsDuChantier;
    }

    /**
     * @return vrai si tous les rôles sont des rôles internes connus
     */
    @AssertTrue(message = "rôle inconnu dans une règle de routage")
    public boolean isRolesConnus() {
      return Roles.INTERNES.containsAll(roles);
    }
  }

  /**
   * Condition d'application.
   *
   * @param champ champ du payload
   * @param operateur opérateur
   * @param valeur valeur de comparaison
   */
  public record Condition(@NotBlank String champ, @NotNull Operateur operateur, String valeur) {}

  /**
   * E-mail.
   *
   * @param actif active l'envoi
   * @param expediteur adresse d'expédition
   * @param prefixeObjet préfixe de l'objet
   */
  public record Mail(boolean actif, @NotBlank String expediteur, @NotBlank String prefixeObjet) {}
}
