package sn.cse.reporting.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tableau de bord d'un profil (contrat §6). Une section non affichée par le profil vaut {@code
 * null}. Montants en FCFA entiers ; taux en pourcentage (0..100, une décimale).
 *
 * @param profil profil
 * @param du premier jour de la période
 * @param au dernier jour de la période
 * @param genereLe instant de génération
 * @param parc section parc
 * @param besoins section besoins
 * @param achats section achats
 * @param fournisseurs section fournisseurs
 * @param finances section finances
 * @param chantiers section chantiers
 */
public record Tableau(
    ProfilTableau profil,
    LocalDate du,
    LocalDate au,
    Instant genereLe,
    Parc parc,
    Besoins besoins,
    Achats achats,
    Fournisseurs fournisseurs,
    Finances finances,
    List<Chantier> chantiers) {

  /**
   * Indicateurs du parc.
   *
   * @param total matériels du périmètre
   * @param disponibles disponibles
   * @param affectes affectés
   * @param enMaintenance en maintenance
   * @param reformes réformés
   * @param operationnels matériels opérationnels (total − en maintenance − réformés : disponibles
   *     ou affectés)
   * @param tauxDisponibilite disponibilité opérationnelle = opérationnels / (total − réformés)
   * @param tauxUtilisation jours affectés / jours opérationnels sur la période
   * @param maintenancesEnCours interventions en cours
   * @param pannesOuvertes pannes non résolues
   * @param alertesMaintenance alertes de maintenance préventive non traitées
   * @param parCategorie détail par catégorie
   */
  public record Parc(
      long total,
      long disponibles,
      long affectes,
      long enMaintenance,
      long reformes,
      long operationnels,
      BigDecimal tauxDisponibilite,
      BigDecimal tauxUtilisation,
      long maintenancesEnCours,
      long pannesOuvertes,
      long alertesMaintenance,
      List<ParcCategorie> parCategorie) {}

  /**
   * Parc d'une catégorie.
   *
   * @param categorie catégorie
   * @param total matériels
   * @param disponibles disponibles
   * @param affectes affectés
   * @param enMaintenance en maintenance
   * @param operationnels opérationnels (total − en maintenance − réformés)
   * @param tauxUtilisation taux d'utilisation sur la période
   */
  public record ParcCategorie(
      CategorieMateriel categorie,
      long total,
      long disponibles,
      long affectes,
      long enMaintenance,
      long operationnels,
      BigDecimal tauxUtilisation) {}

  /**
   * Indicateurs des besoins soumis sur la période.
   *
   * @param total besoins soumis
   * @param enAttente en attente de décision
   * @param valides validés (orientation en attente)
   * @param rejetes rejetés
   * @param affectesDirect satisfaits par le parc
   * @param enAchat orientés vers un achat
   * @param delaiMoyenTraitementHeures délai moyen soumission → décision (null sans décision)
   * @param parPriorite répartition par priorité (toutes les priorités présentes)
   */
  public record Besoins(
      long total,
      long enAttente,
      long valides,
      long rejetes,
      long affectesDirect,
      long enAchat,
      BigDecimal delaiMoyenTraitementHeures,
      Map<Priorite, Long> parPriorite) {

    public Besoins {
      EnumMap<Priorite, Long> complet = new EnumMap<>(Priorite.class);
      for (Priorite p : Priorite.values()) {
        complet.put(p, parPriorite == null ? 0L : parPriorite.getOrDefault(p, 0L));
      }
      parPriorite = java.util.Collections.unmodifiableMap(complet);
    }
  }

  /**
   * Indicateurs achats.
   *
   * @param aoPublies AO publiés sur la période encore ouverts
   * @param aoClotures AO publiés sur la période clôturés (non attribués)
   * @param aoAttribues AO publiés sur la période attribués
   * @param contratsEnCours contrats ni livrés ni clôturés
   * @param livraisonsEnRetard contrats non livrés dont la date prévue est dépassée
   * @param delaiMoyenApprovisionnementJours publication de l'AO → livraison réelle (null sans
   *     livraison)
   * @param tauxLivraisonALHeure livraisons à l'heure / livraisons de la période (null sans
   *     livraison mesurable)
   */
  public record Achats(
      long aoPublies,
      long aoClotures,
      long aoAttribues,
      long contratsEnCours,
      long livraisonsEnRetard,
      BigDecimal delaiMoyenApprovisionnementJours,
      BigDecimal tauxLivraisonALHeure) {}

  /**
   * Indicateurs fournisseurs.
   *
   * @param actifs fournisseurs actifs
   * @param scoreMoyen score moyen des actifs
   * @param litigesOuverts litiges ouverts
   * @param top meilleurs fournisseurs actifs
   */
  public record Fournisseurs(
      long actifs, BigDecimal scoreMoyen, long litigesOuverts, List<FournisseurClasse> top) {}

  /**
   * Ligne du classement des fournisseurs.
   *
   * @param fournisseurId fournisseur
   * @param raisonSociale raison sociale
   * @param scorePerformance score 0..100
   * @param nombreLitiges litiges (tous statuts)
   * @param tauxLivraisonALHeure ponctualité (null sans livraison mesurable)
   */
  public record FournisseurClasse(
      UUID fournisseurId,
      String raisonSociale,
      BigDecimal scorePerformance,
      long nombreLitiges,
      BigDecimal tauxLivraisonALHeure) {}

  /**
   * Indicateurs financiers sur la période.
   *
   * @param budgetTotal budgets des chantiers actifs
   * @param montantEngage engagements validés
   * @param montantMandate factures mandatées
   * @param montantPaye factures payées
   * @param tauxExecutionBudget engagé / budget
   * @param coutMaintenance coût des maintenances clôturées
   * @param coutExploitation jours affectés × tarif journalier
   */
  public record Finances(
      BigDecimal budgetTotal,
      BigDecimal montantEngage,
      BigDecimal montantMandate,
      BigDecimal montantPaye,
      BigDecimal tauxExecutionBudget,
      BigDecimal coutMaintenance,
      BigDecimal coutExploitation) {}

  /**
   * Indicateurs d'un chantier.
   *
   * @param chantierId chantier
   * @param chantierNom nom
   * @param region région
   * @param budget budget
   * @param montantMarche montant du marché
   * @param montantEngage engagements validés sur la période
   * @param coutExploitation coût d'exploitation sur la période
   * @param coutMaintenance coût de maintenance sur la période
   * @param marge marché proratisé − coûts (voir README)
   * @param materielsAffectes matériels actuellement affectés
   * @param tauxUtilisation taux d'utilisation des matériels affectés au chantier
   * @param besoinsEnAttente besoins en attente (actuels)
   */
  public record Chantier(
      UUID chantierId,
      String chantierNom,
      String region,
      BigDecimal budget,
      BigDecimal montantMarche,
      BigDecimal montantEngage,
      BigDecimal coutExploitation,
      BigDecimal coutMaintenance,
      BigDecimal marge,
      long materielsAffectes,
      BigDecimal tauxUtilisation,
      long besoinsEnAttente) {}
}
