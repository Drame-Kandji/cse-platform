package sn.cse.suivi.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import sn.cse.suivi.domain.model.CategorieMateriel;

/**
 * Livraison d'un marché (événement {@code LIVRAISON_EFFECTUEE} de ms-achats).
 *
 * @param contratId contrat
 * @param numeroContrat numéro lisible du contrat
 * @param besoinId besoin d'origine (optionnel)
 * @param chantierId chantier destinataire (optionnel)
 * @param fournisseurId fournisseur
 * @param designation désignation des matériels livrés
 * @param categorie catégorie
 * @param quantite nombre d'unités livrées
 * @param montant montant total du marché (FCFA, optionnel)
 * @param tarifJournalier tarif journalier estimé (optionnel)
 * @param dateLivraison date de livraison effective (optionnelle)
 */
public record LivraisonRecue(
    UUID contratId,
    String numeroContrat,
    UUID besoinId,
    UUID chantierId,
    UUID fournisseurId,
    String designation,
    CategorieMateriel categorie,
    int quantite,
    BigDecimal montant,
    BigDecimal tarifJournalier,
    LocalDate dateLivraison) {}
