package sn.cse.suivi.domain.model;

import java.util.Objects;
import java.util.UUID;
import sn.cse.suivi.domain.exception.SuiviErreurs;

/**
 * Référentiel des types d'engins : porte les seuils de maintenance préventive appliqués aux
 * matériels du type.
 */
public final class TypeEngin {

  private final UUID id;
  private DonneesTypeEngin donnees;
  private final Long version;

  /**
   * @param id identifiant
   * @param donnees données
   * @param version version de verrou optimiste
   */
  public record Snapshot(UUID id, DonneesTypeEngin donnees, Long version) {}

  private TypeEngin(Snapshot s) {
    this.id = Objects.requireNonNull(s.id(), "id");
    this.donnees = Objects.requireNonNull(s.donnees(), "donnees");
    this.version = s.version();
  }

  /**
   * @param donnees données saisies
   * @return le nouveau type
   */
  public static TypeEngin creer(DonneesTypeEngin donnees) {
    return creer(UUID.randomUUID(), donnees);
  }

  /**
   * @param id identifiant imposé (référentiel partagé entre services)
   * @param donnees données saisies
   * @return le nouveau type
   */
  public static TypeEngin creer(UUID id, DonneesTypeEngin donnees) {
    return new TypeEngin(new Snapshot(id, donnees, null));
  }

  /**
   * @param snapshot état persistant
   * @return le type reconstitué
   */
  public static TypeEngin reconstituer(Snapshot snapshot) {
    return new TypeEngin(snapshot);
  }

  /**
   * @param nouvelles données modifiées
   * @return vrai si les seuils de maintenance ont changé (à répercuter sur les matériels)
   */
  public boolean modifier(DonneesTypeEngin nouvelles) {
    boolean seuilsChanges =
        !Objects.equals(donnees.seuilMaintenanceHeures(), nouvelles.seuilMaintenanceHeures())
            || !Objects.equals(donnees.seuilMaintenanceJours(), nouvelles.seuilMaintenanceJours());
    this.donnees = nouvelles;
    return seuilsChanges;
  }

  /**
   * Vérifie qu'un matériel de la catégorie donnée peut être rattaché à ce type.
   *
   * @param categorie catégorie du matériel
   */
  public void verifierCompatible(CategorieMateriel categorie) {
    if (!donnees.actif()) {
      throw SuiviErreurs.regle(
          "TYPE_ENGIN_INACTIF", "Le type d'engin " + donnees.code() + " n'est plus actif.");
    }
    if (donnees.categorie() != categorie) {
      throw SuiviErreurs.regle(
          "TYPE_ENGIN_INCOMPATIBLE",
          "Le type d'engin "
              + donnees.code()
              + " relève de la catégorie "
              + donnees.categorie()
              + ", pas de "
              + categorie
              + ".");
    }
  }

  /**
   * @return l'état persistant
   */
  public Snapshot snapshot() {
    return new Snapshot(id, donnees, version);
  }

  public UUID getId() {
    return id;
  }

  public DonneesTypeEngin getDonnees() {
    return donnees;
  }

  public String getCode() {
    return donnees.code();
  }

  public String getLibelle() {
    return donnees.libelle();
  }

  public Long getVersion() {
    return version;
  }
}
