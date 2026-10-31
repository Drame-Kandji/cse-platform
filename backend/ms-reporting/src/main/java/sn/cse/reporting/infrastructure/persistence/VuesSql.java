package sn.cse.reporting.infrastructure.persistence;

import static sn.cse.reporting.infrastructure.persistence.Parametres.date;
import static sn.cse.reporting.infrastructure.persistence.Parametres.enumeration;
import static sn.cse.reporting.infrastructure.persistence.Parametres.instant;
import static sn.cse.reporting.infrastructure.persistence.Parametres.uuid;

import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.EtatMateriel;
import sn.cse.reporting.domain.model.GraviteLitige;
import sn.cse.reporting.domain.model.GravitePanne;
import sn.cse.reporting.domain.model.Priorite;
import sn.cse.reporting.domain.model.StatutAffectation;
import sn.cse.reporting.domain.model.StatutAppelOffre;
import sn.cse.reporting.domain.model.StatutBesoin;
import sn.cse.reporting.domain.model.StatutChantier;
import sn.cse.reporting.domain.model.StatutContrat;
import sn.cse.reporting.domain.model.StatutFacture;
import sn.cse.reporting.domain.model.StatutFournisseur;
import sn.cse.reporting.domain.model.StatutLitige;
import sn.cse.reporting.domain.model.StatutMaintenance;
import sn.cse.reporting.domain.model.TypeMaintenance;
import sn.cse.reporting.domain.model.vue.AffectationVue;
import sn.cse.reporting.domain.model.vue.AppelOffreVue;
import sn.cse.reporting.domain.model.vue.BesoinVue;
import sn.cse.reporting.domain.model.vue.ChantierVue;
import sn.cse.reporting.domain.model.vue.ContratVue;
import sn.cse.reporting.domain.model.vue.FactureVue;
import sn.cse.reporting.domain.model.vue.FournisseurVue;
import sn.cse.reporting.domain.model.vue.LitigeVue;
import sn.cse.reporting.domain.model.vue.MaintenanceVue;
import sn.cse.reporting.domain.model.vue.MaterielVue;
import sn.cse.reporting.domain.model.vue.PanneVue;
import sn.cse.reporting.domain.model.vue.ParcInstantane;

/** Requêtes d'écriture (upsert) et correspondances ligne → vue, partagées par les adaptateurs. */
final class VuesSql {

  /** Clé « hors chantier » de {@code parc_instantanes.chantier_cle}. */
  static final UUID HORS_CHANTIER = new UUID(0L, 0L);

  private VuesSql() {}

  static final String UPSERT_CHANTIER =
      """
      INSERT INTO chantiers (id, code, nom, region, statut, budget, montant_marche, chef_chantier_id,
        date_debut, date_fin_prevue, complet, maj_le)
      VALUES (:id, :code, :nom, :region, CAST(:statut AS statut_chantier), :budget, :montantMarche,
        :chef, :dateDebut, :dateFinPrevue, :complet, :majLe)
      ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, nom = EXCLUDED.nom,
        region = EXCLUDED.region, statut = EXCLUDED.statut, budget = EXCLUDED.budget,
        montant_marche = EXCLUDED.montant_marche, chef_chantier_id = EXCLUDED.chef_chantier_id,
        date_debut = EXCLUDED.date_debut, date_fin_prevue = EXCLUDED.date_fin_prevue,
        complet = EXCLUDED.complet, maj_le = EXCLUDED.maj_le
      """;

  /** Référence : insertion si absent, sinon seul un nom manquant est complété. */
  static final String REFERENCE_CHANTIER =
      """
      INSERT INTO chantiers (id, nom, budget, montant_marche, complet, maj_le)
      VALUES (:id, :nom, 0, 0, FALSE, :majLe)
      ON CONFLICT (id) DO UPDATE SET nom = coalesce(chantiers.nom, EXCLUDED.nom)
      """;

  static Parametres params(ChantierVue v) {
    return new Parametres()
        .uuid("id", v.id())
        .texte("code", v.code())
        .texte("nom", v.nom())
        .texte("region", v.region())
        .enumeration("statut", v.statut())
        .montant("budget", v.budget())
        .montant("montantMarche", v.montantMarche())
        .uuid("chef", v.chefChantierId())
        .date("dateDebut", v.dateDebut())
        .date("dateFinPrevue", v.dateFinPrevue())
        .booleen("complet", v.complet())
        .instant("majLe", v.majLe());
  }

  static final RowMapper<ChantierVue> CHANTIER =
      (rs, i) ->
          new ChantierVue(
              uuid(rs, "id"),
              rs.getString("code"),
              rs.getString("nom"),
              rs.getString("region"),
              enumeration(rs, "statut", StatutChantier.class),
              rs.getBigDecimal("budget"),
              rs.getBigDecimal("montant_marche"),
              uuid(rs, "chef_chantier_id"),
              date(rs, "date_debut"),
              date(rs, "date_fin_prevue"),
              rs.getBoolean("complet"),
              instant(rs, "maj_le"));

  static final String UPSERT_MATERIEL =
      """
      INSERT INTO materiels (id, code, designation, categorie, etat, chantier_id, tarif_journalier,
        valeur_acquisition, alerte_maintenance_le, etat_maj_le)
      VALUES (:id, :code, :designation, CAST(:categorie AS categorie_materiel),
        CAST(:etat AS etat_materiel), :chantier, :tarif, :valeur, :alerte, :etatMajLe)
      ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, designation = EXCLUDED.designation,
        categorie = EXCLUDED.categorie, etat = EXCLUDED.etat, chantier_id = EXCLUDED.chantier_id,
        tarif_journalier = EXCLUDED.tarif_journalier, valeur_acquisition = EXCLUDED.valeur_acquisition,
        alerte_maintenance_le = EXCLUDED.alerte_maintenance_le, etat_maj_le = EXCLUDED.etat_maj_le
      """;

  static Parametres params(MaterielVue v) {
    return new Parametres()
        .uuid("id", v.id())
        .texte("code", v.code())
        .texte("designation", v.designation())
        .enumeration("categorie", v.categorie())
        .enumeration("etat", v.etat())
        .uuid("chantier", v.chantierId())
        .montant("tarif", v.tarifJournalier())
        .montant("valeur", v.valeurAcquisition())
        .instant("alerte", v.alerteMaintenanceLe())
        .instant("etatMajLe", v.etatMajLe());
  }

  static final RowMapper<MaterielVue> MATERIEL =
      (rs, i) ->
          new MaterielVue(
              uuid(rs, "id"),
              rs.getString("code"),
              rs.getString("designation"),
              enumeration(rs, "categorie", CategorieMateriel.class),
              enumeration(rs, "etat", EtatMateriel.class),
              uuid(rs, "chantier_id"),
              rs.getBigDecimal("tarif_journalier"),
              rs.getBigDecimal("valeur_acquisition"),
              instant(rs, "alerte_maintenance_le"),
              instant(rs, "etat_maj_le"));

  static final String UPSERT_AFFECTATION =
      """
      INSERT INTO affectations (id, materiel_id, categorie, chantier_id, tarif_journalier, besoin_id,
        date_debut, date_fin_prevue, date_fin_reelle, statut, maj_le)
      VALUES (:id, :materiel, CAST(:categorie AS categorie_materiel), :chantier, :tarif, :besoin,
        :dateDebut, :dateFinPrevue, :dateFinReelle, CAST(:statut AS statut_affectation), :majLe)
      ON CONFLICT (id) DO UPDATE SET materiel_id = EXCLUDED.materiel_id,
        categorie = EXCLUDED.categorie, chantier_id = EXCLUDED.chantier_id,
        tarif_journalier = EXCLUDED.tarif_journalier, besoin_id = EXCLUDED.besoin_id,
        date_debut = EXCLUDED.date_debut, date_fin_prevue = EXCLUDED.date_fin_prevue,
        date_fin_reelle = EXCLUDED.date_fin_reelle, statut = EXCLUDED.statut, maj_le = EXCLUDED.maj_le
      """;

  static Parametres params(AffectationVue v) {
    return new Parametres()
        .uuid("id", v.id())
        .uuid("materiel", v.materielId())
        .enumeration("categorie", v.categorie())
        .uuid("chantier", v.chantierId())
        .montant("tarif", v.tarifJournalier())
        .uuid("besoin", v.besoinId())
        .date("dateDebut", v.dateDebut())
        .date("dateFinPrevue", v.dateFinPrevue())
        .date("dateFinReelle", v.dateFinReelle())
        .enumeration("statut", v.statut())
        .instant("majLe", v.majLe());
  }

  static final RowMapper<AffectationVue> AFFECTATION =
      (rs, i) ->
          new AffectationVue(
              uuid(rs, "id"),
              uuid(rs, "materiel_id"),
              enumeration(rs, "categorie", CategorieMateriel.class),
              uuid(rs, "chantier_id"),
              rs.getBigDecimal("tarif_journalier"),
              uuid(rs, "besoin_id"),
              date(rs, "date_debut"),
              date(rs, "date_fin_prevue"),
              date(rs, "date_fin_reelle"),
              enumeration(rs, "statut", StatutAffectation.class),
              instant(rs, "maj_le"));

  static final String UPSERT_MAINTENANCE =
      """
      INSERT INTO maintenances (id, numero_ot, materiel_id, chantier_id, type, statut, cout,
        date_planifiee, date_debut, date_fin, cloturee_le, maj_le)
      VALUES (:id, :numeroOt, :materiel, :chantier, CAST(:type AS type_maintenance),
        CAST(:statut AS statut_maintenance), :cout, :datePlanifiee, :dateDebut, :dateFin,
        :clotureeLe, :majLe)
      ON CONFLICT (id) DO UPDATE SET numero_ot = EXCLUDED.numero_ot,
        materiel_id = EXCLUDED.materiel_id, chantier_id = EXCLUDED.chantier_id, type = EXCLUDED.type,
        statut = EXCLUDED.statut, cout = EXCLUDED.cout, date_planifiee = EXCLUDED.date_planifiee,
        date_debut = EXCLUDED.date_debut, date_fin = EXCLUDED.date_fin,
        cloturee_le = EXCLUDED.cloturee_le, maj_le = EXCLUDED.maj_le
      """;

  static Parametres params(MaintenanceVue v) {
    return new Parametres()
        .uuid("id", v.id())
        .texte("numeroOt", v.numeroOt())
        .uuid("materiel", v.materielId())
        .uuid("chantier", v.chantierId())
        .enumeration("type", v.type())
        .enumeration("statut", v.statut())
        .montant("cout", v.cout())
        .date("datePlanifiee", v.datePlanifiee())
        .date("dateDebut", v.dateDebut())
        .date("dateFin", v.dateFin())
        .instant("clotureeLe", v.clotureeLe())
        .instant("majLe", v.majLe());
  }

  static final RowMapper<MaintenanceVue> MAINTENANCE =
      (rs, i) ->
          new MaintenanceVue(
              uuid(rs, "id"),
              rs.getString("numero_ot"),
              uuid(rs, "materiel_id"),
              uuid(rs, "chantier_id"),
              enumeration(rs, "type", TypeMaintenance.class),
              enumeration(rs, "statut", StatutMaintenance.class),
              rs.getBigDecimal("cout"),
              date(rs, "date_planifiee"),
              date(rs, "date_debut"),
              date(rs, "date_fin"),
              instant(rs, "cloturee_le"),
              instant(rs, "maj_le"));

  static final String UPSERT_PANNE =
      """
      INSERT INTO pannes (id, materiel_id, chantier_id, gravite, signalee_le, resolue_le)
      VALUES (:id, :materiel, :chantier, CAST(:gravite AS gravite_panne), :signaleeLe, :resolueLe)
      ON CONFLICT (id) DO UPDATE SET materiel_id = EXCLUDED.materiel_id,
        chantier_id = EXCLUDED.chantier_id, gravite = EXCLUDED.gravite,
        signalee_le = EXCLUDED.signalee_le, resolue_le = EXCLUDED.resolue_le
      """;

  static Parametres params(PanneVue v) {
    return new Parametres()
        .uuid("id", v.id())
        .uuid("materiel", v.materielId())
        .uuid("chantier", v.chantierId())
        .enumeration("gravite", v.gravite())
        .instant("signaleeLe", v.signaleeLe())
        .instant("resolueLe", v.resolueLe());
  }

  static final RowMapper<PanneVue> PANNE =
      (rs, i) ->
          new PanneVue(
              uuid(rs, "id"),
              uuid(rs, "materiel_id"),
              uuid(rs, "chantier_id"),
              enumeration(rs, "gravite", GravitePanne.class),
              instant(rs, "signalee_le"),
              instant(rs, "resolue_le"));

  static final String UPSERT_BESOIN =
      """
      INSERT INTO besoins (id, reference, chantier_id, chantier_nom, categorie, quantite, priorite,
        statut, date_besoin, soumis_le, decide_le, premier_vu_le, date_soumission, maj_le)
      VALUES (:id, :reference, :chantier, :chantierNom, CAST(:categorie AS categorie_materiel),
        :quantite, CAST(:priorite AS priorite), CAST(:statut AS statut_besoin), :dateBesoin,
        :soumisLe, :decideLe, :premierVuLe, :dateSoumission, :majLe)
      ON CONFLICT (id) DO UPDATE SET reference = EXCLUDED.reference,
        chantier_id = EXCLUDED.chantier_id, chantier_nom = EXCLUDED.chantier_nom,
        categorie = EXCLUDED.categorie, quantite = EXCLUDED.quantite, priorite = EXCLUDED.priorite,
        statut = EXCLUDED.statut, date_besoin = EXCLUDED.date_besoin, soumis_le = EXCLUDED.soumis_le,
        decide_le = EXCLUDED.decide_le, premier_vu_le = EXCLUDED.premier_vu_le,
        date_soumission = EXCLUDED.date_soumission, maj_le = EXCLUDED.maj_le
      """;

  static Parametres params(BesoinVue v) {
    return new Parametres()
        .uuid("id", v.id())
        .texte("reference", v.reference())
        .uuid("chantier", v.chantierId())
        .texte("chantierNom", v.chantierNom())
        .enumeration("categorie", v.categorie())
        .entier("quantite", v.quantite())
        .enumeration("priorite", v.priorite())
        .enumeration("statut", v.statut())
        .date("dateBesoin", v.dateBesoin())
        .instant("soumisLe", v.soumisLe())
        .instant("decideLe", v.decideLe())
        .instant("premierVuLe", v.premierVuLe())
        .date("dateSoumission", v.dateSoumission().orElse(null))
        .instant("majLe", v.majLe());
  }

  static final RowMapper<BesoinVue> BESOIN =
      (rs, i) ->
          new BesoinVue(
              uuid(rs, "id"),
              rs.getString("reference"),
              uuid(rs, "chantier_id"),
              rs.getString("chantier_nom"),
              enumeration(rs, "categorie", CategorieMateriel.class),
              rs.getInt("quantite"),
              enumeration(rs, "priorite", Priorite.class),
              enumeration(rs, "statut", StatutBesoin.class),
              date(rs, "date_besoin"),
              instant(rs, "soumis_le"),
              instant(rs, "decide_le"),
              instant(rs, "premier_vu_le"),
              instant(rs, "maj_le"));

  static final String UPSERT_APPEL_OFFRE =
      """
      INSERT INTO appels_offres (id, reference, chantier_id, besoin_id, categorie, statut,
        premier_vu_le, date_limite, date_publication, montant_attribue, fournisseur_retenu_id, maj_le)
      VALUES (:id, :reference, :chantier, :besoin, CAST(:categorie AS categorie_materiel),
        CAST(:statut AS statut_appel_offre), :premierVuLe, :dateLimite, :datePublication, :montant,
        :fournisseur, :majLe)
      ON CONFLICT (id) DO UPDATE SET reference = EXCLUDED.reference,
        chantier_id = EXCLUDED.chantier_id, besoin_id = EXCLUDED.besoin_id,
        categorie = EXCLUDED.categorie, statut = EXCLUDED.statut,
        premier_vu_le = EXCLUDED.premier_vu_le, date_limite = EXCLUDED.date_limite,
        date_publication = EXCLUDED.date_publication, montant_attribue = EXCLUDED.montant_attribue,
        fournisseur_retenu_id = EXCLUDED.fournisseur_retenu_id, maj_le = EXCLUDED.maj_le
      """;

  static Parametres params(AppelOffreVue v) {
    return new Parametres()
        .uuid("id", v.id())
        .texte("reference", v.reference())
        .uuid("chantier", v.chantierId())
        .uuid("besoin", v.besoinId())
        .enumeration("categorie", v.categorie())
        .enumeration("statut", v.statut())
        .instant("premierVuLe", v.premierVuLe())
        .instant("dateLimite", v.dateLimite())
        .date("datePublication", v.datePublication())
        .montant("montant", v.montantAttribue())
        .uuid("fournisseur", v.fournisseurRetenuId())
        .instant("majLe", v.majLe());
  }

  static final RowMapper<AppelOffreVue> APPEL_OFFRE =
      (rs, i) ->
          new AppelOffreVue(
              uuid(rs, "id"),
              rs.getString("reference"),
              uuid(rs, "chantier_id"),
              uuid(rs, "besoin_id"),
              enumeration(rs, "categorie", CategorieMateriel.class),
              enumeration(rs, "statut", StatutAppelOffre.class),
              instant(rs, "premier_vu_le"),
              instant(rs, "date_limite"),
              rs.getBigDecimal("montant_attribue"),
              uuid(rs, "fournisseur_retenu_id"),
              instant(rs, "maj_le"));

  static final String UPSERT_CONTRAT =
      """
      INSERT INTO contrats (id, numero, appel_offre_id, chantier_id, fournisseur_id, fournisseur_nom,
        montant, statut, date_signature, date_engagement, date_livraison_prevue,
        date_livraison_reelle, maj_le)
      VALUES (:id, :numero, :appelOffre, :chantier, :fournisseur, :fournisseurNom, :montant,
        CAST(:statut AS statut_contrat), :dateSignature, :dateEngagement, :dateLivraisonPrevue,
        :dateLivraisonReelle, :majLe)
      ON CONFLICT (id) DO UPDATE SET numero = EXCLUDED.numero,
        appel_offre_id = EXCLUDED.appel_offre_id, chantier_id = EXCLUDED.chantier_id,
        fournisseur_id = EXCLUDED.fournisseur_id, fournisseur_nom = EXCLUDED.fournisseur_nom,
        montant = EXCLUDED.montant, statut = EXCLUDED.statut,
        date_signature = EXCLUDED.date_signature, date_engagement = EXCLUDED.date_engagement,
        date_livraison_prevue = EXCLUDED.date_livraison_prevue,
        date_livraison_reelle = EXCLUDED.date_livraison_reelle, maj_le = EXCLUDED.maj_le
      """;

  static Parametres params(ContratVue v) {
    return new Parametres()
        .uuid("id", v.id())
        .texte("numero", v.numero())
        .uuid("appelOffre", v.appelOffreId())
        .uuid("chantier", v.chantierId())
        .uuid("fournisseur", v.fournisseurId())
        .texte("fournisseurNom", v.fournisseurNom())
        .montant("montant", v.montant())
        .enumeration("statut", v.statut())
        .date("dateSignature", v.dateSignature())
        .date("dateEngagement", v.dateEngagement())
        .date("dateLivraisonPrevue", v.dateLivraisonPrevue())
        .date("dateLivraisonReelle", v.dateLivraisonReelle())
        .instant("majLe", v.majLe());
  }

  static final RowMapper<ContratVue> CONTRAT =
      (rs, i) ->
          new ContratVue(
              uuid(rs, "id"),
              rs.getString("numero"),
              uuid(rs, "appel_offre_id"),
              uuid(rs, "chantier_id"),
              uuid(rs, "fournisseur_id"),
              rs.getString("fournisseur_nom"),
              rs.getBigDecimal("montant"),
              enumeration(rs, "statut", StatutContrat.class),
              date(rs, "date_signature"),
              date(rs, "date_engagement"),
              date(rs, "date_livraison_prevue"),
              date(rs, "date_livraison_reelle"),
              instant(rs, "maj_le"));

  static final String UPSERT_FACTURE =
      """
      INSERT INTO factures (id, numero, contrat_id, fournisseur_id, montant_ttc, statut,
        date_echeance, date_reception, date_mandatement, date_paiement, maj_le)
      VALUES (:id, :numero, :contrat, :fournisseur, :montant, CAST(:statut AS statut_facture),
        :dateEcheance, :dateReception, :dateMandatement, :datePaiement, :majLe)
      ON CONFLICT (id) DO UPDATE SET numero = EXCLUDED.numero, contrat_id = EXCLUDED.contrat_id,
        fournisseur_id = EXCLUDED.fournisseur_id, montant_ttc = EXCLUDED.montant_ttc,
        statut = EXCLUDED.statut, date_echeance = EXCLUDED.date_echeance,
        date_reception = EXCLUDED.date_reception, date_mandatement = EXCLUDED.date_mandatement,
        date_paiement = EXCLUDED.date_paiement, maj_le = EXCLUDED.maj_le
      """;

  static Parametres params(FactureVue v) {
    return new Parametres()
        .uuid("id", v.id())
        .texte("numero", v.numero())
        .uuid("contrat", v.contratId())
        .uuid("fournisseur", v.fournisseurId())
        .montant("montant", v.montantTtc())
        .enumeration("statut", v.statut())
        .date("dateEcheance", v.dateEcheance())
        .date("dateReception", v.dateReception())
        .date("dateMandatement", v.dateMandatement())
        .date("datePaiement", v.datePaiement())
        .instant("majLe", v.majLe());
  }

  static final RowMapper<FactureVue> FACTURE =
      (rs, i) ->
          new FactureVue(
              uuid(rs, "id"),
              rs.getString("numero"),
              uuid(rs, "contrat_id"),
              uuid(rs, "fournisseur_id"),
              rs.getBigDecimal("montant_ttc"),
              enumeration(rs, "statut", StatutFacture.class),
              date(rs, "date_echeance"),
              date(rs, "date_reception"),
              date(rs, "date_mandatement"),
              date(rs, "date_paiement"),
              instant(rs, "maj_le"));

  static final String UPSERT_FOURNISSEUR =
      """
      INSERT INTO fournisseurs (id, raison_sociale, statut, score_performance, complet, maj_le)
      VALUES (:id, :raisonSociale, CAST(:statut AS statut_fournisseur), :score, :complet, :majLe)
      ON CONFLICT (id) DO UPDATE SET raison_sociale = EXCLUDED.raison_sociale,
        statut = EXCLUDED.statut, score_performance = EXCLUDED.score_performance,
        complet = EXCLUDED.complet, maj_le = EXCLUDED.maj_le
      """;

  /** Référence : insertion si absent, sinon seule une raison sociale manquante est complétée. */
  static final String REFERENCE_FOURNISSEUR =
      """
      INSERT INTO fournisseurs (id, raison_sociale, score_performance, complet, maj_le)
      VALUES (:id, :raisonSociale, 0, FALSE, :majLe)
      ON CONFLICT (id) DO UPDATE SET
        raison_sociale = coalesce(fournisseurs.raison_sociale, EXCLUDED.raison_sociale)
      """;

  static Parametres params(FournisseurVue v) {
    return new Parametres()
        .uuid("id", v.id())
        .texte("raisonSociale", v.raisonSociale())
        .enumeration("statut", v.statut())
        .montant("score", v.scorePerformance())
        .booleen("complet", v.complet())
        .instant("majLe", v.majLe());
  }

  static final RowMapper<FournisseurVue> FOURNISSEUR =
      (rs, i) ->
          new FournisseurVue(
              uuid(rs, "id"),
              rs.getString("raison_sociale"),
              enumeration(rs, "statut", StatutFournisseur.class),
              rs.getBigDecimal("score_performance"),
              rs.getBoolean("complet"),
              instant(rs, "maj_le"));

  static final String UPSERT_LITIGE =
      """
      INSERT INTO litiges (id, reference, fournisseur_id, contrat_id, gravite, statut, ouvert_le,
        maj_le)
      VALUES (:id, :reference, :fournisseur, :contrat, CAST(:gravite AS gravite_litige),
        CAST(:statut AS statut_litige), :ouvertLe, :majLe)
      ON CONFLICT (id) DO UPDATE SET reference = EXCLUDED.reference,
        fournisseur_id = EXCLUDED.fournisseur_id, contrat_id = EXCLUDED.contrat_id,
        gravite = EXCLUDED.gravite, statut = EXCLUDED.statut, ouvert_le = EXCLUDED.ouvert_le,
        maj_le = EXCLUDED.maj_le
      """;

  static Parametres params(LitigeVue v) {
    return new Parametres()
        .uuid("id", v.id())
        .texte("reference", v.reference())
        .uuid("fournisseur", v.fournisseurId())
        .uuid("contrat", v.contratId())
        .enumeration("gravite", v.gravite())
        .enumeration("statut", v.statut())
        .instant("ouvertLe", v.ouvertLe())
        .instant("majLe", v.majLe());
  }

  static final RowMapper<LitigeVue> LITIGE =
      (rs, i) ->
          new LitigeVue(
              uuid(rs, "id"),
              rs.getString("reference"),
              uuid(rs, "fournisseur_id"),
              uuid(rs, "contrat_id"),
              enumeration(rs, "gravite", GraviteLitige.class),
              enumeration(rs, "statut", StatutLitige.class),
              instant(rs, "ouvert_le"),
              instant(rs, "maj_le"));

  static final String INSERT_INSTANTANE =
      """
      INSERT INTO parc_instantanes (jour, categorie, chantier_cle, total, disponibles, affectes,
        en_maintenance, reformes)
      VALUES (:jour, CAST(:categorie AS categorie_materiel), :chantier, :total, :disponibles,
        :affectes, :enMaintenance, :reformes)
      """;

  static Parametres params(ParcInstantane v) {
    return new Parametres()
        .date("jour", v.jour())
        .enumeration("categorie", v.categorie())
        .uuid("chantier", v.chantierId() == null ? HORS_CHANTIER : v.chantierId())
        .entier("total", v.total())
        .entier("disponibles", v.disponibles())
        .entier("affectes", v.affectes())
        .entier("enMaintenance", v.enMaintenance())
        .entier("reformes", v.reformes());
  }

  static final RowMapper<ParcInstantane> INSTANTANE =
      (rs, i) -> {
        UUID cle = uuid(rs, "chantier_cle");
        return new ParcInstantane(
            date(rs, "jour"),
            enumeration(rs, "categorie", CategorieMateriel.class),
            HORS_CHANTIER.equals(cle) ? null : cle,
            rs.getLong("total"),
            rs.getLong("disponibles"),
            rs.getLong("affectes"),
            rs.getLong("en_maintenance"),
            rs.getLong("reformes"));
      };
}
