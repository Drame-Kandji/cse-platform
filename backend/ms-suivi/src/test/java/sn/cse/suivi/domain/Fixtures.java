package sn.cse.suivi.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Coordonnees;
import sn.cse.suivi.domain.model.DescriptionMateriel;
import sn.cse.suivi.domain.model.DonneesChantier;
import sn.cse.suivi.domain.model.DonneesTypeEngin;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.TypeEngin;

/** Objets de test du domaine. */
public final class Fixtures {

  /** Date de référence des tests. */
  public static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 30);

  private Fixtures() {}

  /**
   * @param code code du chantier
   * @return données valides
   */
  public static DonneesChantier donneesChantier(String code) {
    return new DonneesChantier(
        code,
        "Extension autoroute Dakar-AIBD",
        "Diamniadio",
        "Dakar",
        new Coordonnees(14.7167, -17.4677),
        new BigDecimal("18500000000"),
        new BigDecimal("21000000000"),
        AUJOURDHUI.minusDays(100),
        AUJOURDHUI.plusDays(300),
        UUID.fromString("00000000-0000-0000-0000-000000000101"),
        "Mamadou Diop");
  }

  /**
   * @return un chantier en cours non enregistré
   */
  public static Chantier chantier() {
    return Chantier.ouvrir(donneesChantier("CH-DKR-01"));
  }

  /**
   * @param seuilHeures seuil horaire
   * @param seuilJours seuil calendaire
   * @return un type d'engin roulant actif
   */
  public static TypeEngin typePelle(Integer seuilHeures, Integer seuilJours) {
    return TypeEngin.creer(
        new DonneesTypeEngin(
            "PELLE_HYD",
            "Pelle hydraulique",
            CategorieMateriel.MATERIEL_ROULANT,
            seuilHeures,
            seuilJours,
            true));
  }

  /**
   * @param code code parc
   * @param typeId type d'engin
   * @return une description valide
   */
  public static DescriptionMateriel description(String code, UUID typeId) {
    return new DescriptionMateriel(
        code,
        "DK-4521-BH",
        "Pelle hydraulique sur chenilles 20 t",
        CategorieMateriel.MATERIEL_ROULANT,
        typeId,
        "Caterpillar",
        "320 GC",
        new BigDecimal("350000"),
        new BigDecimal("145000000"),
        AUJOURDHUI.minusDays(30));
  }

  /**
   * @param type type d'engin (seuils)
   * @return un matériel disponible
   */
  public static Materiel materiel(TypeEngin type) {
    return Materiel.enregistrer(
        description("PEL-CAT320-01", type.getId()),
        type,
        1000,
        Materiel.Origine.AUCUNE,
        AUJOURDHUI);
  }

  /**
   * @return un matériel disponible (seuils 500 h / 180 j)
   */
  public static Materiel materiel() {
    return materiel(typePelle(500, 180));
  }
}
