package sn.cse.common.pagination;

import java.util.List;
import java.util.function.Function;

/**
 * Enveloppe de pagination standard de toutes les listes de l'API.
 *
 * @param content éléments de la page
 * @param page index de page (base 0)
 * @param size taille demandée
 * @param totalElements nombre total d'éléments
 * @param totalPages nombre total de pages
 * @param first vrai pour la première page
 * @param last vrai pour la dernière page
 * @param <T> type des éléments
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {

  public PageResponse {
    content = List.copyOf(content);
  }

  /**
   * Construit une page à partir de son contenu et du total.
   *
   * @param content éléments
   * @param page index de page
   * @param size taille de page
   * @param totalElements total
   * @param <T> type
   * @return la page
   */
  public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
    int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    return new PageResponse<>(
        content, page, size, totalElements, totalPages, page == 0, page >= totalPages - 1);
  }

  /**
   * Transforme le contenu en conservant les métadonnées de pagination.
   *
   * @param mapper fonction de conversion
   * @param <R> type cible
   * @return la page convertie
   */
  public <R> PageResponse<R> map(Function<T, R> mapper) {
    return new PageResponse<>(
        content.stream().map(mapper).toList(), page, size, totalElements, totalPages, first, last);
  }
}
