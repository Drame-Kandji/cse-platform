package sn.cse.besoins.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * État persistant complet d'un besoin, utilisé pour reconstituer l'agrégat depuis le stockage.
 *
 * @param id identifiant
 * @param reference référence lisible ({@code BES-2026-0001})
 * @param chantierId chantier demandeur
 * @param chantierNom nom du chantier (dénormalisé)
 * @param demandeurId auteur de la demande
 * @param demandeurNom nom du demandeur
 * @param description contenu du besoin
 * @param statut statut courant
 * @param motifRejet motif du rejet
 * @param validateurId auteur de la décision (validation ou rejet)
 * @param validateurNom nom de l'auteur de la décision
 * @param dateValidation instant de la décision
 * @param materielIdsAffectes matériels affectés au besoin
 * @param orientationEnAttente vrai si l'orientation reste à faire ou à relancer
 * @param historique historique chronologique
 * @param version version (verrou optimiste)
 * @param creeLe instant de soumission
 * @param modifieLe instant de dernière modification
 */
public record EtatBesoin(
    UUID id,
    String reference,
    UUID chantierId,
    String chantierNom,
    UUID demandeurId,
    String demandeurNom,
    DescriptionBesoin description,
    StatutBesoin statut,
    String motifRejet,
    UUID validateurId,
    String validateurNom,
    Instant dateValidation,
    List<UUID> materielIdsAffectes,
    boolean orientationEnAttente,
    List<HistoriqueBesoin> historique,
    Long version,
    Instant creeLe,
    Instant modifieLe) {}
