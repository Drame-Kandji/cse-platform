package sn.cse.fournisseurs.support;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;
import sn.cse.fournisseurs.application.command.FournisseurCommand;
import sn.cse.fournisseurs.domain.model.Auteur;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.CoordonneesBancaires;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.ProfilFournisseur;
import sn.cse.fournisseurs.domain.service.ParametresScore;
import sn.cse.fournisseurs.domain.service.ScorePondere;

/** Données de test partagées. */
public final class Fixtures {

  public static final Instant MAINTENANT = Instant.parse("2026-10-30T09:00:00Z");
  public static final Clock HORLOGE = Clock.fixed(MAINTENANT, ZoneOffset.UTC);
  public static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 30);

  public static final UUID RA_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
  public static final UUID DG_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
  public static final Auteur RA = new Auteur(RA_ID, "Fatou Sarr");
  public static final Auteur DG = new Auteur(DG_ID, "Aminata Sow");

  private Fixtures() {}

  public static ParametresScore parametres() {
    return new ParametresScore(
        30,
        25,
        20,
        10,
        15,
        20,
        BigDecimal.valueOf(5),
        BigDecimal.valueOf(2),
        BigDecimal.valueOf(50),
        BigDecimal.valueOf(40));
  }

  public static ScorePondere politique() {
    return new ScorePondere(parametres());
  }

  public static ProfilFournisseur profil(
      String raisonSociale, String ninea, CategorieMateriel... specialites) {
    return new ProfilFournisseur(
        raisonSociale,
        ninea,
        "SN-DKR-2015-B-12345",
        "Km 4,5 boulevard du Centenaire",
        "Dakar",
        "Sénégal",
        "+221 33 849 12 34",
        "Contact@Exemple.SN",
        "Abdoulaye Mbaye",
        "Directeur commercial",
        specialites.length == 0
            ? EnumSet.of(CategorieMateriel.MATERIEL_ROULANT)
            : EnumSet.of(specialites[0], specialites),
        List.of("ISO 9001:2015"));
  }

  public static Fournisseur actif() {
    return Fournisseur.referencer(
        profil("Sahel Engins SA", "005123456 2G3"),
        new CoordonneesBancaires("Banque Sahélienne", "SN08 SN01 0015 2000 0485 0000 3035"),
        BigDecimal.valueOf(50).setScale(1),
        AUJOURDHUI);
  }

  public static Fournisseur actif(String nom, String ninea, CategorieMateriel... specialites) {
    return Fournisseur.referencer(
        profil(nom, ninea, specialites), null, BigDecimal.valueOf(50).setScale(1), AUJOURDHUI);
  }

  public static Fournisseur enAttente() {
    return Fournisseur.inscrire(
        profil("Baobab Logistique", "008789012 2E7"),
        null,
        BigDecimal.valueOf(50).setScale(1),
        AUJOURDHUI);
  }

  public static CurrentUser utilisateur(UUID id, String nom, String... roles) {
    return new CurrentUser(id, nom, "test@cse.sn", Set.of(roles), Set.of(), null);
  }

  public static CurrentUser ra() {
    return utilisateur(RA_ID, "Fatou Sarr", Roles.RESPONSABLE_ACHATS);
  }

  public static CurrentUser dg() {
    return utilisateur(DG_ID, "Aminata Sow", Roles.DIRECTION_GENERALE);
  }

  public static CurrentUser rm() {
    return utilisateur(
        UUID.fromString("00000000-0000-0000-0000-000000000201"),
        "Ibrahima Fall",
        Roles.RESPONSABLE_MATERIEL);
  }

  public static CurrentUser daf() {
    return utilisateur(
        UUID.fromString("00000000-0000-0000-0000-000000000401"), "Ousmane Ba", Roles.DAF);
  }

  public static FournisseurCommand commande(String nom, String ninea) {
    return new FournisseurCommand(
        nom,
        ninea,
        "SN-DKR-2015-B-12345",
        "Km 4,5 boulevard du Centenaire",
        "Dakar",
        "Sénégal",
        "+221 33 849 12 34",
        "contact@exemple.sn",
        "Abdoulaye Mbaye",
        "Directeur commercial",
        EnumSet.of(CategorieMateriel.MATERIEL_ROULANT),
        List.of("ISO 9001:2015"),
        "Banque Sahélienne",
        "SN08SN0100152000048500003035");
  }
}
