package sn.cse.auth.domain.exception;

import sn.cse.common.exception.BusinessRuleException;

/**
 * Un administrateur ne peut ni désactiver son propre compte ni modifier son propre rôle (évite de
 * perdre le dernier accès d'administration) : HTTP 422.
 */
public class OperationSurSoiInterditeException extends BusinessRuleException {

  /** Construit l'exception. */
  public OperationSurSoiInterditeException() {
    super(
        "OPERATION_SUR_SOI_INTERDITE",
        "Vous ne pouvez ni désactiver votre propre compte ni modifier votre propre rôle.");
  }
}
