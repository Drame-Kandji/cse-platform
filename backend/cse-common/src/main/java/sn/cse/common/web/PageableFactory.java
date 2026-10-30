package sn.cse.common.web;

import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.pagination.PageQuery;

/**
 * Construit des pages bornées et un tri restreint à une liste blanche (empêche le tri sur des
 * colonnes arbitraires ou sensibles).
 */
public class PageableFactory {

  private final PaginationProperties properties;

  /**
   * @param properties bornes de pagination
   */
  public PageableFactory(PaginationProperties properties) {
    this.properties = properties;
  }

  /**
   * @param page index demandé (null = 0)
   * @param size taille demandée (null = défaut, bornée au maximum)
   * @param sort tri {@code champ,asc|desc} (null = tri par défaut)
   * @param autorises champs triables
   * @param defaut tri par défaut {@code champ,asc|desc}
   * @return la demande de page validée
   * @throws InvalidRequestException si le champ de tri n'est pas autorisé
   */
  public PageQuery query(
      Integer page, Integer size, String sort, Set<String> autorises, String defaut) {
    int p = page == null || page < 0 ? 0 : page;
    int s =
        size == null || size < 1 ? properties.defaultSize() : Math.min(size, properties.maxSize());
    String brut = sort == null || sort.isBlank() ? defaut : sort;
    String[] parties = brut.split(",");
    String champ = parties[0].trim();
    if (!autorises.contains(champ)) {
      throw new InvalidRequestException(
          "TRI_NON_AUTORISE", "Tri impossible sur « " + champ + " ». Champs : " + autorises);
    }
    boolean asc = parties.length < 2 || !"desc".equals(parties[1].trim().toLowerCase(Locale.ROOT));
    return new PageQuery(p, s, champ, asc);
  }

  /**
   * @param query demande de page
   * @return l'équivalent Spring Data
   */
  public static Pageable toPageable(PageQuery query) {
    Sort sort =
        query.sortField() == null
            ? Sort.unsorted()
            : Sort.by(
                query.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC, query.sortField());
    return PageRequest.of(query.page(), query.size(), sort);
  }
}
