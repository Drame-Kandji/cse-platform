package sn.cse.suivi.application.port.out;

import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Maintenance;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Panne;

/**
 * Publication des événements de domaine du suivi (outbox transactionnelle) : doit être appelée dans
 * la transaction du cas d'utilisation. Les types sont ceux de {@code sn.cse.events.EventTypes}.
 */
public interface SuiviEventPublisher {

  /**
   * @param type {@code CHANTIER_CREE} ou {@code CHANTIER_MODIFIE}
   * @param chantier chantier
   */
  void chantier(String type, Chantier chantier);

  /**
   * @param type {@code MATERIEL_ENREGISTRE}, {@code MATERIEL_ETAT_CHANGE}, {@code
   *     MATERIEL_REFORME_DEMANDEE}, {@code MATERIEL_REFORME} ou {@code ALERTE_MAINTENANCE}
   * @param materiel matériel
   * @param chantierActuel chantier courant (peut être {@code null})
   * @param motif motif ou message associé (peut être {@code null})
   */
  void materiel(String type, Materiel materiel, Chantier chantierActuel, String motif);

  /**
   * @param type {@code MATERIEL_AFFECTE}, {@code RECEPTION_CONFIRMEE}, {@code AFFECTATION_TERMINEE}
   *     ou {@code FIN_AFFECTATION_PROCHAINE}
   * @param affectation affectation
   * @param materiel matériel affecté
   * @param chantier chantier d'affectation
   */
  void affectation(String type, Affectation affectation, Materiel materiel, Chantier chantier);

  /**
   * @param type {@code MAINTENANCE_PLANIFIEE}, {@code MAINTENANCE_DEMARREE} ou {@code
   *     MAINTENANCE_CLOTUREE}
   * @param maintenance ordre de travail
   * @param materiel matériel concerné
   */
  void maintenance(String type, Maintenance maintenance, Materiel materiel);

  /**
   * @param panne panne signalée
   * @param materiel matériel concerné
   * @param chantier chantier du matériel (peut être {@code null})
   */
  void panneSignalee(Panne panne, Materiel materiel, Chantier chantier);
}
