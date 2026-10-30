package sn.cse.besoins.application.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.besoins.application.port.in.ConsoliderBesoinsUseCase;
import sn.cse.besoins.application.port.out.BesoinQueryPort;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.besoins.domain.service.ConsolidationBesoins;
import sn.cse.besoins.domain.service.LigneConsolidation;
import sn.cse.common.exception.InvalidRequestException;

/** Consolidation des besoins en attente ou validés (regroupement et doublons potentiels). */
@Service
@Transactional(readOnly = true)
public class ConsoliderBesoinsService implements ConsoliderBesoinsUseCase {

  static final Set<StatutBesoin> STATUTS_CONSOLIDABLES =
      EnumSet.of(StatutBesoin.EN_ATTENTE, StatutBesoin.VALIDE);

  private final BesoinQueryPort requetes;
  private final ConsolidationBesoins consolidation;
  private final ParametresBesoins parametres;

  ConsoliderBesoinsService(
      BesoinQueryPort requetes, ConsolidationBesoins consolidation, ParametresBesoins parametres) {
    this.requetes = requetes;
    this.consolidation = consolidation;
    this.parametres = parametres;
  }

  @Override
  public List<LigneConsolidation> consolider(StatutBesoin statut) {
    Set<StatutBesoin> statuts;
    if (statut == null) {
      statuts = STATUTS_CONSOLIDABLES;
    } else if (STATUTS_CONSOLIDABLES.contains(statut)) {
      statuts = EnumSet.of(statut);
    } else {
      throw new InvalidRequestException(
          "STATUT_NON_CONSOLIDABLE",
          "La consolidation ne porte que sur les besoins EN_ATTENTE ou VALIDE.");
    }
    return consolidation.consolider(
        requetes.aConsolider(statuts, parametres.limiteConsolidation()));
  }
}
