package sn.cse.reporting.domain.model.vue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.StatutAppelOffre;

/**
 * Vue d'un appel d'offres.
 *
 * @param id identifiant
 * @param reference référence ({@code AO-2026-001})
 * @param chantierId chantier concerné
 * @param besoinId besoin d'origine
 * @param categorie catégorie
 * @param statut statut courant
 * @param premierVuLe instant du premier événement reçu ({@code AO_PUBLIE} en temps normal)
 * @param dateLimite date limite de dépôt des offres
 * @param montantAttribue montant attribué
 * @param fournisseurRetenuId fournisseur retenu
 * @param majLe instant du dernier événement appliqué
 */
public record AppelOffreVue(
    UUID id,
    String reference,
    UUID chantierId,
    UUID besoinId,
    CategorieMateriel categorie,
    StatutAppelOffre statut,
    Instant premierVuLe,
    Instant dateLimite,
    BigDecimal montantAttribue,
    UUID fournisseurRetenuId,
    Instant majLe) {

  public AppelOffreVue {
    Objects.requireNonNull(id, "id");
  }

  /**
   * @param recu vue construite à partir de l'événement reçu
   * @return la vue fusionnée (instant de publication = premier connu)
   */
  public AppelOffreVue fusionnerAvec(AppelOffreVue recu) {
    AppelOffreVue r = Horodatage.plusRecent(recu.majLe, majLe) ? recu : this;
    return new AppelOffreVue(
        id,
        r.reference,
        r.chantierId,
        r.besoinId,
        r.categorie,
        r.statut,
        Horodatage.min(premierVuLe, recu.premierVuLe),
        r.dateLimite,
        r.montantAttribue,
        r.fournisseurRetenuId,
        Horodatage.max(majLe, recu.majLe));
  }

  /**
   * Jour de publication : jour de {@code AO_PUBLIE}, borné par la date limite (une publication est
   * toujours antérieure à la date limite ; la borne corrige les AO historiques publiés a
   * posteriori).
   *
   * @return le jour de publication
   */
  public LocalDate datePublication() {
    return Horodatage.min(Horodatage.jour(premierVuLe), Horodatage.jour(dateLimite));
  }
}
