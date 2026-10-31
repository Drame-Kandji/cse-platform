package sn.cse.achats.application.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.achats.application.port.in.ConsulterAppelsOffresUseCase;
import sn.cse.achats.application.port.in.OffreVue;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.BesoinAAcheterRepository;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.BesoinAAcheter;
import sn.cse.achats.domain.service.MoteurEvaluation;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Lectures sur les appels d'offres, leurs offres et les besoins à acheter. */
@Service
@Transactional(readOnly = true)
public class ConsultationAppelsOffresService implements ConsulterAppelsOffresUseCase {

  private final AppelOffreRepository appelsOffres;
  private final BesoinAAcheterRepository besoins;
  private final ContratRepository contrats;
  private final MoteurEvaluation moteur;

  /**
   * @param appelsOffres dépôt des appels d'offres
   * @param besoins projection des besoins
   * @param contrats dépôt des contrats (contrat généré par appel d'offres)
   * @param moteur moteur d'évaluation
   */
  public ConsultationAppelsOffresService(
      AppelOffreRepository appelsOffres,
      BesoinAAcheterRepository besoins,
      ContratRepository contrats,
      MoteurEvaluation moteur) {
    this.appelsOffres = appelsOffres;
    this.besoins = besoins;
    this.contrats = contrats;
    this.moteur = moteur;
  }

  @Override
  public PageResponse<AppelOffre> rechercher(AppelOffreRepository.Filtre filtre, PageQuery page) {
    return appelsOffres.rechercher(filtre, page);
  }

  @Override
  public AppelOffre detail(UUID id) {
    return Chargements.appelOffre(appelsOffres, id);
  }

  @Override
  public Map<UUID, UUID> contratsGeneres(Collection<UUID> appelOffreIds) {
    return contrats.idsParAppelOffre(appelOffreIds);
  }

  @Override
  public List<OffreVue> offres(UUID id) {
    AppelOffre appelOffre = detail(id);
    // Règle R10 : le moteur ne produit aucune note et l'anonymat est signalé tant que PUBLIE.
    return moteur.evaluer(appelOffre).stream()
        .map(e -> new OffreVue(appelOffre.getId(), appelOffre.estAnonyme(), e))
        .toList();
  }

  @Override
  public PageResponse<BesoinAAcheter> besoinsAAcheter(PageQuery page) {
    return besoins.lister(page);
  }
}
