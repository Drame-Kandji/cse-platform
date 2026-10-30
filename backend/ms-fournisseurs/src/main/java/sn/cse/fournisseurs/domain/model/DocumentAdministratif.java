package sn.cse.fournisseurs.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.fournisseurs.domain.exception.CodesErreur;

/**
 * Document administratif d'un fournisseur (NINEA, RCCM, quitus fiscal…), éventuellement soumis à
 * expiration. Fait partie de l'agrégat {@link Fournisseur}.
 */
public final class DocumentAdministratif {

  private final UUID id;
  private final TypeDocument type;
  private final String libelle;
  private final LocalDate dateExpiration;
  private boolean expirationNotifiee;

  private DocumentAdministratif(
      UUID id,
      TypeDocument type,
      String libelle,
      LocalDate dateExpiration,
      boolean expirationNotifiee) {
    this.id = Objects.requireNonNull(id, "id");
    this.type = Objects.requireNonNull(type, "type");
    if (libelle == null || libelle.isBlank()) {
      throw new BusinessRuleException(
          CodesErreur.DONNEE_INVALIDE, "Le libellé du document est obligatoire.");
    }
    this.libelle = libelle.trim();
    this.dateExpiration = dateExpiration;
    this.expirationNotifiee = expirationNotifiee;
  }

  /**
   * Crée un nouveau document.
   *
   * @param type type de document
   * @param libelle libellé
   * @param dateExpiration date d'expiration (optionnelle)
   * @return document
   */
  public static DocumentAdministratif creer(
      TypeDocument type, String libelle, LocalDate dateExpiration) {
    return new DocumentAdministratif(UUID.randomUUID(), type, libelle, dateExpiration, false);
  }

  /**
   * Reconstitue un document depuis la persistance.
   *
   * @param id identifiant
   * @param type type
   * @param libelle libellé
   * @param dateExpiration date d'expiration
   * @param expirationNotifiee l'événement {@code DOCUMENT_EXPIRE} a déjà été émis
   * @return document
   */
  public static DocumentAdministratif reconstituer(
      UUID id,
      TypeDocument type,
      String libelle,
      LocalDate dateExpiration,
      boolean expirationNotifiee) {
    return new DocumentAdministratif(id, type, libelle, dateExpiration, expirationNotifiee);
  }

  /**
   * @param aujourdhui date de référence
   * @return {@code true} si la date d'expiration est strictement antérieure à la date de référence
   */
  public boolean estExpire(LocalDate aujourdhui) {
    return dateExpiration != null && dateExpiration.isBefore(aujourdhui);
  }

  /**
   * @param aujourdhui date de référence
   * @return {@code true} si le document est expiré et que l'alerte n'a pas encore été émise
   */
  public boolean expirationANotifier(LocalDate aujourdhui) {
    return estExpire(aujourdhui) && !expirationNotifiee;
  }

  void marquerExpirationNotifiee() {
    this.expirationNotifiee = true;
  }

  public UUID getId() {
    return id;
  }

  public TypeDocument getType() {
    return type;
  }

  public String getLibelle() {
    return libelle;
  }

  public Optional<LocalDate> getDateExpiration() {
    return Optional.ofNullable(dateExpiration);
  }

  public boolean isExpirationNotifiee() {
    return expirationNotifiee;
  }
}
