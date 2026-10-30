package sn.cse.common.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Règles d'architecture hexagonale communes (AGENTS.md A.4). Usage dans chaque service :
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "sn.cse.suivi", importOptions = ImportOption.DoNotIncludeTests.class)
 * class ArchitectureTest {
 *   @ArchTest static final ArchTests regles = ArchTests.in(HexagonalArchitectureRules.class);
 * }
 * }</pre>
 */
public final class HexagonalArchitectureRules {

  /** Le domaine est pur : aucun framework, aucune dépendance vers les autres couches. */
  @ArchTest
  public static final ArchRule DOMAINE_PUR =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "org.hibernate..",
              "com.fasterxml.jackson..",
              "org.apache.kafka..",
              "lombok..",
              "..application..",
              "..infrastructure..",
              "..api..");

  /** L'application ne connaît ni les adaptateurs ni les technologies d'entrée/sortie. */
  @ArchTest
  public static final ArchRule APPLICATION_SANS_ADAPTATEURS =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..infrastructure..",
              "..api..",
              "jakarta.persistence..",
              "org.springframework.web..",
              "org.springframework.kafka..",
              "org.springframework.data..");

  /** Les contrôleurs n'accèdent jamais directement à l'infrastructure. */
  @ArchTest
  public static final ArchRule API_SANS_INFRASTRUCTURE =
      noClasses()
          .that()
          .resideInAPackage("..api..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..infrastructure..");

  /** Les contrôleurs REST vivent dans l'adaptateur entrant. */
  @ArchTest
  public static final ArchRule CONTROLEURS_DANS_API =
      classes()
          .that()
          .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
          .should()
          .resideInAPackage("..api..");

  /** Les entités JPA vivent dans l'infrastructure (distinctes des objets du domaine). */
  @ArchTest
  public static final ArchRule ENTITES_JPA_DANS_INFRASTRUCTURE =
      classes()
          .that()
          .areAnnotatedWith("jakarta.persistence.Entity")
          .should()
          .resideInAPackage("..infrastructure..");

  private HexagonalArchitectureRules() {}
}
