package sn.cse.reporting.domain.model;

import java.math.BigDecimal;

/**
 * Point d'une série temporelle.
 *
 * @param periode libellé de la tranche ({@code 2026-07}, {@code 2026-07-14}, {@code 2026-W29})
 * @param valeur valeur de l'indicateur sur la tranche
 */
public record Point(String periode, BigDecimal valeur) {}
