package sn.cse.fournisseurs.application.command;

import java.time.LocalDate;
import sn.cse.fournisseurs.domain.model.TypeDocument;

/**
 * Ajout d'un document administratif.
 *
 * @param type type
 * @param libelle libellé
 * @param dateExpiration date d'expiration (optionnelle)
 */
public record DocumentCommand(TypeDocument type, String libelle, LocalDate dateExpiration) {}
