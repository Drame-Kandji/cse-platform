package sn.cse.besoins;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import sn.cse.besoins.domain.model.Acteur;
import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.DescriptionBesoin;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;

/** Jeux d'essai partagés par les tests unitaires. */
public final class Fixtures {

  public static final Instant MAINTENANT = Instant.parse("2026-10-30T09:00:00Z");
  public static final LocalDate AUJOURDHUI = LocalDate.parse("2026-10-30");
  public static final UUID CHANTIER_1 = UUID.fromString("c0000000-0000-0000-0000-000000000001");
  public static final UUID CHANTIER_2 = UUID.fromString("c0000000-0000-0000-0000-000000000002");
  public static final UUID CHANTIER_3 = UUID.fromString("c0000000-0000-0000-0000-000000000003");
  public static final UUID CHEF_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  public static final UUID RM_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

  public static final Acteur CHEF =
      new Acteur(CHEF_ID, "Mamadou Diop", Set.of(Roles.CHEF_CHANTIER));
  public static final Acteur RM =
      new Acteur(RM_ID, "Ibrahima Fall", Set.of(Roles.RESPONSABLE_MATERIEL));
  public static final Acteur RA =
      new Acteur(
          UUID.fromString("00000000-0000-0000-0000-000000000301"),
          "Fatou Sarr",
          Set.of(Roles.RESPONSABLE_ACHATS));

  public static final CurrentUser USER_CHEF =
      new CurrentUser(
          CHEF_ID,
          "Mamadou Diop",
          "chef.chantier@cse.sn",
          Set.of(Roles.CHEF_CHANTIER),
          Set.of(CHANTIER_1, CHANTIER_2),
          null);
  public static final CurrentUser USER_RM =
      new CurrentUser(
          RM_ID,
          "Ibrahima Fall",
          "resp.materiel@cse.sn",
          Set.of(Roles.RESPONSABLE_MATERIEL),
          Set.of(),
          null);
  public static final CurrentUser USER_RA =
      new CurrentUser(
          RA.id(),
          "Fatou Sarr",
          "resp.achats@cse.sn",
          Set.of(Roles.RESPONSABLE_ACHATS),
          Set.of(),
          null);

  private Fixtures() {}

  public static DescriptionBesoin description(int quantite, Priorite priorite) {
    return new DescriptionBesoin(
        "Pelle hydraulique 20 t",
        CategorieMateriel.MATERIEL_ROULANT,
        null,
        quantite,
        "Godet 1 m³",
        priorite,
        30,
        AUJOURDHUI.plusDays(5),
        priorite == Priorite.NORMAL ? null : "Chantier à l'arrêt");
  }

  public static BesoinMateriel besoinEnAttente(int quantite) {
    return BesoinMateriel.soumettre(
        UUID.randomUUID(),
        "BES-2026-0001",
        CHANTIER_1,
        "Extension autoroute Dakar-AIBD",
        CHEF,
        description(quantite, Priorite.NORMAL),
        MAINTENANT);
  }
}
