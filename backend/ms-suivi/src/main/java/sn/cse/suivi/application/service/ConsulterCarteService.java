package sn.cse.suivi.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.application.port.in.ConsulterCarteUseCase;
import sn.cse.suivi.application.port.out.ChantierRepository;
import sn.cse.suivi.application.port.out.MaterielRepository;
import sn.cse.suivi.application.query.CarteVue;
import sn.cse.suivi.application.query.Perimetre;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.EtatMateriel;

/** Carte du parc dans le périmètre de l'utilisateur. */
@Service
@Transactional(readOnly = true)
public class ConsulterCarteService implements ConsulterCarteUseCase {

  private final ChantierRepository chantiers;
  private final MaterielRepository materiels;
  private final AssembleurVues vues;

  /**
   * @param chantiers stockage des chantiers
   * @param materiels stockage des matériels
   * @param vues assembleur de vues
   */
  public ConsulterCarteService(
      ChantierRepository chantiers, MaterielRepository materiels, AssembleurVues vues) {
    this.chantiers = chantiers;
    this.materiels = materiels;
    this.vues = vues;
  }

  @Override
  public CarteVue carte(CategorieMateriel categorie, EtatMateriel etat, CurrentUser lecteur) {
    Perimetre perimetre = Perimetre.de(lecteur);
    List<Chantier> visibles =
        perimetre.restreint()
            ? chantiers.findAllById(perimetre.chantierIds())
            : chantiers.findAll();
    return new CarteVue(
        vues.chantiers(visibles),
        vues.materiels(materiels.findPourCarte(categorie, etat, perimetre)));
  }
}
