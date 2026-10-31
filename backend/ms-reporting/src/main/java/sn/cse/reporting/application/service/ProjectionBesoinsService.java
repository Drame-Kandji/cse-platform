package sn.cse.reporting.application.service;

import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.events.EventTypes;
import sn.cse.events.payload.BesoinPayload;
import sn.cse.reporting.application.command.EvenementRecu;
import sn.cse.reporting.application.port.in.ProjeterBesoinsUseCase;
import sn.cse.reporting.application.port.out.ProjectionRepository;
import sn.cse.reporting.domain.model.CategorieMateriel;
import sn.cse.reporting.domain.model.Priorite;
import sn.cse.reporting.domain.model.StatutBesoin;
import sn.cse.reporting.domain.model.ValeursEnum;
import sn.cse.reporting.domain.model.vue.BesoinVue;
import sn.cse.reporting.domain.model.vue.ChantierVue;

/** Projection des événements de ms-besoins. */
@Service
@Transactional
public class ProjectionBesoinsService implements ProjeterBesoinsUseCase {

  /** Types traduisant une décision du responsable matériel (R01). */
  private static final Set<String> DECISIONS =
      Set.of(
          EventTypes.BESOIN_VALIDE,
          EventTypes.BESOIN_REJETE,
          EventTypes.BESOIN_AFFECTE_DIRECT,
          EventTypes.BESOIN_EN_ACHAT);

  private static final Set<String> TYPES =
      Set.of(
          EventTypes.BESOIN_SOUMIS,
          EventTypes.BESOIN_MODIFIE,
          EventTypes.BESOIN_VALIDE,
          EventTypes.BESOIN_REJETE,
          EventTypes.BESOIN_AFFECTE_DIRECT,
          EventTypes.BESOIN_EN_ACHAT);

  private final ProjectionRepository vues;

  /**
   * @param vues persistance des vues
   */
  public ProjectionBesoinsService(ProjectionRepository vues) {
    this.vues = vues;
  }

  @Override
  public boolean projeter(EvenementRecu e) {
    if (!TYPES.contains(e.type())) {
      return false;
    }
    BesoinPayload p = e.payload(BesoinPayload.class);
    Instant quand = e.occurredAt();
    BesoinVue recu =
        new BesoinVue(
            p.besoinId(),
            p.reference(),
            p.chantierId(),
            p.chantierNom(),
            ValeursEnum.lire(CategorieMateriel.class, p.categorie()),
            p.quantite(),
            ValeursEnum.lire(Priorite.class, p.priorite()),
            ValeursEnum.lire(StatutBesoin.class, p.statut()),
            p.dateBesoin(),
            EventTypes.BESOIN_SOUMIS.equals(e.type()) ? quand : null,
            DECISIONS.contains(e.type()) ? quand : null,
            quand,
            quand);
    vues.enregistrer(vues.besoin(p.besoinId()).map(b -> b.fusionnerAvec(recu)).orElse(recu));
    if (p.chantierId() != null) {
      vues.referencer(ChantierVue.reference(p.chantierId(), p.chantierNom(), quand));
    }
    return true;
  }
}
