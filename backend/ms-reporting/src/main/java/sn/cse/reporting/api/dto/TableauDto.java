package sn.cse.reporting.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import sn.cse.reporting.domain.model.Tableau;

/**
 * Tableau de bord (contrat §6). Les sections non affichées par le profil sont absentes du JSON.
 *
 * @param profil profil
 * @param du début de période
 * @param au fin de période
 * @param genereLe instant de génération
 * @param parc section parc
 * @param besoins section besoins
 * @param achats section achats
 * @param fournisseurs section fournisseurs
 * @param finances section finances
 * @param chantiers section chantiers
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "Tableau",
    description =
        "Tableau de bord d'un profil. Montants en FCFA entiers, taux en % (0..100, une décimale).")
public record TableauDto(
    @Schema(
            description = "Profil du tableau",
            example = "DG",
            allowableValues = {
              "DG",
              "DAF",
              "RESPONSABLE_ACHATS",
              "RESPONSABLE_MATERIEL",
              "CHEF_CHANTIER"
            })
        String profil,
    @Schema(description = "Premier jour de la période", example = "2026-08-01") LocalDate du,
    @Schema(description = "Dernier jour de la période", example = "2026-10-30") LocalDate au,
    @Schema(description = "Instant de génération (UTC)", example = "2026-10-30T09:15:00Z")
        Instant genereLe,
    @Schema(description = "Parc matériel (DG, RM, CHEF)", nullable = true) ParcDto parc,
    @Schema(description = "Besoins soumis sur la période (DG, RM, CHEF)", nullable = true)
        BesoinsDto besoins,
    @Schema(description = "Achats (DG, RA, DAF)", nullable = true) AchatsDto achats,
    @Schema(description = "Fournisseurs (DG, RA)", nullable = true) FournisseursDto fournisseurs,
    @Schema(description = "Finances (DG, DAF)", nullable = true) FinancesDto finances,
    @Schema(description = "Indicateurs par chantier (DG, RM, DAF, CHEF)", nullable = true)
        List<ChantierDto> chantiers) {

  /**
   * @param t tableau du domaine
   * @return le DTO
   */
  public static TableauDto depuis(Tableau t) {
    return new TableauDto(
        t.profil().name(),
        t.du(),
        t.au(),
        t.genereLe(),
        t.parc() == null ? null : ParcDto.depuis(t.parc()),
        t.besoins() == null ? null : BesoinsDto.depuis(t.besoins()),
        t.achats() == null ? null : AchatsDto.depuis(t.achats()),
        t.fournisseurs() == null ? null : FournisseursDto.depuis(t.fournisseurs()),
        t.finances() == null ? null : FinancesDto.depuis(t.finances()),
        t.chantiers() == null ? null : t.chantiers().stream().map(ChantierDto::depuis).toList());
  }

  /** Parc matériel. */
  @Schema(name = "TableauParc", description = "Indicateurs du parc matériel")
  public record ParcDto(
      @Schema(description = "Matériels du périmètre", example = "120") long total,
      @Schema(description = "Disponibles", example = "45") long disponibles,
      @Schema(description = "Affectés", example = "60") long affectes,
      @Schema(description = "En maintenance", example = "10") long enMaintenance,
      @Schema(description = "Réformés", example = "5") long reformes,
      @Schema(
              description =
                  "Opérationnels = total − en maintenance − réformés (disponibles ou affectés)",
              example = "105")
          long operationnels,
      @Schema(
              description =
                  "Disponibilité opérationnelle = (total − en maintenance − réformés) / (total −"
                      + " réformés), en %",
              example = "91.3")
          BigDecimal tauxDisponibilite,
      @Schema(
              description = "Jours affectés / jours opérationnels sur la période, en %",
              example = "62.4")
          BigDecimal tauxUtilisation,
      @Schema(description = "Maintenances en cours", example = "4") long maintenancesEnCours,
      @Schema(description = "Pannes non résolues", example = "3") long pannesOuvertes,
      @Schema(description = "Alertes de maintenance préventive non traitées", example = "2")
          long alertesMaintenance,
      @Schema(description = "Détail par catégorie (toutes les catégories)")
          List<ParcCategorieDto> parCategorie) {

    static ParcDto depuis(Tableau.Parc p) {
      return new ParcDto(
          p.total(),
          p.disponibles(),
          p.affectes(),
          p.enMaintenance(),
          p.reformes(),
          p.operationnels(),
          p.tauxDisponibilite(),
          p.tauxUtilisation(),
          p.maintenancesEnCours(),
          p.pannesOuvertes(),
          p.alertesMaintenance(),
          p.parCategorie().stream()
              .map(
                  c ->
                      new ParcCategorieDto(
                          c.categorie().name(),
                          c.total(),
                          c.disponibles(),
                          c.affectes(),
                          c.enMaintenance(),
                          c.operationnels(),
                          c.tauxUtilisation()))
              .toList());
    }
  }

  /** Parc d'une catégorie. */
  @Schema(name = "TableauParcCategorie", description = "Parc d'une catégorie de matériel")
  public record ParcCategorieDto(
      @Schema(
              description = "Catégorie",
              example = "MATERIEL_ROULANT",
              allowableValues = {
                "MATERIEL_ROULANT",
                "OUTILLAGE",
                "EQUIPEMENT_INFORMATIQUE",
                "CONSOMMABLE"
              })
          String categorie,
      @Schema(description = "Matériels", example = "40") long total,
      @Schema(description = "Disponibles", example = "12") long disponibles,
      @Schema(description = "Affectés", example = "25") long affectes,
      @Schema(description = "En maintenance", example = "3") long enMaintenance,
      @Schema(description = "Opérationnels (total − en maintenance − réformés)", example = "37")
          long operationnels,
      @Schema(description = "Taux d'utilisation, en %", example = "71.0")
          BigDecimal tauxUtilisation) {}

  /** Besoins. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(name = "TableauBesoins", description = "Besoins soumis sur la période (statut courant)")
  public record BesoinsDto(
      @Schema(description = "Besoins soumis", example = "38") long total,
      @Schema(description = "En attente", example = "6") long enAttente,
      @Schema(description = "Validés (orientation en attente)", example = "2") long valides,
      @Schema(description = "Rejetés", example = "4") long rejetes,
      @Schema(description = "Affectés directement", example = "15") long affectesDirect,
      @Schema(description = "En achat", example = "11") long enAchat,
      @Schema(
              description = "Délai moyen soumission → décision, en heures",
              example = "18.5",
              nullable = true)
          BigDecimal delaiMoyenTraitementHeures,
      @Schema(
              description = "Répartition par priorité (NORMAL, URGENT, CRITIQUE)",
              example = "{\"NORMAL\": 25, \"URGENT\": 10, \"CRITIQUE\": 3}")
          Map<String, Long> parPriorite) {

    static BesoinsDto depuis(Tableau.Besoins b) {
      Map<String, Long> priorites = new LinkedHashMap<>();
      b.parPriorite().forEach((k, v) -> priorites.put(k.name(), v));
      return new BesoinsDto(
          b.total(),
          b.enAttente(),
          b.valides(),
          b.rejetes(),
          b.affectesDirect(),
          b.enAchat(),
          b.delaiMoyenTraitementHeures(),
          priorites);
    }
  }

  /** Achats. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(name = "TableauAchats", description = "Appels d'offres, contrats et livraisons")
  public record AchatsDto(
      @Schema(description = "AO publiés sur la période encore ouverts", example = "3")
          long aoPublies,
      @Schema(description = "AO publiés sur la période clôturés", example = "2") long aoClotures,
      @Schema(description = "AO publiés sur la période attribués", example = "7") long aoAttribues,
      @Schema(description = "Contrats ni livrés ni clôturés", example = "5") long contratsEnCours,
      @Schema(description = "Contrats non livrés dont la date prévue est dépassée", example = "1")
          long livraisonsEnRetard,
      @Schema(
              description = "Délai moyen publication AO → livraison réelle, en jours",
              example = "41.5",
              nullable = true)
          BigDecimal delaiMoyenApprovisionnementJours,
      @Schema(description = "Livraisons à l'heure, en %", example = "85.7", nullable = true)
          BigDecimal tauxLivraisonALHeure) {

    static AchatsDto depuis(Tableau.Achats a) {
      return new AchatsDto(
          a.aoPublies(),
          a.aoClotures(),
          a.aoAttribues(),
          a.contratsEnCours(),
          a.livraisonsEnRetard(),
          a.delaiMoyenApprovisionnementJours(),
          a.tauxLivraisonALHeure());
    }
  }

  /** Fournisseurs. */
  @Schema(name = "TableauFournisseurs", description = "Performance des fournisseurs")
  public record FournisseursDto(
      @Schema(description = "Fournisseurs actifs", example = "5") long actifs,
      @Schema(description = "Score moyen des actifs (0..100)", example = "76.4")
          BigDecimal scoreMoyen,
      @Schema(description = "Litiges ouverts", example = "2") long litigesOuverts,
      @Schema(description = "Meilleurs fournisseurs actifs") List<FournisseurTopDto> top) {

    static FournisseursDto depuis(Tableau.Fournisseurs f) {
      return new FournisseursDto(
          f.actifs(),
          f.scoreMoyen(),
          f.litigesOuverts(),
          f.top().stream()
              .map(
                  c ->
                      new FournisseurTopDto(
                          c.fournisseurId(),
                          c.raisonSociale(),
                          c.scorePerformance(),
                          c.nombreLitiges(),
                          c.tauxLivraisonALHeure()))
              .toList());
    }
  }

  /** Ligne du classement. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(name = "TableauFournisseurTop", description = "Fournisseur du classement")
  public record FournisseurTopDto(
      @Schema(description = "Fournisseur", example = "f0000000-0000-0000-0000-000000000001")
          UUID fournisseurId,
      @Schema(description = "Raison sociale", example = "Sahel Engins SA") String raisonSociale,
      @Schema(description = "Score de performance (0..100)", example = "88.5")
          BigDecimal scorePerformance,
      @Schema(description = "Litiges (tous statuts)", example = "0") long nombreLitiges,
      @Schema(description = "Livraisons à l'heure, en %", example = "100.0", nullable = true)
          BigDecimal tauxLivraisonALHeure) {}

  /** Finances. */
  @Schema(name = "TableauFinances", description = "Budget, engagements et coûts de la période")
  public record FinancesDto(
      @Schema(description = "Budget des chantiers actifs (FCFA)", example = "4500000000")
          BigDecimal budgetTotal,
      @Schema(description = "Engagements validés sur la période (FCFA)", example = "820000000")
          BigDecimal montantEngage,
      @Schema(description = "Factures mandatées sur la période (FCFA)", example = "510000000")
          BigDecimal montantMandate,
      @Schema(description = "Factures payées sur la période (FCFA)", example = "430000000")
          BigDecimal montantPaye,
      @Schema(description = "Montant engagé / budget, en %", example = "18.2")
          BigDecimal tauxExecutionBudget,
      @Schema(description = "Coût des maintenances clôturées (FCFA)", example = "36500000")
          BigDecimal coutMaintenance,
      @Schema(description = "Jours affectés × tarif journalier (FCFA)", example = "152000000")
          BigDecimal coutExploitation) {

    static FinancesDto depuis(Tableau.Finances f) {
      return new FinancesDto(
          f.budgetTotal(),
          f.montantEngage(),
          f.montantMandate(),
          f.montantPaye(),
          f.tauxExecutionBudget(),
          f.coutMaintenance(),
          f.coutExploitation());
    }
  }

  /** Indicateurs d'un chantier. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(name = "TableauChantier", description = "Indicateurs d'un chantier sur la période")
  public record ChantierDto(
      @Schema(description = "Chantier", example = "c0000000-0000-0000-0000-000000000001")
          UUID chantierId,
      @Schema(description = "Nom", example = "Extension autoroute Dakar–AIBD") String chantierNom,
      @Schema(description = "Région", example = "Dakar", nullable = true) String region,
      @Schema(description = "Budget (FCFA)", example = "1200000000") BigDecimal budget,
      @Schema(description = "Montant du marché (FCFA)", example = "1500000000")
          BigDecimal montantMarche,
      @Schema(description = "Engagements validés sur la période (FCFA)", example = "250000000")
          BigDecimal montantEngage,
      @Schema(description = "Coût d'exploitation sur la période (FCFA)", example = "48000000")
          BigDecimal coutExploitation,
      @Schema(description = "Coût de maintenance sur la période (FCFA)", example = "6500000")
          BigDecimal coutMaintenance,
      @Schema(
              description = "Marché proratisé sur la période − exploitation − maintenance (FCFA)",
              example = "130500000")
          BigDecimal marge,
      @Schema(description = "Matériels actuellement affectés", example = "14")
          long materielsAffectes,
      @Schema(description = "Taux d'utilisation des matériels affectés, en %", example = "88.2")
          BigDecimal tauxUtilisation,
      @Schema(description = "Besoins en attente", example = "2") long besoinsEnAttente) {

    static ChantierDto depuis(Tableau.Chantier c) {
      return new ChantierDto(
          c.chantierId(),
          c.chantierNom(),
          c.region(),
          c.budget(),
          c.montantMarche(),
          c.montantEngage(),
          c.coutExploitation(),
          c.coutMaintenance(),
          c.marge(),
          c.materielsAffectes(),
          c.tauxUtilisation(),
          c.besoinsEnAttente());
    }
  }
}
