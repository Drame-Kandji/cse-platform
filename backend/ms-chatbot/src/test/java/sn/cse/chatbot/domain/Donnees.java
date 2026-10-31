package sn.cse.chatbot.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import sn.cse.chatbot.domain.model.AffectationInfo;
import sn.cse.chatbot.domain.model.AppelOffreInfo;
import sn.cse.chatbot.domain.model.BesoinInfo;
import sn.cse.chatbot.domain.model.ChantierInfo;
import sn.cse.chatbot.domain.model.ContratInfo;
import sn.cse.chatbot.domain.model.FournisseurInfo;
import sn.cse.chatbot.domain.model.MaintenanceInfo;
import sn.cse.chatbot.domain.model.MaterielInfo;
import sn.cse.chatbot.domain.model.Section;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;

/** Jeux de données de test (identifiants du référentiel de démonstration). */
public final class Donnees {

  public static final UUID CHANTIER_THIES = UUID.fromString("c0000000-0000-0000-0000-000000000002");

  public static final MaterielInfo NIVELEUSE =
      new MaterielInfo(
          "m1",
          "NIV-002",
          "Niveleuse CAT 140K",
          "MATERIEL_ROULANT",
          "AFFECTE",
          "Contournement de Thiès RN2",
          CHANTIER_THIES.toString(),
          14.791,
          -16.9256,
          LocalDate.of(2026, 11, 20),
          false);
  public static final MaterielInfo PELLE =
      new MaterielInfo(
          "m2",
          "PEL-001",
          "Pelle hydraulique 20 t",
          "MATERIEL_ROULANT",
          "DISPONIBLE",
          null,
          null,
          null,
          null,
          null,
          true);
  public static final BesoinInfo BESOIN_REJETE =
      new BesoinInfo(
          "b1",
          "BES-2026-0012",
          "Compacteur",
          1,
          "REJETE",
          "URGENT",
          "Pont de la lagune",
          LocalDate.of(2026, 11, 5),
          "Doublon avec BES-2026-0010",
          "Ibrahima Fall");
  public static final BesoinInfo BESOIN_ATTENTE =
      new BesoinInfo(
          "b2",
          "BES-2026-0013",
          "Bétonnière",
          2,
          "EN_ATTENTE",
          "NORMAL",
          "Thiès",
          LocalDate.of(2026, 11, 9),
          null,
          null);
  public static final ContratInfo CONTRAT =
      new ContratInfo(
          "k1",
          "CTR-2026-003",
          "Fourniture de 2 pelles",
          "Sahel Engins SA",
          "EN_LIVRAISON",
          new BigDecimal("180000000"),
          LocalDate.of(2026, 11, 15),
          null,
          "Thiès");
  public static final AppelOffreInfo AO =
      new AppelOffreInfo(
          "a1",
          "AO-2026-001",
          "Acquisition de compacteurs",
          "PUBLIE",
          Instant.parse("2026-11-20T12:00:00Z"),
          3,
          new BigDecimal("90000000"));
  public static final FournisseurInfo SAHEL =
      new FournisseurInfo(
          "f1",
          1,
          "Sahel Engins SA",
          new BigDecimal("87.5"),
          new BigDecimal("92"),
          0,
          List.of("MATERIEL_ROULANT"));
  public static final MaintenanceInfo VIDANGE =
      new MaintenanceInfo(
          "o1",
          "OT-2026-0007",
          "m1",
          "NIV-002",
          "Niveleuse CAT 140K",
          "PREVENTIVE",
          "CLOTUREE",
          LocalDate.of(2026, 9, 1),
          LocalDate.of(2026, 9, 2),
          "Vidange 500 h",
          new BigDecimal("450000"));
  public static final AffectationInfo AFFECTATION =
      new AffectationInfo(
          "af1",
          "m1",
          "NIV-002",
          "Niveleuse CAT 140K",
          CHANTIER_THIES.toString(),
          "Contournement de Thiès RN2",
          LocalDate.of(2026, 10, 1),
          LocalDate.of(2026, 12, 31),
          "EN_COURS");
  public static final ChantierInfo THIES =
      new ChantierInfo(
          CHANTIER_THIES.toString(),
          "CH-THS-01",
          "Contournement de Thiès RN2",
          "Thiès",
          "EN_COURS");

  public static final Map<Section, Set<String>> ACCES =
      Map.of(
          Section.BESOINS,
              Set.of(
                  Roles.CHEF_CHANTIER,
                  Roles.RESPONSABLE_MATERIEL,
                  Roles.RESPONSABLE_ACHATS,
                  Roles.DIRECTION_GENERALE),
          Section.CONTRATS,
              Set.of(
                  Roles.RESPONSABLE_ACHATS,
                  Roles.RESPONSABLE_MATERIEL,
                  Roles.DAF,
                  Roles.DIRECTION_GENERALE),
          Section.APPELS_OFFRES,
              Set.of(
                  Roles.RESPONSABLE_ACHATS,
                  Roles.RESPONSABLE_MATERIEL,
                  Roles.DAF,
                  Roles.DIRECTION_GENERALE),
          Section.FOURNISSEURS,
              Set.of(
                  Roles.RESPONSABLE_ACHATS,
                  Roles.RESPONSABLE_MATERIEL,
                  Roles.DAF,
                  Roles.DIRECTION_GENERALE));
  public static final Set<String> MONTANTS =
      Set.of(
          Roles.RESPONSABLE_ACHATS,
          Roles.RESPONSABLE_MATERIEL,
          Roles.DAF,
          Roles.DIRECTION_GENERALE);

  private Donnees() {}

  public static CurrentUser user(String... roles) {
    return new CurrentUser(
        UUID.randomUUID(), "Utilisateur", "u@cse.sn", Set.of(roles), Set.of(), null);
  }

  public static CurrentUser chef(UUID... chantiers) {
    return new CurrentUser(
        UUID.randomUUID(),
        "Mamadou Diop",
        "chef@cse.sn",
        Set.of(Roles.CHEF_CHANTIER),
        Set.of(chantiers),
        null);
  }
}
