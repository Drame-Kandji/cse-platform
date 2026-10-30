package sn.cse.events.payload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Instantané d'un contrat d'achat (CONTRAT_*, ENGAGEMENT_*, LIVRAISON_*). */
public record ContratPayload(
    UUID contratId,
    String numero,
    UUID appelOffreId,
    UUID besoinId,
    UUID chantierId,
    String chantierNom,
    UUID fournisseurId,
    String fournisseurNom,
    String designation,
    String categorie,
    int quantite,
    BigDecimal montant,
    String statut,
    LocalDate dateSignature,
    LocalDate dateLivraisonPrevue,
    LocalDate dateLivraisonReelle,
    BigDecimal tarifJournalierEstime) {}
