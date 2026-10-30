package sn.cse.suivi.domain.model;

/**
 * Situation opérationnelle d'un matériel, calculée à partir des autres agrégats et nécessaire aux
 * décisions de changement d'état.
 *
 * @param affectationActive une affectation non terminée existe
 * @param maintenanceEnCours un ordre de travail est en cours
 */
public record ContexteMateriel(boolean affectationActive, boolean maintenanceEnCours) {}
