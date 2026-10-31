package sn.cse.chatbot.domain.model;

import java.time.LocalDate;

/**
 * Expression de besoin (lecture ms-besoins).
 *
 * @param id identifiant
 * @param reference référence
 * @param designation désignation
 * @param quantite quantité
 * @param statut statut
 * @param priorite priorité
 * @param chantierNom chantier
 * @param dateBesoin date de besoin
 * @param motifRejet motif de rejet ou {@code null}
 * @param validateurNom validateur ou {@code null}
 */
public record BesoinInfo(
    String id,
    String reference,
    String designation,
    int quantite,
    String statut,
    String priorite,
    String chantierNom,
    LocalDate dateBesoin,
    String motifRejet,
    String validateurNom) {}
