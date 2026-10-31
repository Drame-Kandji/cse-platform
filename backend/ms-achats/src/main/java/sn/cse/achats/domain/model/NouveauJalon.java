package sn.cse.achats.domain.model;

import java.time.LocalDate;

/**
 * Jalon saisi lors de la rédaction des clauses.
 *
 * @param libelle libellé
 * @param datePrevue échéance
 */
public record NouveauJalon(String libelle, LocalDate datePrevue) {}
