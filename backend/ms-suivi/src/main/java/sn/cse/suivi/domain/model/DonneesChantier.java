package sn.cse.suivi.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Données saisissables d'un chantier (création et modification).
 *
 * @param code code unique (ex. {@code CH-DKR-01})
 * @param nom intitulé
 * @param localisation lieu
 * @param region région administrative
 * @param coordonnees position GPS
 * @param budget budget alloué (FCFA)
 * @param montantMarche montant du marché (FCFA)
 * @param dateDebut date de démarrage
 * @param dateFinPrevue fin prévisionnelle (optionnelle)
 * @param chefChantierId chef de chantier (optionnel)
 * @param chefChantierNom nom affichable du chef (optionnel)
 */
public record DonneesChantier(
    String code,
    String nom,
    String localisation,
    String region,
    Coordonnees coordonnees,
    BigDecimal budget,
    BigDecimal montantMarche,
    LocalDate dateDebut,
    LocalDate dateFinPrevue,
    UUID chefChantierId,
    String chefChantierNom) {}
