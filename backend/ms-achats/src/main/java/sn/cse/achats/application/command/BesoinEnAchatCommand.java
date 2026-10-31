package sn.cse.achats.application.command;

import java.time.Instant;
import java.util.UUID;
import sn.cse.achats.domain.model.CategorieMateriel;
import sn.cse.achats.domain.model.Priorite;

/**
 * Réception d'un besoin orienté vers l'achat (BESOIN_EN_ACHAT).
 *
 * @param besoinId besoin
 * @param reference référence
 * @param designation désignation
 * @param categorie catégorie
 * @param quantite quantité
 * @param specifications spécifications
 * @param priorite priorité
 * @param chantierId chantier
 * @param chantierNom nom du chantier
 * @param recuLe instant de l'événement
 */
public record BesoinEnAchatCommand(
    UUID besoinId,
    String reference,
    String designation,
    CategorieMateriel categorie,
    int quantite,
    String specifications,
    Priorite priorite,
    UUID chantierId,
    String chantierNom,
    Instant recuLe) {}
