package sn.cse.audit.application.port.in;

import sn.cse.audit.domain.model.TraceAction;

/**
 * Cas d'utilisation : ajouter une action au journal d'audit (ajout seul, jamais de mise à jour).
 */
public interface EnregistrerTraceUseCase {

  /**
   * Ajoute l'action au journal si son événement n'a pas déjà été enregistré.
   *
   * @param trace action auditée
   * @return vrai si une entrée a été ajoutée, faux si l'événement était déjà journalisé
   */
  boolean enregistrer(TraceAction trace);
}
