package sn.cse.reporting.domain.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import sn.cse.common.exception.ForbiddenOperationException;
import sn.cse.common.security.CurrentUser;
import sn.cse.reporting.domain.model.Indicateur;
import sn.cse.reporting.domain.model.Perimetre;
import sn.cse.reporting.domain.model.ProfilTableau;

/**
 * Qui voit quel tableau de bord (contrat §6, B.13) :
 *
 * <ul>
 *   <li>la Direction Générale accède à tous les profils ; chaque autre rôle à son seul profil ;
 *   <li>le paramètre {@code profil} choisit parmi les profils autorisés, sinon 403 ;
 *   <li>un chef de chantier est limité à ses {@code chantier_ids} ; un {@code chantierId} hors de
 *       ses chantiers est refusé (403).
 * </ul>
 */
public final class PolitiqueAcces {

  private PolitiqueAcces() {}

  /**
   * Résultat du contrôle d'accès.
   *
   * @param profil profil retenu
   * @param perimetre chantiers visibles
   */
  public record Acces(ProfilTableau profil, Perimetre perimetre) {}

  /**
   * @param utilisateur utilisateur courant
   * @param profilDemande profil demandé ({@code null} : profil principal)
   * @param chantierId filtre de chantier facultatif
   * @return le profil et le périmètre
   * @throws ForbiddenOperationException si le profil ou le chantier n'est pas autorisé
   */
  public static Acces pourTableau(CurrentUser utilisateur, String profilDemande, UUID chantierId) {
    List<ProfilTableau> autorises = profilsAutorises(utilisateur);
    ProfilTableau profil;
    if (profilDemande == null || profilDemande.isBlank()) {
      profil = autorises.get(0);
    } else {
      profil = ProfilTableau.depuis(profilDemande);
      if (!autorises.contains(profil)) {
        throw new ForbiddenOperationException(
            "PROFIL_NON_AUTORISE",
            "Le tableau de bord « " + profil.libelle() + " » n'est pas accessible à votre rôle.");
      }
    }
    return new Acces(profil, perimetre(utilisateur, chantierId));
  }

  /**
   * @param utilisateur utilisateur courant
   * @param indicateur indicateur demandé
   * @param chantierId filtre de chantier facultatif
   * @return le périmètre visible
   * @throws ForbiddenOperationException si aucun profil autorisé n'affiche l'indicateur
   */
  public static Perimetre pourSerie(
      CurrentUser utilisateur, Indicateur indicateur, UUID chantierId) {
    if (!indicateur.accessiblePour(profilsAutorises(utilisateur))) {
      throw new ForbiddenOperationException(
          "INDICATEUR_NON_AUTORISE",
          "L'indicateur « " + indicateur.libelle() + " » n'est pas accessible à votre rôle.");
    }
    return perimetre(utilisateur, chantierId);
  }

  private static List<ProfilTableau> profilsAutorises(CurrentUser utilisateur) {
    List<ProfilTableau> autorises = ProfilTableau.autorisesPour(utilisateur.roles());
    if (autorises.isEmpty()) {
      throw new ForbiddenOperationException(
          "PROFIL_NON_AUTORISE", "Aucun tableau de bord n'est accessible à votre rôle.");
    }
    return autorises;
  }

  private static Perimetre perimetre(CurrentUser utilisateur, UUID chantierId) {
    if (utilisateur.estRestreintAuxChantiers()) {
      if (chantierId == null) {
        return Perimetre.chantiers(utilisateur.chantierIds());
      }
      if (!utilisateur.chantierIds().contains(chantierId)) {
        throw new ForbiddenOperationException(
            "CHANTIER_NON_AUTORISE", "Ce chantier ne fait pas partie de vos chantiers.");
      }
    }
    return chantierId == null ? Perimetre.tous() : Perimetre.chantiers(Set.of(chantierId));
  }
}
