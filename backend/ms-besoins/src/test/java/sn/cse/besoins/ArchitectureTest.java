package sn.cse.besoins;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import sn.cse.common.archunit.HexagonalArchitectureRules;

/**
 * Vérifie toutes les règles de {@link HexagonalArchitectureRules} sur les classes du service. Les
 * règles sont évaluées par JUnit Jupiter (une règle = un test dynamique) : sous Surefire, la forme
 * {@code @ArchTest ArchTests.in(...)} n'exécutait aucune règle.
 */
class ArchitectureTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("sn.cse.besoins");

  @TestFactory
  Stream<DynamicTest> regles_hexagonales() {
    List<Field> champs =
        Arrays.stream(HexagonalArchitectureRules.class.getFields())
            .filter(
                f ->
                    Modifier.isStatic(f.getModifiers())
                        && ArchRule.class.isAssignableFrom(f.getType()))
            .toList();
    assertThat(champs).hasSizeGreaterThanOrEqualTo(5);
    return champs.stream()
        .map(
            f ->
                DynamicTest.dynamicTest(
                    f.getName(), () -> ((ArchRule) f.get(null)).check(CLASSES)));
  }
}
