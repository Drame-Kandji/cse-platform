package sn.cse.reporting.domain.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.security.Roles;

/**
 * Profils de tableau de bord (contrat §6, B.13). L'ordre de déclaration est l'ordre de priorité du
 * « profil principal » retenu quand aucun profil n'est demandé.
 */
public enum ProfilTableau {
  /** Direction Générale : toutes les sections, tous les profils accessibles. */
  DG("Direction Générale", Roles.DIRECTION_GENERALE, EnumSet.allOf(Section.class)),
  /** Direction Administrative et Financière. */
  DAF(
      "Direction Administrative et Financière",
      Roles.DAF,
      EnumSet.of(Section.FINANCES, Section.ACHATS, Section.CHANTIERS)),
  /** Responsable des Achats. */
  RESPONSABLE_ACHATS(
      "Responsable des Achats",
      Roles.RESPONSABLE_ACHATS,
      EnumSet.of(Section.ACHATS, Section.FOURNISSEURS)),
  /** Responsable Matériel. */
  RESPONSABLE_MATERIEL(
      "Responsable Matériel",
      Roles.RESPONSABLE_MATERIEL,
      EnumSet.of(Section.PARC, Section.BESOINS, Section.CHANTIERS)),
  /** Chef de chantier : données limitées à ses chantiers ({@code chantier_ids}). */
  CHEF_CHANTIER(
      "Chef de chantier",
      Roles.CHEF_CHANTIER,
      EnumSet.of(Section.BESOINS, Section.PARC, Section.CHANTIERS));

  /** Alias accepté pour {@link #DG} : le code du rôle Keycloak. */
  static final String ALIAS_DG = Roles.DIRECTION_GENERALE;

  private final String libelle;
  private final String role;
  private final Set<Section> sections;

  ProfilTableau(String libelle, String role, Set<Section> sections) {
    this.libelle = libelle;
    this.role = role;
    this.sections = Set.copyOf(sections);
  }

  /**
   * @return libellé français du profil
   */
  public String libelle() {
    return libelle;
  }

  /**
   * @return sections remplies pour ce profil
   */
  public Set<Section> sections() {
    return sections;
  }

  /**
   * @param section section testée
   * @return vrai si le profil affiche cette section
   */
  public boolean affiche(Section section) {
    return sections.contains(section);
  }

  /**
   * Profils accessibles à un utilisateur : la Direction Générale accède à tous les profils, les
   * autres rôles à leur seul profil.
   *
   * @param roles rôles realm de l'utilisateur
   * @return profils autorisés, par ordre de priorité (vide si aucun)
   */
  public static List<ProfilTableau> autorisesPour(Set<String> roles) {
    if (roles.contains(Roles.DIRECTION_GENERALE)) {
      return List.of(values());
    }
    return Arrays.stream(values()).filter(p -> roles.contains(p.role)).toList();
  }

  /**
   * @param code code demandé ({@code DG}, {@code DIRECTION_GENERALE}, {@code DAF}…)
   * @return le profil
   * @throws InvalidRequestException si le code est inconnu
   */
  public static ProfilTableau depuis(String code) {
    String c = code.strip().toUpperCase(Locale.ROOT);
    if (ALIAS_DG.equals(c)) {
      return DG;
    }
    return Arrays.stream(values())
        .filter(p -> p.name().equals(c))
        .findFirst()
        .orElseThrow(
            () ->
                new InvalidRequestException(
                    "PROFIL_INCONNU",
                    "Profil « "
                        + code
                        + " » inconnu. Valeurs : DG, DAF, RESPONSABLE_ACHATS, RESPONSABLE_MATERIEL,"
                        + " CHEF_CHANTIER."));
  }
}
