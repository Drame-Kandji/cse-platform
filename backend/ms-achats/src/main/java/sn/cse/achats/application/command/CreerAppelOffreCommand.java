package sn.cse.achats.application.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.CriterePondere;
import sn.cse.achats.domain.model.TypeAppelOffre;
import sn.cse.common.security.CurrentUser;

/**
 * Publication d'un appel d'offres.
 *
 * @param besoinId besoin d'origine (facultatif)
 * @param objet objet
 * @param description description
 * @param type procédure
 * @param categorie catégorie
 * @param quantite quantité
 * @param specifications spécifications
 * @param budgetEstime budget estimé
 * @param dateLimite date limite de dépôt
 * @param criteres critères pondérés
 * @param fournisseurIdsInvites fournisseurs invités
 * @param auteur Responsable des Achats
 */
public record CreerAppelOffreCommand(
    UUID besoinId,
    String objet,
    String description,
    TypeAppelOffre type,
    CategorieMateriel categorie,
    int quantite,
    String specifications,
    BigDecimal budgetEstime,
    Instant dateLimite,
    List<CriterePondere> criteres,
    List<UUID> fournisseurIdsInvites,
    CurrentUser auteur) {

  public CreerAppelOffreCommand {
    criteres = criteres == null ? List.of() : List.copyOf(criteres);
    fournisseurIdsInvites =
        fournisseurIdsInvites == null ? List.of() : List.copyOf(fournisseurIdsInvites);
  }
}
