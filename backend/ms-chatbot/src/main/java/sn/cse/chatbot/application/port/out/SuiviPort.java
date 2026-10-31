package sn.cse.chatbot.application.port.out;

import java.time.LocalDate;
import java.util.List;
import sn.cse.chatbot.domain.model.AffectationInfo;
import sn.cse.chatbot.domain.model.ChantierInfo;
import sn.cse.chatbot.domain.model.MaintenanceInfo;
import sn.cse.chatbot.domain.model.MaterielInfo;

/**
 * Lecture seule dans ms-suivi, avec le JWT de l'utilisateur. Une ressource interdite ou absente
 * donne une liste vide ; une panne lève {@link
 * sn.cse.chatbot.application.exception.SourceIndisponibleException}.
 */
public interface SuiviPort {

  /**
   * @param critere filtre
   * @param limite nombre maximal
   * @return les matériels
   */
  List<MaterielInfo> rechercherMateriels(CritereMateriel critere, int limite);

  /**
   * @param materielId matériel ou {@code null} (toutes)
   * @param limite nombre maximal
   * @return les maintenances, plus récentes d'abord
   */
  List<MaintenanceInfo> maintenances(String materielId, int limite);

  /**
   * @param chantierId chantier ou {@code null}
   * @param du début de période
   * @param au fin de période
   * @param limite nombre maximal
   * @return les affectations de la période
   */
  List<AffectationInfo> planning(String chantierId, LocalDate du, LocalDate au, int limite);

  /**
   * @param materielId matériel
   * @param limite nombre maximal
   * @return ses affectations
   */
  List<AffectationInfo> affectationsDuMateriel(String materielId, int limite);

  /**
   * @param q nom ou code
   * @param limite nombre maximal
   * @return les chantiers correspondants
   */
  List<ChantierInfo> rechercherChantiers(String q, int limite);
}
