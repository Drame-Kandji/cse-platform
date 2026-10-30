package sn.cse.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import sn.cse.common.exception.InvalidRequestException;
import sn.cse.common.pagination.PageQuery;

class PageableFactoryTest {

  private final PageableFactory factory = new PageableFactory(new PaginationProperties(20, 50));
  private static final Set<String> TRIABLES = Set.of("createdAt", "reference");

  @Test
  void should_cap_size_to_max_when_too_large() {
    PageQuery q = factory.query(0, 500, null, TRIABLES, "createdAt,desc");

    assertThat(q.size()).isEqualTo(50);
    assertThat(q.sortField()).isEqualTo("createdAt");
    assertThat(q.ascending()).isFalse();
  }

  @Test
  void should_apply_defaults_when_params_missing() {
    PageQuery q = factory.query(null, null, "reference", TRIABLES, "createdAt,desc");

    assertThat(q.page()).isZero();
    assertThat(q.size()).isEqualTo(20);
    assertThat(q.ascending()).isTrue();
  }

  @Test
  void should_reject_sort_when_field_not_whitelisted() {
    assertThatThrownBy(() -> factory.query(0, 10, "motDePasse,asc", TRIABLES, "createdAt,desc"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("motDePasse");
  }

  @Test
  void should_convert_to_spring_pageable() {
    var pageable = PageableFactory.toPageable(new PageQuery(2, 10, "reference", true));

    assertThat(pageable.getPageNumber()).isEqualTo(2);
    assertThat(pageable.getSort().getOrderFor("reference")).isNotNull();
  }
}
