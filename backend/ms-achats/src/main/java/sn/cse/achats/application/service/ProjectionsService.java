package sn.cse.achats.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.command.BesoinEnAchatCommand;
import sn.cse.achats.application.command.FournisseurObserveCommand;
import sn.cse.achats.application.port.in.MettreAJourProjectionsUseCase;
import sn.cse.achats.application.port.out.BesoinAAcheterRepository;
import sn.cse.achats.application.port.out.FournisseurProjectionRepository;
import sn.cse.achats.domain.model.BesoinAAcheter;
import sn.cse.achats.domain.model.FournisseurConnu;

/** Mise à jour des projections locales (besoins à acheter, statut des fournisseurs pour R14). */
@Service
@Transactional
public class ProjectionsService implements MettreAJourProjectionsUseCase {

  private final BesoinAAcheterRepository besoins;
  private final FournisseurProjectionRepository fournisseurs;

  /**
   * @param besoins projection des besoins
   * @param fournisseurs projection des fournisseurs
   */
  public ProjectionsService(
      BesoinAAcheterRepository besoins, FournisseurProjectionRepository fournisseurs) {
    this.besoins = besoins;
    this.fournisseurs = fournisseurs;
  }

  @Override
  public void besoinEnAchat(BesoinEnAchatCommand c) {
    // Un rejeu (ou une nouvelle orientation) ne détache jamais l'appel d'offres déjà lancé.
    var appelOffreId =
        besoins.parBesoin(c.besoinId()).map(BesoinAAcheter::appelOffreId).orElse(null);
    besoins.enregistrer(
        new BesoinAAcheter(
            c.besoinId(),
            c.reference(),
            c.designation(),
            c.categorie(),
            c.quantite(),
            c.specifications(),
            c.priorite(),
            c.chantierId(),
            c.chantierNom(),
            c.recuLe(),
            appelOffreId));
  }

  @Override
  public void fournisseurObserve(FournisseurObserveCommand c) {
    fournisseurs.enregistrerSiPlusRecent(
        new FournisseurConnu(c.fournisseurId(), c.raisonSociale(), c.statut()), c.observeLe());
  }
}
