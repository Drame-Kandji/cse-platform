package sn.cse.besoins.infrastructure.client;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Sous-ensemble des DTO de ms-suivi (contrat d'API §2) utiles à ms-besoins. */
final class SuiviDtos {

  /** Statut d'un chantier en cours. */
  static final String CHANTIER_EN_COURS = "EN_COURS";

  private SuiviDtos() {}

  record Chantier(UUID id, String nom, String statut) {}

  record Materiel(UUID id, String code, String designation) {}

  record Disponibilite(
      boolean disponible, int quantiteDemandee, int quantiteDisponible, List<Materiel> materiels) {}

  record DemandeAffectation(
      UUID chantierId, UUID besoinId, LocalDate dateDebut, LocalDate dateFinPrevue) {}

  record Affectation(UUID id, UUID materielId) {}

  record Probleme(String code, String detail) {}
}
