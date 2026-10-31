package sn.cse.audit.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import sn.cse.common.exception.InvalidRequestException;

/**
 * Filtres de consultation du journal d'audit. Les bornes {@code du}/{@code au} sont des jours
 * calendaires UTC inclusifs, convertis en intervalle d'instants semi-ouvert {@code [debut, fin[}.
 *
 * @param utilisateurId auteur exact
 * @param entite type d'entité exact
 * @param action action exacte
 * @param service service émetteur exact
 * @param du premier jour inclus
 * @param au dernier jour inclus
 * @param q texte libre recherché (résumé, identifiant d'entité, nom de l'auteur, action, entité)
 */
public record CritereRechercheAudit(
    String utilisateurId,
    String entite,
    String action,
    String service,
    LocalDate du,
    LocalDate au,
    String q) {

  public CritereRechercheAudit {
    utilisateurId = normaliser(utilisateurId);
    entite = normaliser(entite);
    action = normaliser(action);
    service = normaliser(service);
    q = normaliser(q);
    if (du != null && au != null && du.isAfter(au)) {
      throw new InvalidRequestException(
          "PERIODE_INVALIDE", "La date de début doit précéder ou égaler la date de fin.");
    }
  }

  /**
   * @param longueurMax longueur maximale autorisée pour le texte libre
   * @throws InvalidRequestException si le texte libre dépasse la longueur autorisée
   */
  public void verifierTexteLibre(int longueurMax) {
    if (q != null && q.length() > longueurMax) {
      throw new InvalidRequestException(
          "RECHERCHE_TROP_LONGUE",
          "Le texte recherché ne doit pas dépasser " + longueurMax + " caractères.");
    }
  }

  /**
   * @return début inclus de l'intervalle, s'il est borné
   */
  public Optional<Instant> debut() {
    return Optional.ofNullable(du).map(d -> d.atStartOfDay().toInstant(ZoneOffset.UTC));
  }

  /**
   * @return fin exclue de l'intervalle (lendemain du dernier jour), s'il est borné
   */
  public Optional<Instant> fin() {
    return Optional.ofNullable(au).map(d -> d.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
  }

  private static String normaliser(String valeur) {
    return valeur == null || valeur.isBlank() ? null : valeur.strip();
  }
}
