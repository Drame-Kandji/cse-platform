package sn.cse.besoins.application.port.out;

import java.time.LocalDate;
import java.util.UUID;

/** Affectation d'un matériel du parc à un chantier (ms-suivi, JWT de l'utilisateur relayé). */
public interface AffectationMaterielPort {

  /**
   * @param demande affectation demandée
   * @return le résultat (affecté ou refusé pour raison métier)
   * @throws sn.cse.besoins.application.exception.ServiceSuiviIndisponibleException si ms-suivi ne
   *     répond pas
   */
  ResultatAffectation affecter(DemandeAffectation demande);

  /**
   * Affectation demandée.
   *
   * @param materielId matériel
   * @param chantierId chantier destinataire
   * @param besoinId besoin à l'origine
   * @param dateDebut début ({@code dateBesoin})
   * @param dateFinPrevue fin prévue ({@code dateBesoin + dureeUtilisationJours})
   */
  record DemandeAffectation(
      UUID materielId,
      UUID chantierId,
      UUID besoinId,
      LocalDate dateDebut,
      LocalDate dateFinPrevue) {}

  /** Résultat d'une demande d'affectation. */
  sealed interface ResultatAffectation {

    /**
     * Matériel affecté.
     *
     * @param affectationId affectation créée par ms-suivi
     */
    record Affecte(UUID affectationId) implements ResultatAffectation {}

    /**
     * Affectation refusée (conflit de planning, matériel indisponible…).
     *
     * @param raison code ou motif retourné
     */
    record Refuse(String raison) implements ResultatAffectation {}
  }
}
