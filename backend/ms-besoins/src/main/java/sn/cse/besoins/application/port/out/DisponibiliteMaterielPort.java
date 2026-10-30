package sn.cse.besoins.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import sn.cse.besoins.domain.model.CategorieMateriel;

/** Interrogation de la disponibilité du parc (ms-suivi). */
public interface DisponibiliteMaterielPort {

  /**
   * @param demande critères
   * @return matériels disponibles sur la période
   * @throws sn.cse.besoins.application.exception.ServiceSuiviIndisponibleException si ms-suivi ne
   *     répond pas
   */
  Disponibilite verifier(DemandeDisponibilite demande);

  /**
   * Critères de disponibilité.
   *
   * @param categorie catégorie
   * @param designation désignation recherchée
   * @param quantite quantité souhaitée
   * @param du début de la période
   * @param au fin de la période
   */
  record DemandeDisponibilite(
      CategorieMateriel categorie, String designation, int quantite, LocalDate du, LocalDate au) {}

  /**
   * Réponse de disponibilité.
   *
   * @param quantiteDisponible nombre de matériels disponibles annoncé
   * @param materielIds identifiants des matériels disponibles
   */
  record Disponibilite(int quantiteDisponible, List<UUID> materielIds) {

    public Disponibilite {
      materielIds = materielIds == null ? List.of() : List.copyOf(materielIds);
    }
  }
}
