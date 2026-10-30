package sn.cse.gateway;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;
import sn.cse.common.archunit.HexagonalArchitectureRules;

/**
 * La passerelle n'a ni domaine ni couche application : seules les règles applicables sont
 * vérifiées, ainsi que l'absence de Spring MVC (incompatible avec Spring Cloud Gateway WebFlux).
 */
class ArchitectureTest {

  private static final JavaClasses CLASSES = HexagonalArchitectureRules.classesDe("sn.cse.gateway");

  @Test
  void should_placer_les_controleurs_dans_api() {
    HexagonalArchitectureRules.CONTROLEURS_DANS_API.check(CLASSES);
  }

  @Test
  void should_ne_dependre_ni_de_servlet_ni_de_spring_mvc() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.servlet..", "org.springframework.web.servlet..")
        .check(CLASSES);
    assertThat(ClassUtils.isPresent("org.springframework.web.servlet.DispatcherServlet", null))
        .isFalse();
  }
}
