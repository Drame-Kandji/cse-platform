package sn.cse.common.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;

/**
 * Règles d'architecture hexagonale communes (AGENTS.md A.4). Usage dans chaque service (forme JUnit
 * Jupiter explicite : chaque règle devient un test, exécuté par Surefire) :
 *
 * <pre>{@code
 * class ArchitectureTest {
 *   @TestFactory
 *   Stream<DynamicTest> regles() {
 *     JavaClasses classes = HexagonalArchitectureRules.classesDe("sn.cse.suivi");
 *     return HexagonalArchitectureRules.toutes().stream()
 *         .map(r -> DynamicTest.dynamicTest(r.getDescription(), () -> r.check(classes)));
 *   }
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

  /**
   * @return toutes les règles, dans l'ordre de déclaration
   */
  public static List<ArchRule> toutes() {
    return List.of(
        DOMAINE_PUR,
        APPLICATION_SANS_ADAPTATEURS,
        API_SANS_INFRASTRUCTURE,
        CONTROLEURS_DANS_API,
        ENTITES_JPA_DANS_INFRASTRUCTURE);
  }

  /**
   * @param paquetRacine paquet du service (ex. {@code sn.cse.suivi})
   * @return les classes de production du service
   */
  public static JavaClasses classesDe(String paquetRacine) {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(paquetRacine);
  }

  private HexagonalArchitectureRules() {}
}
