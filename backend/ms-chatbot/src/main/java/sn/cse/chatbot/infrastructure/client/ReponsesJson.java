package sn.cse.chatbot.infrastructure.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Sous-ensembles des DTO des services interrogés (docs/API_CONTRACT.md §2 à §5). Les champs
 * inconnus sont ignorés : le chatbot reste compatible avec l'évolution des contrats.
 */
final class ReponsesJson {

  private ReponsesJson() {}

  record Page<T>(List<T> content) {
    List<T> elements() {
      return content == null ? List.of() : content;
    }
  }

  record Materiel(
      String id,
      String code,
      String designation,
      String categorie,
      String etat,
      String chantierActuelId,
      String chantierActuelNom,
      Double latitude,
      Double longitude,
      LocalDate prochaineMaintenance,
      Boolean maintenanceDue) {}

  record Maintenance(
      String id,
      String numeroOt,
      String materielId,
      String materielCode,
      String materielDesignation,
      String type,
      String statut,
      LocalDate datePlanifiee,
      LocalDate dateFin,
      String description,
      BigDecimal cout) {}

  record Affectation(
      String id,
      String materielId,
      String materielCode,
      String materielDesignation,
      String chantierId,
      String chantierNom,
      LocalDate dateDebut,
      LocalDate dateFinPrevue,
      String statut) {}

  record Chantier(String id, String code, String nom, String localisation, String statut) {}

  record Besoin(
      String id,
      String reference,
      String designation,
      Integer quantite,
      String statut,
      String priorite,
      String chantierNom,
      LocalDate dateBesoin,
      String motifRejet,
      String validateurNom) {}

  record Contrat(
      String id,
      String numero,
      String objet,
      String fournisseurNom,
      String statut,
      BigDecimal montant,
      LocalDate dateLivraisonPrevue,
      LocalDate dateLivraisonReelle,
      String chantierNom) {}

  record AppelOffre(
      String id,
      String reference,
      String objet,
      String statut,
      Instant dateLimite,
      Integer nombreOffres,
      BigDecimal montantAttribue) {}

  record Classement(
      Integer rang,
      String fournisseurId,
      String raisonSociale,
      BigDecimal scorePerformance,
      BigDecimal tauxLivraisonALHeure,
      Integer nombreLitiges,
      List<String> specialites) {}
}
