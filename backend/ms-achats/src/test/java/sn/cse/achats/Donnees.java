package sn.cse.achats;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import sn.cse.achats.domain.model.Acteur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.CritereEvaluation;
import sn.cse.achats.domain.model.CriterePondere;
import sn.cse.achats.domain.model.FournisseurConnu;
import sn.cse.achats.domain.model.NouvelAppelOffre;
import sn.cse.achats.domain.model.OrigineBesoin;
import sn.cse.achats.domain.model.ParametresContrat;
import sn.cse.achats.domain.model.PonderationCriteres;
import sn.cse.achats.domain.model.StatutFournisseur;
import sn.cse.achats.domain.model.TypeAppelOffre;

/** Jeu de données partagé par les tests unitaires du domaine et de l'application. */
public final class Donnees {

  public static final Instant T0 = Instant.parse("2026-10-01T08:00:00Z");
  public static final Instant LIMITE = T0.plus(Duration.ofDays(10));
  public static final BigDecimal SEUIL = new BigDecimal("150000000");

  public static final Acteur RA =
      new Acteur(uuid("00000000-0000-0000-0000-000000000301"), "Fatou Sarr");
  public static final Acteur RM =
      new Acteur(uuid("00000000-0000-0000-0000-000000000201"), "Ibrahima Fall");
  public static final Acteur DAF =
      new Acteur(uuid("00000000-0000-0000-0000-000000000401"), "Ousmane Ba");
  public static final Acteur DG =
      new Acteur(uuid("00000000-0000-0000-0000-000000000501"), "Aminata Sow");
  public static final Acteur REPRESENTANT_F1 =
      new Acteur(uuid("00000000-0000-0000-0000-000000000701"), "Moussa Kane");

  public static final FournisseurConnu F1 =
      fournisseur(1, "Sahel Engins SA", StatutFournisseur.ACTIF);
  public static final FournisseurConnu F2 =
      fournisseur(2, "Teranga Équipements BTP", StatutFournisseur.ACTIF);
  public static final FournisseurConnu F3 =
      fournisseur(3, "Dakar Outillage Pro", StatutFournisseur.ACTIF);
  public static final FournisseurConnu SUSPENDU =
      fournisseur(6, "Ndiambour Matériaux", StatutFournisseur.SUSPENDU);
  public static final FournisseurConnu BLACKLISTE =
      fournisseur(8, "Fournisseur Blacklisté", StatutFournisseur.BLACKLISTE);
  public static final FournisseurConnu EN_ATTENTE =
      fournisseur(7, "Baobab Logistique", StatutFournisseur.EN_ATTENTE_VALIDATION);

  public static final OrigineBesoin BESOIN =
      new OrigineBesoin(
          uuid("b0000000-0000-0000-0000-000000000001"),
          "BES-2026-0001",
          uuid("c0000000-0000-0000-0000-000000000001"),
          "Extension autoroute Dakar-AIBD",
          "Pelle hydraulique 20 t");

  public static final ParametresContrat PARAMETRES =
      new ParametresContrat(
          new BigDecimal("0.1"), new BigDecimal("10"), new BigDecimal("0.0025"), 7);

  private Donnees() {}

  public static UUID uuid(String valeur) {
    return UUID.fromString(valeur);
  }

  public static FournisseurConnu fournisseur(int n, String nom, StatutFournisseur statut) {
    return new FournisseurConnu(
        uuid(String.format("f0000000-0000-0000-0000-%012d", n)), nom, statut);
  }

  /** PRIX 40, DELAI 20, QUALITE_TECHNIQUE 40. */
  public static PonderationCriteres grille() {
    return new PonderationCriteres(
        List.of(
            new CriterePondere(CritereEvaluation.PRIX, 40),
            new CriterePondere(CritereEvaluation.DELAI, 20),
            new CriterePondere(CritereEvaluation.QUALITE_TECHNIQUE, 40)));
  }

  public static NouvelAppelOffre nouvelAo(TypeAppelOffre type, List<FournisseurConnu> invites) {
    return new NouvelAppelOffre(
        "Acquisition de deux pelles hydrauliques 20 t",
        "Pelles sur chenilles",
        type,
        BESOIN,
        CategorieMateriel.MATERIEL_ROULANT,
        2,
        "Godet 1,2 m3",
        new BigDecimal("320000000"),
        LIMITE,
        grille(),
        invites,
        RA);
  }

  public static AppelOffre aoPublie() {
    return AppelOffre.publier(
        nouvelAo(TypeAppelOffre.APPEL_OFFRES, List.of(F1, F2)), "AO-2026-001", T0);
  }

  /** AO clôturé avec deux offres : F1 (montant1, 60 j) et F2 (montant2, 45 j). */
  public static AppelOffre aoCloture(String montant1, String montant2) {
    AppelOffre ao = aoPublie();
    ao.deposerOffre(F1, new BigDecimal(montant1), 60, 24, "Komatsu PC210", T0.plusSeconds(3600));
    ao.deposerOffre(F2, new BigDecimal(montant2), 45, 18, "CAT 320", T0.plusSeconds(7200));
    ao.cloturer(LIMITE);
    return ao;
  }

  /** AO attribué à F1 (montant sous le seuil : co-validation RM seule). */
  public static AppelOffre aoAttribue() {
    AppelOffre ao = aoCloture("120000000", "130000000");
    ao.evaluerOffre(
        ao.offreDu(F1.id()).orElseThrow().getId(), Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 80));
    ao.evaluerOffre(
        ao.offreDu(F2.id()).orElseThrow().getId(), Map.of(CritereEvaluation.QUALITE_TECHNIQUE, 70));
    ao.proposerAttribution(
        ao.offreDu(F1.id()).orElseThrow().getId(),
        "Meilleur score",
        RA,
        LIMITE.plusSeconds(60),
        SEUIL);
    ao.coValiderAttribution(true, null, RM, LIMITE.plusSeconds(120));
    return ao;
  }

  public static ContratAchat contratBrouillon() {
    return ContratAchat.generer(aoAttribue(), "CTR-2026-001", PARAMETRES);
  }

  /**
   * Signature en deux temps : le CSE une heure avant, le fournisseur à l'instant donné.
   *
   * @param c contrat BROUILLON
   * @param signatureFournisseur instant de la signature du fournisseur
   * @return le contrat SIGNE
   */
  public static ContratAchat signer(ContratAchat c, Instant signatureFournisseur) {
    c.signerPourLeCse(RA, signatureFournisseur.minusSeconds(3600));
    c.signerPourLeFournisseur(REPRESENTANT_F1, signatureFournisseur);
    return c;
  }
}
