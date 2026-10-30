package sn.cse.suivi.application.service;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;
import sn.cse.suivi.application.fake.Memoire;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Coordonnees;
import sn.cse.suivi.domain.model.DescriptionMateriel;
import sn.cse.suivi.domain.model.DonneesChantier;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Periode;
import sn.cse.suivi.domain.model.TypeEngin;
import sn.cse.suivi.domain.service.PolitiqueAffectation;

/** Socle des tests de cas d'utilisation : ports en mémoire et jeux de données. */
abstract class ServiceTestSupport {

  static final UUID RM_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
  static final UUID CHEF_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

  final Memoire m = new Memoire();

  static CurrentUser rm() {
    return new CurrentUser(
        RM_ID, "Ibrahima Fall", "rm@cse.sn", Set.of(Roles.RESPONSABLE_MATERIEL), Set.of(), null);
  }

  static CurrentUser chef(UUID... chantiers) {
    return new CurrentUser(
        CHEF_ID,
        "Mamadou Diop",
        "chef@cse.sn",
        Set.of(Roles.CHEF_CHANTIER),
        Set.of(chantiers),
        null);
  }

  static CurrentUser dg() {
    return new CurrentUser(
        UUID.randomUUID(),
        "Aminata Sow",
        "dg@cse.sn",
        Set.of(Roles.DIRECTION_GENERALE),
        Set.of(),
        null);
  }

  static DonneesChantier donneesChantier(String code) {
    return new DonneesChantier(
        code,
        "Chantier " + code,
        "Dakar",
        "Dakar",
        new Coordonnees(14.7, -17.4),
        new BigDecimal("1000000000"),
        new BigDecimal("1200000000"),
        Memoire.AUJOURDHUI.minusDays(100),
        null,
        CHEF_ID,
        "Mamadou Diop");
  }

  Chantier chantier(String code) {
    return m.chantiers.save(Chantier.ouvrir(donneesChantier(code)));
  }

  TypeEngin type(Integer heures, Integer jours) {
    return m.types.save(
        TypeEngin.creer(
            new DonneesTypeEngin(
                "PELLE_HYD",
                "Pelle hydraulique",
                CategorieMateriel.MATERIEL_ROULANT,
                heures,
                jours,
                true)));
  }

  static DescriptionMateriel description(String code, UUID typeId) {
    return new DescriptionMateriel(
        code,
        null,
        "Pelle hydraulique 20 t",
        CategorieMateriel.MATERIEL_ROULANT,
        typeId,
        "Caterpillar",
        "320 GC",
        new BigDecimal("350000"),
        new BigDecimal("145000000"),
        Memoire.AUJOURDHUI.minusDays(30));
  }

  Materiel materiel(String code) {
    TypeEngin type = type(500, 180);
    return m.materiels.save(
        Materiel.enregistrer(
            description(code, type.getId()),
            type,
            1000,
            Materiel.Origine.AUCUNE,
            Memoire.AUJOURDHUI));
  }

  /** Affecte directement (hors cas d'utilisation) : renvoie l'affectation enregistrée. */
  Affectation affecter(Materiel materiel, Chantier chantier) {
    Affectation a =
        m.affectations.save(
            PolitiqueAffectation.affecter(
                materiel,
                chantier,
                null,
                new Periode(Memoire.AUJOURDHUI.minusDays(10), Memoire.AUJOURDHUI.plusDays(50)),
                java.util.List.of()));
    m.materiels.save(materiel);
    return a;
  }
}
