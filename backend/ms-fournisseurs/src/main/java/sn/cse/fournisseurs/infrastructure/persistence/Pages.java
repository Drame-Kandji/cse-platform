package sn.cse.fournisseurs.infrastructure.persistence;

import java.util.function.Function;
import org.springframework.data.domain.Page;
import sn.cse.common.pagination.PageResponse;

/** Conversion des pages Spring Data en {@link PageResponse}. */
final class Pages {

  private Pages() {}

  static <E, D> PageResponse<D> versReponse(Page<E> page, Function<E, D> mapper) {
    return PageResponse.of(
        page.getContent().stream().map(mapper).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements());
  }
}
