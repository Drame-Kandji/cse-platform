package sn.cse.achats.application.port.in;

import java.util.List;
import java.util.UUID;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.BesoinAAcheter;
import sn.cse.common.pagination.PageQuery;
import sn.cse.common.pagination.PageResponse;

/** Lectures sur les appels d'offres et les besoins à acheter. */
public interface ConsulterAppelsOffresUseCase {

  /**
   * @param filtre critères
   * @param page pagination
   * @return la page d'appels d'offres
   */
  PageResponse<AppelOffre> rechercher(AppelOffreRepository.Filtre filtre, PageQuery page);

  /**
   * @param id appel d'offres
   * @return l'appel d'offres
   */
  AppelOffre detail(UUID id);

  /**
   * @param id appel d'offres
   * @return ses offres (anonymisées tant qu'il est publié, R10)
   */
  List<OffreVue> offres(UUID id);

  /**
   * @param page pagination
   * @return les besoins transmis aux achats
   */
  PageResponse<BesoinAAcheter> besoinsAAcheter(PageQuery page);
}
